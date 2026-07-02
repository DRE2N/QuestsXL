package de.erethon.hermes;

import de.erethon.hecate.Hecate;
import de.erethon.aether.Aether;
import de.erethon.factions.Factions;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.crafting.VanillaRecipe;
import de.erethon.hephaestus.items.upgrades.HItemUpgrade;
import de.erethon.hephaestus.jobs.HJob;
import de.erethon.hephaestus.jobs.crafting.JobRecipe;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.error.FriendlyError;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class HermesContentService {

    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Hermes plugin;
    private final QuestsXL qxl;
    private final Path draftRoot;
    private final Path backupRoot;

    public HermesContentService(Hermes plugin) {
        this.plugin = plugin;
        this.qxl = QuestsXL.get();
        this.draftRoot = plugin.getDataFolder().toPath().resolve("web-drafts");
        this.backupRoot = plugin.getDataFolder().toPath().resolve("web-backups");
    }

    public List<Map<String, Object>> listTypes() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (HermesContentType type : HermesContentType.values()) {
            if (!type.visibleInWebEditor()) {
                continue;
            }
            result.add(Map.of(
                    "id", type.id(),
                    "label", type.label(),
                    "directory", type.directory()
            ));
        }
        return result;
    }

    public List<Map<String, Object>> listItems(HermesContentType type) throws IOException {
        if (type == HermesContentType.TRANSLATIONS) {
            return listTranslationItems();
        }
        if (isVirtualHephaestusType(type)) {
            return listVirtualHephaestusItems(type);
        }
        List<Map<String, Object>> result = new ArrayList<>();
        if (!type.directory()) {
            File root = type.root();
            result.add(itemDto(type, root.getName(), root.toPath(), Files.exists(root.toPath())));
            return result;
        }
        Path root = requireRoot(type);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> matchesContentExtension(type, path))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .forEach(path -> result.add(itemDto(type, normalize(root.relativize(path)), path, true)));
        }
        return result;
    }

    public Map<String, Object> read(HermesContentType type, String itemPath) throws IOException {
        if (type == HermesContentType.TRANSLATIONS) {
            return readTranslations(itemPath);
        }
        if (isVirtualHephaestusType(type)) {
            return readVirtualHephaestus(type, itemPath);
        }
        Path path = resolveContentPath(type, itemPath, false);
        String yaml = Files.exists(path) ? Files.readString(path, StandardCharsets.UTF_8) : "";
        return Map.of(
                "type", type.id(),
                "path", displayPath(type, itemPath),
                "yaml", yaml,
                "exists", Files.exists(path),
                "draft", readDraft(type, itemPath)
        );
    }

    public Map<String, Object> saveDraft(HermesContentType type, String itemPath, String yaml) throws IOException {
        ValidationResult validation = validateYaml(yaml);
        Path draft = resolveDraftPath(type, itemPath);
        Files.createDirectories(draft.getParent());
        Files.writeString(draft, yaml, StandardCharsets.UTF_8);
        return Map.of(
                "success", true,
                "valid", validation.valid(),
                "errors", validation.errors(),
                "draftPath", normalize(draftRoot.relativize(draft))
        );
    }

    public Map<String, Object> validate(HermesContentType type, String itemPath, String yaml) {
        ValidationResult syntax = validateYaml(yaml);
        if (!syntax.valid()) {
            return Map.of("valid", false, "errors", syntax.errors());
        }
        List<Map<String, Object>> warnings = new ArrayList<>();
        if ((type == HermesContentType.QUESTS || type == HermesContentType.EVENTS) && !yaml.contains("stages:")) {
            warnings.add(error("Missing stages", "Quest and event files usually need a stages section.", displayPath(type, itemPath)));
        }
        if (type == HermesContentType.INTERACTIONS && !yaml.contains("objectives:")) {
            warnings.add(error("Missing objectives", "Interaction files usually define objectives.", displayPath(type, itemPath)));
        }
        if (type == HermesContentType.TRANSLATIONS) {
            TranslationValidation translationValidation = validateTranslations(itemPath, yaml);
            if (!translationValidation.errors().isEmpty()) {
                return Map.of("valid", false, "errors", translationValidation.errors());
            }
            warnings.addAll(translationValidation.warnings());
        }
        if (type == HermesContentType.HEPHAESTUS_ITEMS) {
            ValidationResult itemValidation = validateHephaestusItem(yaml);
            if (!itemValidation.valid()) {
                return Map.of("valid", false, "errors", itemValidation.errors());
            }
        }
        if (isHephaestusContent(type)) {
            ValidationResult hephaestusValidation = validateHephaestusContent(type, itemPath, yaml);
            if (!hephaestusValidation.valid()) {
                return Map.of("valid", false, "errors", hephaestusValidation.errors());
            }
            warnings.addAll(hephaestusValidation.errors());
        }
        if (type == HermesContentType.FACTIONS_BUILDINGS) {
            ValidationResult buildingValidation = validateFactionsBuilding(itemPath, yaml);
            if (!buildingValidation.valid()) {
                return Map.of("valid", false, "errors", buildingValidation.errors());
            }
            warnings.addAll(buildingValidation.errors());
        }
        return Map.of("valid", true, "errors", List.of(), "warnings", warnings);
    }

    public CompletableFuture<Map<String, Object>> publish(HermesContentType type, String itemPath, String yaml) {
        ValidationResult validation = validateYaml(yaml);
        if (!validation.valid()) {
            return CompletableFuture.completedFuture(Map.of("success", false, "errors", validation.errors()));
        }
        if (type == HermesContentType.TRANSLATIONS) {
            TranslationValidation translationValidation = validateTranslations(itemPath, yaml);
            if (!translationValidation.errors().isEmpty()) {
                return CompletableFuture.completedFuture(Map.of("success", false, "errors", translationValidation.errors()));
            }
            return publishTranslations(itemPath, yaml);
        }
        if (type == HermesContentType.HEPHAESTUS_ITEMS) {
            ValidationResult itemValidation = validateHephaestusItem(yaml);
            if (!itemValidation.valid()) {
                return CompletableFuture.completedFuture(Map.of("success", false, "errors", itemValidation.errors()));
            }
        }
        if (isHephaestusContent(type)) {
            ValidationResult hephaestusValidation = validateHephaestusContent(type, itemPath, yaml);
            if (!hephaestusValidation.valid()) {
                return CompletableFuture.completedFuture(Map.of("success", false, "errors", hephaestusValidation.errors()));
            }
        }
        if (type == HermesContentType.FACTIONS_BUILDINGS) {
            ValidationResult buildingValidation = validateFactionsBuilding(itemPath, yaml);
            if (!buildingValidation.valid()) {
                return CompletableFuture.completedFuture(Map.of("success", false, "errors", buildingValidation.errors()));
            }
        }
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (isVirtualHephaestusType(type)) {
                    publishVirtualHephaestus(type, itemPath, yaml);
                    deleteDraftIfExists(type, itemPath);
                    List<Map<String, Object>> reloadWarnings = reloadSafely(type);
                    List<Map<String, Object>> errors = new ArrayList<>(reloadWarnings);
                    errors.addAll(plugin.getGitExportService().markDirty(type, displayPath(type, itemPath), virtualContentPath(type, itemPath), false));
                    future.complete(Map.of(
                            "success", true,
                            "errors", errors,
                            "path", displayPath(type, itemPath)
                    ));
                    return;
                }
                Path live = resolveContentPath(type, itemPath, true);
                backup(live, type, itemPath);
                Files.createDirectories(live.getParent());
                Files.writeString(live, yaml, StandardCharsets.UTF_8);
                deleteDraftIfExists(type, itemPath);
                List<Map<String, Object>> reloadWarnings = reloadSafely(type);
                List<Map<String, Object>> errors = new ArrayList<>(errors());
                errors.addAll(reloadWarnings);
                errors.addAll(plugin.getGitExportService().markDirty(type, displayPath(type, itemPath), live, false));
                future.complete(Map.of(
                        "success", true,
                        "errors", errors,
                        "path", displayPath(type, itemPath)
                ));
            } catch (Throwable e) {
                future.complete(Map.of("success", false, "errors", List.of(exceptionError("Publish failed", e, displayPath(type, itemPath)))));
            }
        });
        return future;
    }

    public CompletableFuture<Map<String, Object>> publishBatch(List<Map<String, String>> items) {
        List<BatchItem> batchItems = new ArrayList<>();
        List<Map<String, Object>> fileResults = new ArrayList<>();
        for (Map<String, String> item : items) {
            HermesContentType type;
            String path = item.getOrDefault("path", "");
            String yaml = item.getOrDefault("yaml", "");
            try {
                type = HermesContentType.byId(item.getOrDefault("type", ""));
            } catch (Throwable e) {
                Map<String, Object> result = fileResult(item.getOrDefault("type", ""), path, false, List.of(exceptionError("Invalid content type", e, path)));
                fileResults.add(result);
                continue;
            }
            List<Map<String, Object>> validationErrors = validateForPublish(type, path, yaml);
            if (!validationErrors.isEmpty()) {
                fileResults.add(fileResult(type.id(), displayPathSafe(type, path), false, validationErrors));
                continue;
            }
            batchItems.add(new BatchItem(type, path, yaml));
        }
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Map<String, List<Map<String, Object>>> reloadGroups = new LinkedHashMap<>();
            try {
                for (BatchItem item : batchItems) {
                    try {
                        PublishWriteResult writeResult = publishWithoutReload(item.type(), item.path(), item.yaml());
                        reloadGroups.putIfAbsent(writeResult.reloadGroup(), new ArrayList<>());
                        fileResults.add(fileResult(item.type().id(), writeResult.path(), true, writeResult.errors()));
                    } catch (Throwable e) {
                        fileResults.add(fileResult(item.type().id(), displayPathSafe(item.type(), item.path()), false, List.of(exceptionError("Publish failed", e, displayPathSafe(item.type(), item.path())))));
                    }
                }
                List<Map<String, Object>> reloadResults = new ArrayList<>();
                for (String reloadGroup : reloadGroups.keySet()) {
                    List<Map<String, Object>> errors = reloadGroup(reloadGroup);
                    reloadResults.add(reloadResult(reloadGroup, errors));
                }
                List<Map<String, Object>> errors = new ArrayList<>();
                fileResults.stream()
                        .filter(result -> Boolean.FALSE.equals(result.get("success")))
                        .flatMap(result -> ((List<Map<String, Object>>) result.getOrDefault("errors", List.of())).stream())
                        .forEach(errors::add);
                reloadResults.stream()
                        .flatMap(result -> ((List<Map<String, Object>>) result.getOrDefault("errors", List.of())).stream())
                        .forEach(errors::add);
                boolean success = fileResults.stream().allMatch(result -> Boolean.TRUE.equals(result.get("success"))) && errors.isEmpty();
                future.complete(Map.of(
                        "success", success,
                        "files", fileResults,
                        "reloads", reloadResults,
                        "errors", errors
                ));
            } catch (Throwable e) {
                future.complete(Map.of(
                        "success", false,
                        "files", fileResults,
                        "reloads", List.of(),
                        "errors", List.of(exceptionError("Batch publish failed", e, "Hermes"))
                ));
            }
        });
        return future;
    }

    private List<Map<String, Object>> validateForPublish(HermesContentType type, String itemPath, String yaml) {
        ValidationResult validation = validateYaml(yaml);
        if (!validation.valid()) {
            return validation.errors();
        }
        if (type == HermesContentType.TRANSLATIONS) {
            TranslationValidation translationValidation = validateTranslations(itemPath, yaml);
            return translationValidation.errors();
        }
        if (type == HermesContentType.HEPHAESTUS_ITEMS) {
            ValidationResult itemValidation = validateHephaestusItem(yaml);
            if (!itemValidation.valid()) {
                return itemValidation.errors();
            }
        }
        if (isHephaestusContent(type)) {
            ValidationResult hephaestusValidation = validateHephaestusContent(type, itemPath, yaml);
            if (!hephaestusValidation.valid()) {
                return hephaestusValidation.errors();
            }
        }
        if (type == HermesContentType.FACTIONS_BUILDINGS) {
            ValidationResult buildingValidation = validateFactionsBuilding(itemPath, yaml);
            if (!buildingValidation.valid()) {
                return buildingValidation.errors();
            }
        }
        return List.of();
    }

    private PublishWriteResult publishWithoutReload(HermesContentType type, String itemPath, String yaml) throws IOException, InvalidConfigurationException {
        if (type == HermesContentType.TRANSLATIONS) {
            TranslationWriteResult translation = publishTranslationsWithoutReload(itemPath, yaml);
            return new PublishWriteResult(translation.pluginName(), translation.pluginName(), translation.errors());
        }
        if (isVirtualHephaestusType(type)) {
            publishVirtualHephaestus(type, itemPath, yaml);
            deleteDraftIfExists(type, itemPath);
            List<Map<String, Object>> errors = new ArrayList<>();
            errors.addAll(plugin.getGitExportService().markDirty(type, displayPath(type, itemPath), virtualContentPath(type, itemPath), false));
            return new PublishWriteResult(displayPath(type, itemPath), reloadGroupFor(type, itemPath), errors);
        }
        Path live = resolveContentPath(type, itemPath, true);
        backup(live, type, itemPath);
        Files.createDirectories(live.getParent());
        Files.writeString(live, yaml, StandardCharsets.UTF_8);
        deleteDraftIfExists(type, itemPath);
        List<Map<String, Object>> errors = new ArrayList<>();
        errors.addAll(plugin.getGitExportService().markDirty(type, displayPath(type, itemPath), live, false));
        return new PublishWriteResult(displayPath(type, itemPath), reloadGroupFor(type, itemPath), errors);
    }

    private String reloadGroupFor(HermesContentType type, String itemPath) {
        return switch (type) {
            case QUESTS, EVENTS, INTERACTIONS, MACROS, DIALOGUES, RESPAWNS, EXPLORABLES, EXPLORATION_SETS, GLOBAL_OBJECTIVES, PERIODIC_QUESTS -> "QuestsXL";
            case AETHER_MOBS -> "Aether";
            case HEPHAESTUS_ITEMS, HEPHAESTUS_UPGRADES, HEPHAESTUS_JOBS, HEPHAESTUS_VANILLA_RECIPES, HEPHAESTUS_JOB_RECIPES, HEPHAESTUS_SHOPS -> "Hephaestus";
            case HECATE_CLASSES, HECATE_TRAITLINES -> "Hecate";
            case SPELLBOOK_SPELLS, SPELLBOOK_TRAITS, SPELLBOOK_EFFECTS -> "Spellbook";
            case FACTIONS_BUILDINGS, FACTIONS_BUILDING_TAGS -> "Factions";
            case TRANSLATIONS -> translationPluginName(itemPath);
            default -> type.label();
        };
    }

    private List<Map<String, Object>> reloadGroup(String group) {
        return switch (group.toLowerCase(java.util.Locale.ROOT)) {
            case "questsxl" -> reloadAllQxlContent();
            case "aether" -> reloadAether();
            case "hephaestus" -> reloadAllHephaestusContent();
            case "hecate" -> reloadHecate();
            case "spellbook" -> reloadSpellbookLibrary();
            case "factions" -> reloadFactionsBuildings();
            default -> reloadPluginByName(group);
        };
    }

    private Map<String, Object> fileResult(String type, String path, boolean success, List<Map<String, Object>> errors) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", type);
        result.put("path", path);
        result.put("success", success);
        result.put("errors", errors);
        return result;
    }

    private Map<String, Object> reloadResult(String pluginName, List<Map<String, Object>> errors) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("plugin", pluginName);
        result.put("success", errors.isEmpty());
        result.put("errors", errors);
        return result;
    }

    private String displayPathSafe(HermesContentType type, String itemPath) {
        try {
            return displayPath(type, itemPath);
        } catch (Throwable ignored) {
            return itemPath == null ? "" : itemPath;
        }
    }

    public CompletableFuture<Map<String, Object>> delete(HermesContentType type, String itemPath) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (isVirtualHephaestusType(type)) {
                    deleteVirtualHephaestus(type, itemPath);
                    deleteDraftIfExists(type, itemPath);
                    List<Map<String, Object>> reloadWarnings = reloadSafely(type);
                    List<Map<String, Object>> errors = new ArrayList<>(reloadWarnings);
                    errors.addAll(plugin.getGitExportService().markDirty(type, displayPath(type, itemPath), virtualContentPath(type, itemPath), false));
                    future.complete(Map.of(
                            "success", true,
                            "errors", errors,
                            "path", displayPath(type, itemPath)
                    ));
                    return;
                }
                requireDirectoryType(type, "Delete");
                Path live = resolveContentPath(type, itemPath, false);
                backup(live, type, itemPath);
                Files.delete(live);
                deleteDraftIfExists(type, itemPath);
                List<Map<String, Object>> reloadWarnings = reloadSafely(type);
                List<Map<String, Object>> errors = new ArrayList<>(errors());
                errors.addAll(reloadWarnings);
                errors.addAll(plugin.getGitExportService().markDirty(type, displayPath(type, itemPath), live, true));
                future.complete(Map.of(
                        "success", true,
                        "errors", errors,
                        "path", displayPath(type, itemPath)
                ));
            } catch (Throwable e) {
                future.complete(Map.of("success", false, "errors", List.of(exceptionError("Delete failed", e, displayPath(type, itemPath)))));
            }
        });
        return future;
    }

    public CompletableFuture<Map<String, Object>> move(HermesContentType type, String itemPath, String nextPath) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (isVirtualHephaestusType(type)) {
                    moveVirtualHephaestus(type, itemPath, nextPath);
                    Path draft = resolveDraftPath(type, itemPath);
                    if (Files.exists(draft)) {
                        Path movedDraft = resolveDraftPath(type, nextPath);
                        Files.createDirectories(movedDraft.getParent());
                        Files.move(draft, movedDraft, StandardCopyOption.REPLACE_EXISTING);
                    }
                    String normalizedNextPath = displayPath(type, nextPath);
                    List<Map<String, Object>> reloadWarnings = reloadSafely(type);
                    List<Map<String, Object>> errors = new ArrayList<>(reloadWarnings);
                    errors.addAll(plugin.getGitExportService().markDirty(type, displayPath(type, itemPath), virtualContentPath(type, itemPath), false));
                    errors.addAll(plugin.getGitExportService().markDirty(type, normalizedNextPath, virtualContentPath(type, nextPath), false));
                    future.complete(Map.of(
                            "success", true,
                            "errors", errors,
                            "path", normalizedNextPath,
                            "oldPath", displayPath(type, itemPath)
                    ));
                    return;
                }
                requireDirectoryType(type, "Move");
                Path source = resolveContentPath(type, itemPath, false);
                Path target = resolveContentPath(type, nextPath, true);
                String normalizedNextPath = displayPath(type, nextPath);
                if (Files.exists(target)) {
                    throw new IllegalArgumentException("Target file already exists: " + normalizedNextPath);
                }
                backup(source, type, itemPath);
                Files.createDirectories(target.getParent());
                Files.move(source, target);
                Path draft = resolveDraftPath(type, itemPath);
                if (Files.exists(draft)) {
                    Path movedDraft = resolveDraftPath(type, nextPath);
                    Files.createDirectories(movedDraft.getParent());
                    Files.move(draft, movedDraft, StandardCopyOption.REPLACE_EXISTING);
                }
                List<Map<String, Object>> reloadWarnings = reloadSafely(type);
                List<Map<String, Object>> errors = new ArrayList<>(errors());
                errors.addAll(reloadWarnings);
                errors.addAll(plugin.getGitExportService().markDirty(type, displayPath(type, itemPath), source, true));
                errors.addAll(plugin.getGitExportService().markDirty(type, normalizedNextPath, target, false));
                future.complete(Map.of(
                        "success", true,
                        "errors", errors,
                        "path", normalizedNextPath,
                        "oldPath", displayPath(type, itemPath)
                ));
            } catch (Throwable e) {
                future.complete(Map.of("success", false, "errors", List.of(exceptionError("Move failed", e, displayPath(type, itemPath)))));
            }
        });
        return future;
    }

    private List<Map<String, Object>> listTranslationItems() throws IOException {
        Set<String> itemPaths = new LinkedHashSet<>();
        for (Plugin installed : Bukkit.getPluginManager().getPlugins()) {
            collectTranslationItems(installed.getName(), installed.getDataFolder().toPath(), itemPaths);
        }
        Path plugins = Path.of("plugins");
        if (Files.isDirectory(plugins)) {
            try (var stream = Files.list(plugins)) {
                stream.filter(Files::isDirectory)
                        .forEach(path -> collectTranslationItems(path.getFileName().toString(), path, itemPaths));
            }
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (String itemPath : itemPaths) {
            Path root = translationRoot(itemPath);
            result.add(itemDto(HermesContentType.TRANSLATIONS, itemPath, root, true));
        }
        result.sort(Comparator.comparing(row -> String.valueOf(row.get("id"))));
        return result;
    }

    private Map<String, Object> readTranslations(String itemPath) throws IOException {
        String pluginName = translationPluginName(itemPath);
        YamlConfiguration english = loadLanguage(itemPath, "english.yml");
        YamlConfiguration german = loadLanguage(itemPath, "german.yml");
        Map<String, Map<String, String>> rows = new LinkedHashMap<>();
        flattenTranslations(english).forEach((key, value) -> rows.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).put("english", value));
        flattenTranslations(german).forEach((key, value) -> rows.computeIfAbsent(key, ignored -> new LinkedHashMap<>()).put("german", value));
        YamlConfiguration synthetic = new YamlConfiguration();
        synthetic.set("plugin", pluginName);
        List<Map<String, String>> rowList = new ArrayList<>();
        for (String key : rows.keySet().stream().sorted().toList()) {
            Map<String, String> row = rows.get(key);
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("key", key);
            entry.put("english", row.getOrDefault("english", ""));
            entry.put("german", row.getOrDefault("german", ""));
            rowList.add(entry);
        }
        synthetic.set("rows", rowList);
        return Map.of(
                "type", HermesContentType.TRANSLATIONS.id(),
                "path", translationItemPath(itemPath),
                "yaml", synthetic.saveToString(),
                "exists", true,
                "draft", readDraft(HermesContentType.TRANSLATIONS, translationItemPath(itemPath))
        );
    }

    private CompletableFuture<Map<String, Object>> publishTranslations(String itemPath, String yaml) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                TranslationWriteResult writeResult = publishTranslationsWithoutReload(itemPath, yaml);
                List<Map<String, Object>> errors = new ArrayList<>(reloadPluginByName(writeResult.pluginName()));
                errors.addAll(writeResult.errors());
                future.complete(Map.of(
                        "success", true,
                        "errors", errors,
                        "path", writeResult.pluginName()
                ));
            } catch (Throwable e) {
                future.complete(Map.of("success", false, "errors", List.of(exceptionError("Publish failed", e, translationPluginName(itemPath)))));
            }
        });
        return future;
    }

    private TranslationWriteResult publishTranslationsWithoutReload(String itemPath, String yaml) throws IOException, InvalidConfigurationException {
        String pluginName = translationPluginName(itemPath);
        YamlConfiguration synthetic = new YamlConfiguration();
        synthetic.loadFromString(yaml == null ? "" : yaml);
        String configuredPlugin = synthetic.getString("plugin", pluginName);
        if (!pluginName.equals(configuredPlugin)) {
            throw new IllegalArgumentException("Translation document plugin does not match selected item.");
        }
        String translationItemPath = translationItemPath(itemPath);
        Path root = translationRoot(itemPath);
        Path englishPath = root.resolve("english.yml");
        Path germanPath = root.resolve("german.yml");
        YamlConfiguration english = loadLanguage(itemPath, "english.yml");
        YamlConfiguration german = loadLanguage(itemPath, "german.yml");
        Map<String, String> previousEnglish = flattenTranslations(english);
        Map<String, String> previousGerman = flattenTranslations(german);
        List<Map<String, String>> rows = translationRows(synthetic);
        Set<String> nextKeys = rows.stream().map(row -> row.getOrDefault("key", "")).filter(key -> !key.isBlank()).collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        for (String key : previousEnglish.keySet()) {
            if (!nextKeys.contains(key)) english.set(key, null);
        }
        for (String key : previousGerman.keySet()) {
            if (!nextKeys.contains(key)) german.set(key, null);
        }
        for (Map<String, String> row : rows) {
            String key = row.getOrDefault("key", "").trim();
            if (key.isBlank()) {
                continue;
            }
            english.set(key, languageValue(row.getOrDefault("english", "")));
            german.set(key, languageValue(row.getOrDefault("german", "")));
        }
        backup(englishPath, HermesContentType.TRANSLATIONS, pluginName + "-english.yml");
        backup(germanPath, HermesContentType.TRANSLATIONS, pluginName + "-german.yml");
        Files.createDirectories(root);
        english.save(englishPath.toFile());
        german.save(germanPath.toFile());
        deleteDraftIfExists(HermesContentType.TRANSLATIONS, translationItemPath);
        List<Map<String, Object>> errors = new ArrayList<>();
        errors.addAll(plugin.getGitExportService().markDirtyPath(translationGitPath(itemPath, "english.yml"), englishPath, false));
        errors.addAll(plugin.getGitExportService().markDirtyPath(translationGitPath(itemPath, "german.yml"), germanPath, false));
        return new TranslationWriteResult(pluginName, errors);
    }

    private TranslationValidation validateTranslations(String itemPath, String yaml) {
        List<Map<String, Object>> errors = new ArrayList<>();
        List<Map<String, Object>> warnings = new ArrayList<>();
        String pluginName = translationPluginName(itemPath);
        try {
            YamlConfiguration synthetic = new YamlConfiguration();
            synthetic.loadFromString(yaml == null ? "" : yaml);
            String configuredPlugin = synthetic.getString("plugin", pluginName);
            if (!pluginName.equals(configuredPlugin)) {
                errors.add(error("Plugin mismatch", "Translation document plugin does not match selected item.", pluginName));
            }
            Set<String> seen = new LinkedHashSet<>();
            for (Map<String, String> row : translationRows(synthetic)) {
                String key = row.getOrDefault("key", "").trim();
                if (key.isBlank()) {
                    errors.add(error("Empty translation key", "Translation rows need a non-empty key.", pluginName));
                    continue;
                }
                if (!seen.add(key)) {
                    errors.add(error("Duplicate translation key", "The key '" + key + "' appears more than once.", pluginName));
                }
                if (row.getOrDefault("english", "").isBlank()) {
                    warnings.add(error("Missing English text", key + " has no English value.", pluginName));
                }
                if (row.getOrDefault("german", "").isBlank()) {
                    warnings.add(error("Missing German text", key + " has no German value.", pluginName));
                }
            }
        } catch (InvalidConfigurationException e) {
            errors.add(exceptionError("Invalid translations", e, pluginName));
        }
        return new TranslationValidation(errors, warnings);
    }

    private void collectTranslationItems(String pluginName, Path pluginFolder, Set<String> itemPaths) {
        if (!Files.isDirectory(pluginFolder)) {
            return;
        }
        try (var stream = Files.walk(pluginFolder)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> !isIgnoredTranslationPath(path))
                    .filter(path -> isTranslationFile(path.getFileName().toString()))
                    .map(Path::getParent)
                    .filter(path -> path != null && (Files.isRegularFile(path.resolve("english.yml")) || Files.isRegularFile(path.resolve("german.yml"))))
                    .map(path -> translationItemPath(pluginName, pluginFolder, path))
                    .forEach(itemPaths::add);
        } catch (IOException ignored) {
        }
    }

    private boolean isTranslationFile(String fileName) {
        return fileName.equalsIgnoreCase("english.yml") || fileName.equalsIgnoreCase("german.yml");
    }

    private boolean isIgnoredTranslationPath(Path path) {
        String normalized = normalize(path);
        return normalized.contains("/backups/") || normalized.contains("/git-export/");
    }

    private String translationItemPath(String pluginName, Path pluginFolder, Path translationFolder) {
        String relative = normalize(pluginFolder.relativize(translationFolder));
        return relative.isBlank() || relative.equals(".") || relative.equals("languages") ? pluginName : pluginName + "/" + relative;
    }

    private YamlConfiguration loadLanguage(String itemPath, String fileName) {
        return YamlConfiguration.loadConfiguration(translationRoot(itemPath).resolve(fileName).toFile());
    }

    private Map<String, String> flattenTranslations(YamlConfiguration yaml) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : yaml.getKeys(true)) {
            if (yaml.isConfigurationSection(key)) {
                continue;
            }
            Object value = yaml.get(key);
            if (value instanceof List<?> list) {
                result.put(key, String.join("\n", list.stream().map(String::valueOf).toList()));
            } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
                result.put(key, String.valueOf(value));
            }
        }
        return result;
    }

    private Object languageValue(String value) {
        String text = value == null ? "" : value;
        if (text.contains("\n")) {
            return text.lines().toList();
        }
        return text;
    }

    private List<Map<String, String>> translationRows(YamlConfiguration yaml) {
        List<Map<String, String>> result = new ArrayList<>();
        for (Object raw : yaml.getList("rows", List.of())) {
            if (!(raw instanceof Map<?, ?> map)) {
                continue;
            }
            Map<String, String> row = new LinkedHashMap<>();
            row.put("key", stringValue(map, "key"));
            row.put("english", stringValue(map, "english"));
            row.put("german", stringValue(map, "german"));
            result.add(row);
        }
        return result;
    }

    private static String stringValue(Map<?, ?> map, String key) {
        Object value = map.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private Path translationRoot(String itemPath) {
        TranslationPath path = translationPath(itemPath);
        return pluginDataFolder(path.pluginName()).toPath().resolve(path.relativeFolder());
    }

    private static String translationPluginName(String itemPath) {
        return translationPath(itemPath).pluginName();
    }

    private static String translationItemPath(String itemPath) {
        TranslationPath path = translationPath(itemPath);
        return path.relativeFolder().equals("languages") ? path.pluginName() : path.pluginName() + "/" + path.relativeFolder();
    }

    private static String translationGitPath(String itemPath, String fileName) {
        TranslationPath path = translationPath(itemPath);
        return path.pluginName() + "/" + path.relativeFolder() + "/" + fileName;
    }

    private static TranslationPath translationPath(String itemPath) {
        String name = itemPath == null ? "" : itemPath.replace('\\', '/').trim();
        if (name.endsWith(".yml")) {
            name = name.substring(0, name.length() - ".yml".length());
        }
        if (name.contains("..") || name.isBlank() || name.startsWith("/") || name.endsWith("/")) {
            throw new IllegalArgumentException("Invalid translation plugin name");
        }
        int slash = name.indexOf('/');
        String pluginName = slash < 0 ? name : name.substring(0, slash);
        String relativeFolder = slash < 0 ? "languages" : name.substring(slash + 1);
        if (pluginName.isBlank() || relativeFolder.isBlank()) {
            throw new IllegalArgumentException("Invalid translation plugin name");
        }
        return new TranslationPath(pluginName, relativeFolder);
    }

    private List<Map<String, Object>> reloadSafely(HermesContentType type) {
        try {
            return reload(type);
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, type.label()));
        }
    }

    private List<Map<String, Object>> reloadPluginByName(String pluginName) {
        Plugin target = Bukkit.getPluginManager().getPlugin(pluginName);
        if (target == null || !target.isEnabled()) {
            return List.of(error("Reload skipped", pluginName + " is not enabled on this server.", pluginName));
        }
        return switch (pluginName.toLowerCase(java.util.Locale.ROOT)) {
            case "questsxl" -> reloadAllQxlContent();
            case "aether" -> reloadAether();
            case "hephaestus" -> reloadAllHephaestusContent();
            case "hecate" -> reloadHecate();
            case "spellbook" -> reloadSpellbookLibrary();
            case "factions" -> reloadFactionsBuildings();
            default -> List.of(error("Reload skipped", "Hermes does not know a safe reload path for " + pluginName + ".", pluginName));
        };
    }

    private List<Map<String, Object>> reloadAllQxlContent() {
        List<Map<String, Object>> issues = new ArrayList<>();
        issues.addAll(reload(HermesContentType.QUESTS));
        issues.addAll(reload(HermesContentType.EVENTS));
        issues.addAll(reload(HermesContentType.INTERACTIONS));
        issues.addAll(reload(HermesContentType.MACROS));
        issues.addAll(reload(HermesContentType.RESPAWNS));
        issues.addAll(reload(HermesContentType.EXPLORATION_SETS));
        issues.addAll(reload(HermesContentType.EXPLORABLES));
        issues.addAll(reload(HermesContentType.PERIODIC_QUESTS));
        issues.addAll(reload(HermesContentType.GLOBAL_OBJECTIVES));
        return issues;
    }

    private List<Map<String, Object>> reloadAllHephaestusContent() {
        List<Map<String, Object>> issues = new ArrayList<>();
        issues.addAll(reloadHephaestusItems());
        issues.addAll(reloadHephaestusJobs());
        issues.addAll(reloadHephaestusVanillaRecipes());
        issues.addAll(reloadHephaestusJobRecipes());
        issues.addAll(reloadHephaestusShops());
        return issues;
    }

    private List<Map<String, Object>> reload(HermesContentType type) {
        qxl.getErrors().clear();
        switch (type) {
            case QUESTS -> {
                qxl.getQuestManager().load();
                return List.of();
            }
            case EVENTS -> {
                qxl.getEventManager().load(QuestsXL.EVENTS);
                return List.of();
            }
            case INTERACTIONS -> {
                qxl.getInteractionManager().reload(QuestsXL.INTERACTIONS);
                return List.of();
            }
            case MACROS -> {
                qxl.getMacroRegistry().getGlobalMacros().clear();
                qxl.getMacroRegistry().loadFromDirectory(QuestsXL.MACROS);
                return List.of();
            }
            case PERIODIC_QUESTS -> {
                qxl.getPeriodicQuestManager().load();
                return List.of();
            }
            case GLOBAL_OBJECTIVES -> {
                try {
                    qxl.getGlobalObjectives().getObjectives().clear();
                    qxl.getGlobalObjectives().load(QuestsXL.GLOBAL_OBJ);
                    return List.of();
                } catch (InvalidConfigurationException e) {
                    return List.of(error("Reload failed", e.getMessage(), type.label()));
                }
            }
            case RESPAWNS -> {
                qxl.getRespawnPointManager().reload(QuestsXL.RESPAWNS);
                qxl.getExplorationManager().load();
                qxl.getLootChestManager().reloadLootChests();
                qxl.getExploration().initializeVFX();
                return List.of();
            }
            case EXPLORABLES, EXPLORATION_SETS -> {
                qxl.getExplorationManager().load();
                qxl.getLootChestManager().reloadLootChests();
                qxl.getExploration().initializeVFX();
                return List.of();
            }
            case AETHER_MOBS -> {
                return reloadAether();
            }
            case HEPHAESTUS_ITEMS, HEPHAESTUS_UPGRADES -> {
                return reloadHephaestusItems();
            }
            case HEPHAESTUS_JOBS -> {
                return reloadHephaestusJobs();
            }
            case HEPHAESTUS_VANILLA_RECIPES -> {
                return reloadHephaestusVanillaRecipes();
            }
            case HEPHAESTUS_JOB_RECIPES -> {
                return reloadHephaestusJobRecipes();
            }
            case HEPHAESTUS_SHOPS -> {
                return reloadHephaestusShops();
            }
            case HECATE_CLASSES, HECATE_TRAITLINES -> {
                return reloadHecate();
            }
            case SPELLBOOK_SPELLS, SPELLBOOK_TRAITS, SPELLBOOK_EFFECTS -> {
                return reloadSpellbookLibrary();
            }
            case FACTIONS_BUILDINGS -> {
                return reloadFactionsBuildings();
            }
            case FACTIONS_BUILDING_TAGS -> {
                return reloadFactionsBuildingTags();
            }
            default -> {
                return List.of(error("Reload skipped", "This content type was saved but needs a normal QXL reload before it is active.", type.label()));
            }
        }
    }

    private List<Map<String, Object>> reloadFactionsBuildings() {
        Plugin target = Bukkit.getPluginManager().getPlugin("Factions");
        if (!(target instanceof Factions factions) || !target.isEnabled()) {
            return List.of(error("Reload skipped", "Factions is not enabled on this server.", "Factions"));
        }
        try {
            factions.getBuildingManager().getTagManager().reload();
            factions.getBuildingManager().getBuildings().clear();
            factions.getBuildingManager().load(HermesContentType.FACTIONS_BUILDINGS.root());
            return List.of(error("Reload warning", "Factions building definitions were reloaded. Existing active building effect instances are not recreated by Hermes.", "Factions"));
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Factions"));
        }
    }

    private List<Map<String, Object>> reloadFactionsBuildingTags() {
        Plugin target = Bukkit.getPluginManager().getPlugin("Factions");
        if (!(target instanceof Factions factions) || !target.isEnabled()) {
            return List.of(error("Reload skipped", "Factions is not enabled on this server.", "Factions"));
        }
        try {
            factions.getBuildingManager().getTagManager().reload();
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Factions building tags"));
        }
    }

    private List<Map<String, Object>> reloadHephaestusJobs() {
        Plugin target = Bukkit.getPluginManager().getPlugin("Hephaestus");
        if (!(target instanceof Hephaestus hephaestus) || !target.isEnabled()) {
            return List.of(error("Reload skipped", "Hephaestus is not enabled on this server.", "Hephaestus"));
        }
        try {
            if (hephaestus.getJobManager() == null) {
                return List.of(error("Reload skipped", "Hephaestus job manager is not available.", "Hephaestus"));
            }
            hephaestus.getJobManager().reloadJobs();
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Hephaestus jobs"));
        }
    }

    private List<Map<String, Object>> reloadHephaestusVanillaRecipes() {
        Plugin target = Bukkit.getPluginManager().getPlugin("Hephaestus");
        if (!(target instanceof Hephaestus hephaestus) || !target.isEnabled()) {
            return List.of(error("Reload skipped", "Hephaestus is not enabled on this server.", "Hephaestus"));
        }
        try {
            if (hephaestus.getVanillaRecipeManager() == null) {
                return List.of(error("Reload skipped", "Hephaestus vanilla recipe manager is not available.", "Hephaestus"));
            }
            hephaestus.getVanillaRecipeManager().reload();
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Hephaestus vanilla recipes"));
        }
    }

    private List<Map<String, Object>> reloadHephaestusJobRecipes() {
        Plugin target = Bukkit.getPluginManager().getPlugin("Hephaestus");
        if (!(target instanceof Hephaestus hephaestus) || !target.isEnabled()) {
            return List.of(error("Reload skipped", "Hephaestus is not enabled on this server.", "Hephaestus"));
        }
        try {
            if (hephaestus.getRecipeManager() == null) {
                return List.of(error("Reload skipped", "Hephaestus job recipe manager is not available.", "Hephaestus"));
            }
            hephaestus.getRecipeManager().reloadRecipes();
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Hephaestus job recipes"));
        }
    }

    private List<Map<String, Object>> reloadHephaestusItems() {
        Plugin target = Bukkit.getPluginManager().getPlugin("Hephaestus");
        if (!(target instanceof Hephaestus hephaestus) || !target.isEnabled()) {
            return List.of(error("Reload skipped", "Hephaestus is not enabled on this server.", "Hephaestus"));
        }
        try {
            hephaestus.getLibrary().reload();
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Hephaestus"));
        }
    }

    private List<Map<String, Object>> reloadHephaestusShops() {
        Plugin target = Bukkit.getPluginManager().getPlugin("Hephaestus");
        if (!(target instanceof Hephaestus hephaestus) || !target.isEnabled()) {
            return List.of(error("Reload skipped", "Hephaestus is not enabled on this server.", "Hephaestus"));
        }
        try {
            if (hephaestus.getShopManager() == null) {
                return List.of(error("Reload skipped", "Hephaestus shop manager is not available.", "Hephaestus"));
            }
            hephaestus.getShopManager().reloadShops();
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Hephaestus shops"));
        }
    }

    private List<Map<String, Object>> reloadAether() {
        Plugin target = Bukkit.getPluginManager().getPlugin("Aether");
        if (!(target instanceof Aether aether) || !target.isEnabled()) {
            return List.of(error("Reload skipped", "Aether is not enabled on this server.", "Aether"));
        }
        try {
            aether.reload();
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Aether"));
        }
    }

    private List<Map<String, Object>> reloadHecate() {
        Hecate hecate = Hecate.getInstance();
        if (hecate == null || !hecate.isEnabled()) {
            return List.of(error("Reload skipped", "Hecate is not enabled on this server.", "Hecate"));
        }
        try {
            hecate.reloadContent();
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Hecate"));
        }
    }

    private List<Map<String, Object>> reloadSpellbookLibrary() {
        try {
            Object api = Bukkit.getServer().getClass().getMethod("getSpellbookAPI").invoke(Bukkit.getServer());
            Object library = api.getClass().getMethod("getLibrary").invoke(api);
            library.getClass().getMethod("reload").invoke(library);
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Spellbook"));
        }
    }

    private List<Map<String, Object>> errors() {
        return qxl.getErrors().stream().map(this::errorDto).toList();
    }

    private Map<String, Object> errorDto(FriendlyError error) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("title", blankToDefault(error.getTitle(), "QXL load error"));
        dto.put("message", blankToDefault(error.getException(), error.getMessage()));
        dto.put("location", blankToDefault(error.getLocation(), ""));
        dto.put("hint", blankToDefault(error.getHint(), ""));
        dto.put("stacktrace", cleanStacktrace(error.getStacktrace()));
        dto.put("stackPreview", blankToDefault(error.getConsoleStackPreview(), ""));
        return dto;
    }

    private void backup(Path live, HermesContentType type, String itemPath) throws IOException {
        if (!Files.exists(live)) {
            return;
        }
        String safeName = displayPath(type, itemPath).replace('\\', '_').replace('/', '_').replace(':', '_');
        Path backup = backupRoot.resolve(type.id()).resolve(LocalDateTime.now().format(BACKUP_FORMAT) + "-" + safeName);
        Files.createDirectories(backup.getParent());
        Files.copy(live, backup, StandardCopyOption.REPLACE_EXISTING);
    }

    private ValidationResult validateYaml(String yaml) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(yaml == null ? "" : yaml);
            return new ValidationResult(true, List.of());
        } catch (InvalidConfigurationException e) {
            return new ValidationResult(false, List.of(error("Invalid YAML", e.getMessage(), "draft")));
        }
    }

    private ValidationResult validateHephaestusItem(String yaml) {
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(yaml == null ? "" : yaml);
            for (String key : List.of("vanilla", "patch")) {
                String json = configuration.getString(key);
                if (json == null || json.isBlank()) {
                    continue;
                }
                JsonParser.parseString(json);
            }
            return new ValidationResult(true, List.of());
        } catch (Exception e) {
            return new ValidationResult(false, List.of(error("Invalid Hephaestus item", e.getMessage(), "draft")));
        }
    }

    private ValidationResult validateHephaestusContent(HermesContentType type, String itemPath, String yaml) {
        ValidationResult syntax = validateYaml(yaml);
        if (!syntax.valid()) {
            return syntax;
        }
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(yaml == null ? "" : yaml);
            switch (type) {
                case HEPHAESTUS_UPGRADES -> validateHephaestusUpgrade(itemPath, configuration);
                case HEPHAESTUS_JOBS -> HJob.deserialize(configuration);
                case HEPHAESTUS_VANILLA_RECIPES -> {
                    YamlConfiguration wrapper = new YamlConfiguration();
                    ConfigurationSection recipes = wrapper.createSection("recipes");
                    copySection(configuration, recipes.createSection(virtualKey(type, itemPath)));
                    VanillaRecipe.deserialize(recipes.getConfigurationSection(virtualKey(type, itemPath)));
                }
                case HEPHAESTUS_JOB_RECIPES -> JobRecipe.deserialize(virtualKey(type, itemPath), configuration);
                case HEPHAESTUS_SHOPS -> validateHephaestusShop(configuration);
                default -> {
                }
            }
            return new ValidationResult(true, List.of());
        } catch (Exception e) {
            return new ValidationResult(false, List.of(error("Invalid " + type.label(), e.getMessage(), displayPath(type, itemPath))));
        }
    }

    private void validateHephaestusUpgrade(String itemPath, YamlConfiguration configuration) {
        String type = configuration.getString("type", "");
        if (type.isBlank()) {
            throw new IllegalArgumentException("Upgrade type is required.");
        }
        if (configuration.getString("id", "").isBlank()) {
            throw new IllegalArgumentException("Upgrade id is required.");
        }
        File temp = new File(plugin.getDataFolder(), "hermes-validation-" + System.nanoTime() + ".yml");
        try {
            configuration.save(temp);
            HItemUpgrade upgrade = HItemUpgrade.createInstance(temp);
            if (upgrade == null) {
                throw new IllegalArgumentException("Unknown upgrade type: " + type);
            }
            upgrade.load(temp);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage() == null ? "Invalid upgrade: " + itemPath : e.getMessage(), e);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            temp.delete();
        }
    }

    private void validateHephaestusShop(YamlConfiguration configuration) {
        if (configuration.getString("name", "").isBlank()) {
            throw new IllegalArgumentException("Shop name is required.");
        }
        ConfigurationSection items = configuration.getConfigurationSection("items");
        if (items == null) {
            throw new IllegalArgumentException("Shop items section is required.");
        }
        Set<String> knownItems = new LinkedHashSet<>();
        try {
            for (Map<String, Object> item : new QxlAssetCatalogService().items()) {
                Object id = item.get("id");
                if (id != null) {
                    knownItems.add(String.valueOf(id));
                }
            }
        } catch (Throwable ignored) {
        }
        for (String itemId : items.getKeys(false)) {
            ConfigurationSection item = items.getConfigurationSection(itemId);
            if (item == null) {
                throw new IllegalArgumentException("Shop item '" + itemId + "' must be a map.");
            }
            if (!knownItems.isEmpty() && !knownItems.contains(itemId)) {
                throw new IllegalArgumentException("Unknown Hephaestus item in shop: " + itemId);
            }
            String type = item.getString("type", "BUY").toUpperCase(java.util.Locale.ROOT);
            if (!Set.of("BUY", "SELL", "BOTH").contains(type)) {
                throw new IllegalArgumentException("Invalid transaction type for " + itemId + ": " + type);
            }
            for (String numberKey : List.of("buyPrice", "sellPrice", "restockAmount", "restockTime")) {
                if (item.contains(numberKey) && !item.isInt(numberKey) && !item.isDouble(numberKey) && !item.isLong(numberKey)) {
                    throw new IllegalArgumentException(numberKey + " for " + itemId + " must be numeric.");
                }
            }
        }
    }

    private ValidationResult validateFactionsBuilding(String itemPath, String yaml) {
        ValidationResult syntax = validateYaml(yaml);
        if (!syntax.valid()) {
            return syntax;
        }
        List<Map<String, Object>> issues = new ArrayList<>();
        try {
            YamlConfiguration configuration = new YamlConfiguration();
            configuration.loadFromString(yaml == null ? "" : yaml);
            if (!configuration.contains("size")) {
                issues.add(error("Missing size", "Factions buildings should define a positive size.", displayPath(HermesContentType.FACTIONS_BUILDINGS, itemPath)));
            } else if (configuration.getInt("size", 0) <= 0) {
                issues.add(error("Invalid size", "Building size must be greater than 0.", displayPath(HermesContentType.FACTIONS_BUILDINGS, itemPath)));
            }
            var effects = configuration.getConfigurationSection("effects");
            if (effects != null) {
                for (String key : effects.getKeys(false)) {
                    String type = key.replaceAll("_.*$", "");
                    try {
                        Class<?> clazz = Class.forName("de.erethon.factions.building.effects." + type);
                        if (!de.erethon.factions.building.BuildingEffect.class.isAssignableFrom(clazz)) {
                            issues.add(error("Invalid building effect", type + " does not extend BuildingEffect.", "effects." + key));
                        }
                    } catch (ClassNotFoundException e) {
                        issues.add(error("Unknown building effect", "No Factions effect class exists for " + type + ".", "effects." + key));
                    }
                    Object raw = effects.get(key);
                    if (!(raw instanceof ConfigurationSection) && !(raw instanceof Map)) {
                        issues.add(error("Invalid building effect", "Effect " + key + " must be a YAML map.", "effects." + key));
                    }
                }
            }
            return new ValidationResult(issues.isEmpty(), issues);
        } catch (Exception e) {
            return new ValidationResult(false, List.of(error("Invalid Factions building", e.getMessage(), displayPath(HermesContentType.FACTIONS_BUILDINGS, itemPath))));
        }
    }

    private List<Map<String, Object>> listVirtualHephaestusItems(HermesContentType type) throws IOException {
        List<Map<String, Object>> result = new ArrayList<>();
        if (type == HermesContentType.HEPHAESTUS_JOBS || type == HermesContentType.HEPHAESTUS_VANILLA_RECIPES) {
            Path file = virtualRootFile(type);
            if (!Files.exists(file)) {
                return result;
            }
            YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
            ConfigurationSection root = config.getConfigurationSection(virtualRootSection(type));
            if (root == null) {
                return result;
            }
            for (String key : root.getKeys(false).stream().sorted().toList()) {
                result.add(virtualItemDto(type, virtualFileName(type) + "#" + key, file, key));
            }
            return result;
        }
        Path root = requireRoot(type);
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml") || path.getFileName().toString().endsWith(".yaml"))
                    .sorted(Comparator.comparing(path -> root.relativize(path).toString()))
                    .forEach(path -> collectVirtualRecipeItems(type, root, path, result));
        }
        result.sort(Comparator
                .<Map<String, Object>, String>comparing(row -> String.valueOf(row.get("id")))
                .thenComparing(row -> String.valueOf(row.get("path"))));
        return result;
    }

    private void collectVirtualRecipeItems(HermesContentType type, Path root, Path path, List<Map<String, Object>> result) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
        ConfigurationSection recipes = config.getConfigurationSection("recipes");
        if (recipes == null) {
            return;
        }
        String relative = normalize(root.relativize(path));
        for (String key : recipes.getKeys(false).stream().sorted().toList()) {
            String jobId = recipes.getString(key + ".jobId", "");
            String id = jobId == null || jobId.isBlank() ? key : jobId + "/" + key;
            result.add(virtualItemDto(type, relative + "#" + key, path, id));
        }
    }

    private Map<String, Object> readVirtualHephaestus(HermesContentType type, String itemPath) throws IOException {
        VirtualPath virtual = resolveVirtualPath(type, itemPath, false);
        YamlConfiguration config = YamlConfiguration.loadConfiguration(virtual.file().toFile());
        ConfigurationSection root = config.getConfigurationSection(virtualRootSection(type));
        ConfigurationSection section = root == null ? null : root.getConfigurationSection(virtual.key());
        String yaml = section == null ? "" : sectionToYaml(section);
        return Map.of(
                "type", type.id(),
                "path", virtual.displayPath(),
                "yaml", yaml,
                "exists", section != null,
                "draft", readDraft(type, virtual.displayPath())
        );
    }

    private void publishVirtualHephaestus(HermesContentType type, String itemPath, String yaml) throws IOException, InvalidConfigurationException {
        VirtualPath virtual = resolveVirtualPath(type, itemPath, true);
        backup(virtual.file(), type, virtual.displayPath());
        Files.createDirectories(virtual.file().getParent());
        YamlConfiguration config = Files.exists(virtual.file()) ? YamlConfiguration.loadConfiguration(virtual.file().toFile()) : new YamlConfiguration();
        YamlConfiguration entry = new YamlConfiguration();
        entry.loadFromString(yaml == null ? "" : yaml);
        ConfigurationSection root = config.getConfigurationSection(virtualRootSection(type));
        if (root == null) {
            root = config.createSection(virtualRootSection(type));
        }
        root.set(virtual.key(), null);
        copySection(entry, root.createSection(virtual.key()));
        config.save(virtual.file().toFile());
    }

    private void deleteVirtualHephaestus(HermesContentType type, String itemPath) throws IOException {
        VirtualPath virtual = resolveVirtualPath(type, itemPath, false);
        backup(virtual.file(), type, virtual.displayPath());
        YamlConfiguration config = YamlConfiguration.loadConfiguration(virtual.file().toFile());
        ConfigurationSection root = config.getConfigurationSection(virtualRootSection(type));
        if (root != null) {
            root.set(virtual.key(), null);
            config.save(virtual.file().toFile());
        }
    }

    private void moveVirtualHephaestus(HermesContentType type, String itemPath, String nextPath) throws IOException, InvalidConfigurationException {
        VirtualPath source = resolveVirtualPath(type, itemPath, false);
        VirtualPath target = resolveVirtualPath(type, nextPath, true);
        if (Files.exists(target.file())) {
            YamlConfiguration targetConfig = YamlConfiguration.loadConfiguration(target.file().toFile());
            ConfigurationSection targetRoot = targetConfig.getConfigurationSection(virtualRootSection(type));
            if (targetRoot != null && targetRoot.contains(target.key())) {
                throw new IllegalArgumentException("Target entry already exists: " + target.displayPath());
            }
        }
        YamlConfiguration sourceConfig = YamlConfiguration.loadConfiguration(source.file().toFile());
        ConfigurationSection sourceRoot = sourceConfig.getConfigurationSection(virtualRootSection(type));
        ConfigurationSection sourceSection = sourceRoot == null ? null : sourceRoot.getConfigurationSection(source.key());
        if (sourceSection == null) {
            throw new IllegalArgumentException("Source entry does not exist: " + source.displayPath());
        }
        String yaml = sectionToYaml(sourceSection);
        publishVirtualHephaestus(type, target.displayPath(), yaml);
        deleteVirtualHephaestus(type, source.displayPath());
    }

    private VirtualPath resolveVirtualPath(HermesContentType type, String itemPath, boolean allowCreate) throws IOException {
        String normalized = normalizeVirtualItemPath(type, itemPath);
        int hash = normalized.lastIndexOf('#');
        if (hash <= 0 || hash == normalized.length() - 1) {
            throw new IllegalArgumentException("Virtual Hephaestus entries use file.yml#entryId paths.");
        }
        String filePart = normalized.substring(0, hash);
        String key = normalized.substring(hash + 1);
        Path file;
        if (type == HermesContentType.HEPHAESTUS_JOBS || type == HermesContentType.HEPHAESTUS_VANILLA_RECIPES) {
            file = virtualRootFile(type);
            filePart = virtualFileName(type);
        } else {
            Path root = requireRoot(type);
            file = root.resolve(filePart).normalize();
            if (!file.startsWith(root.normalize())) {
                throw new IllegalArgumentException("Invalid content path");
            }
            if (!file.getFileName().toString().endsWith(".yml") && !file.getFileName().toString().endsWith(".yaml")) {
                throw new IllegalArgumentException("Job recipe files must be YAML files.");
            }
        }
        if (!allowCreate && !Files.exists(file)) {
            throw new IllegalArgumentException("Content file does not exist: " + filePart);
        }
        return new VirtualPath(file, key, filePart + "#" + key);
    }

    private Path virtualContentPath(HermesContentType type, String itemPath) throws IOException {
        return resolveVirtualPath(type, itemPath, true).file();
    }

    private Path virtualRootFile(HermesContentType type) {
        Path root = pluginDataFolder("Hephaestus").toPath();
        if (type == HermesContentType.HEPHAESTUS_JOBS) {
            return root.resolve("jobs.yml");
        }
        if (type == HermesContentType.HEPHAESTUS_VANILLA_RECIPES) {
            return root.resolve("vanilla_recipes.yml");
        }
        throw new IllegalArgumentException("No aggregate file for " + type.id());
    }

    private String virtualFileName(HermesContentType type) {
        if (type == HermesContentType.HEPHAESTUS_JOBS) {
            return "jobs.yml";
        }
        if (type == HermesContentType.HEPHAESTUS_VANILLA_RECIPES) {
            return "vanilla_recipes.yml";
        }
        return "";
    }

    private static String virtualRootSection(HermesContentType type) {
        if (type == HermesContentType.HEPHAESTUS_JOBS) {
            return "jobs";
        }
        return "recipes";
    }

    private static String normalizeVirtualItemPath(HermesContentType type, String itemPath) {
        String path = itemPath == null ? "" : itemPath.replace('\\', '/').trim();
        if (path.isBlank()) {
            throw new IllegalArgumentException("Missing item path");
        }
        if (!path.contains("#")) {
            if (type == HermesContentType.HEPHAESTUS_JOBS) {
                return "jobs.yml#" + path.replaceFirst("\\.ya?ml$", "");
            }
            if (type == HermesContentType.HEPHAESTUS_VANILLA_RECIPES) {
                return "vanilla_recipes.yml#" + path.replaceFirst("\\.ya?ml$", "");
            }
            path = (path.endsWith(".yml") || path.endsWith(".yaml")) ? path : path + ".yml";
            return path + "#" + idFromPath(path);
        }
        String file = path.substring(0, path.lastIndexOf('#'));
        String key = path.substring(path.lastIndexOf('#') + 1).trim();
        if (type == HermesContentType.HEPHAESTUS_JOBS) {
            file = "jobs.yml";
        } else if (type == HermesContentType.HEPHAESTUS_VANILLA_RECIPES) {
            file = "vanilla_recipes.yml";
        } else if (!file.endsWith(".yml") && !file.endsWith(".yaml")) {
            file = file + ".yml";
        }
        return file + "#" + key;
    }

    private String sectionToYaml(ConfigurationSection section) {
        YamlConfiguration out = new YamlConfiguration();
        for (String key : section.getKeys(false)) {
            out.set(key, section.get(key));
        }
        return out.saveToString();
    }

    private void copySection(ConfigurationSection from, ConfigurationSection to) {
        for (String key : from.getKeys(false)) {
            Object value = from.get(key);
            if (value instanceof ConfigurationSection child) {
                copySection(child, to.createSection(key));
            } else {
                to.set(key, value);
            }
        }
    }

    private Map<String, Object> virtualItemDto(HermesContentType type, String displayPath, Path path, String id) {
        Map<String, Object> dto = itemDto(type, displayPath, path, true);
        dto.put("id", id);
        return dto;
    }

    private static boolean isHephaestusContent(HermesContentType type) {
        return type == HermesContentType.HEPHAESTUS_UPGRADES
                || type == HermesContentType.HEPHAESTUS_JOBS
                || type == HermesContentType.HEPHAESTUS_VANILLA_RECIPES
                || type == HermesContentType.HEPHAESTUS_JOB_RECIPES
                || type == HermesContentType.HEPHAESTUS_SHOPS;
    }

    private static boolean isVirtualHephaestusType(HermesContentType type) {
        return type == HermesContentType.HEPHAESTUS_JOBS
                || type == HermesContentType.HEPHAESTUS_VANILLA_RECIPES
                || type == HermesContentType.HEPHAESTUS_JOB_RECIPES;
    }

    private static String virtualKey(HermesContentType type, String itemPath) {
        String normalized = normalizeVirtualItemPath(type, itemPath);
        return normalized.substring(normalized.lastIndexOf('#') + 1);
    }

    private Map<String, Object> readDraft(HermesContentType type, String itemPath) throws IOException {
        Path draft = resolveDraftPath(type, itemPath);
        if (!Files.exists(draft)) {
            return Map.of("exists", false);
        }
        return Map.of(
                "exists", true,
                "yaml", Files.readString(draft, StandardCharsets.UTF_8),
                "updatedAt", Files.getLastModifiedTime(draft).toInstant().toString()
        );
    }

    private void deleteDraftIfExists(HermesContentType type, String itemPath) throws IOException {
        Files.deleteIfExists(resolveDraftPath(type, itemPath));
    }

    private static void requireDirectoryType(HermesContentType type, String operation) {
        if (!type.directory()) {
            throw new IllegalArgumentException(operation + " is only supported for file-based content types.");
        }
    }

    private Map<String, Object> itemDto(HermesContentType type, String displayPath, Path path, boolean exists) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("type", type.id());
        dto.put("path", displayPath);
        dto.put("id", idFromPath(displayPath));
        dto.put("exists", exists);
        if (Files.exists(path)) {
            try {
                dto.put("updatedAt", Files.getLastModifiedTime(path).toInstant().toString());
                dto.put("size", Files.size(path));
            } catch (IOException ignored) {
            }
        }
        return dto;
    }

    private Path resolveContentPath(HermesContentType type, String itemPath, boolean allowCreate) throws IOException {
        if (!type.directory()) {
            return type.root().toPath();
        }
        Path root = requireRoot(type);
        String normalized = normalizeItemPath(itemPath);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Missing item path");
        }
        Path target = root.resolve(normalized).normalize();
        if (!target.startsWith(root.normalize())) {
            throw new IllegalArgumentException("Invalid content path");
        }
        if (!allowCreate && !Files.exists(target)) {
            throw new IllegalArgumentException("Content file does not exist: " + normalized);
        }
        return target;
    }

    private Path resolveDraftPath(HermesContentType type, String itemPath) {
        String relative = isVirtualHephaestusType(type)
                ? normalizeVirtualItemPath(type, itemPath)
                : type == HermesContentType.TRANSLATIONS ? translationPluginName(itemPath) + ".yml"
                : type.directory() ? normalizeItemPath(itemPath) : type.root().getName();
        relative = relative.replace('#', '_');
        Path target = draftRoot.resolve(type.id()).resolve(relative).normalize();
        Path allowed = draftRoot.resolve(type.id()).normalize();
        if (!target.startsWith(allowed)) {
            throw new IllegalArgumentException("Invalid draft path");
        }
        return target;
    }

    private Path requireRoot(HermesContentType type) throws IOException {
        Path root = type.root().toPath();
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        return root;
    }

    private static String normalizeItemPath(String itemPath) {
        String path = itemPath == null ? "" : itemPath.replace('\\', '/').trim();
        if (path.isBlank()) {
            return "";
        }
        if (!path.endsWith(".yml") && !path.endsWith(".yaml")) {
            path = path + ".yml";
        }
        return path;
    }

    private static boolean matchesContentExtension(HermesContentType type, Path path) {
        String name = path.getFileName().toString().toLowerCase();
        if (type == HermesContentType.DAEDALUS_MODELS) {
            return name.endsWith(".bbmodel");
        }
        return name.endsWith(".yml") || name.endsWith(".yaml");
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String displayPath(HermesContentType type, String itemPath) {
        if (isVirtualHephaestusType(type)) {
            return normalizeVirtualItemPath(type, itemPath);
        }
        if (type == HermesContentType.TRANSLATIONS) {
            return translationPluginName(itemPath);
        }
        return type.directory() ? normalizeItemPath(itemPath) : type.root().getName();
    }

    private static File pluginDataFolder(String pluginName) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        return plugin == null ? new File("plugins", pluginName) : plugin.getDataFolder();
    }

    private static String idFromPath(String path) {
        String file = path.replace('\\', '/');
        int slash = file.lastIndexOf('/');
        if (slash >= 0) {
            file = file.substring(slash + 1);
        }
        int dot = file.lastIndexOf('.');
        return dot > 0 ? file.substring(0, dot) : file;
    }

    private static Map<String, Object> error(String title, String message, String location) {
        return Map.of(
                "title", title == null ? "Error" : title,
                "message", message == null ? "" : message,
                "location", location == null ? "" : location
        );
    }

    private static Map<String, Object> exceptionError(String title, Throwable throwable, String location) {
        Map<String, Object> dto = new HashMap<>();
        dto.put("title", title == null ? "Error" : title);
        dto.put("message", throwable.getMessage() == null ? throwable.toString() : throwable.getMessage());
        dto.put("location", location == null ? "" : location);
        dto.put("hint", "");
        dto.put("stacktrace", stacktrace(throwable, 32));
        dto.put("stackPreview", stacktrace(throwable, 3));
        return dto;
    }

    private static String stacktrace(Throwable throwable, int maxLines) {
        StringBuilder builder = new StringBuilder();
        StackTraceElement[] trace = throwable.getStackTrace();
        for (int i = 0; i < Math.min(trace.length, maxLines); i++) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(trace[i]);
        }
        return builder.toString();
    }

    private static String cleanStacktrace(String stacktrace) {
        if (stacktrace == null) {
            return "";
        }
        return stacktrace.replace("<gray>", "").trim();
    }

    private static String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private record ValidationResult(boolean valid, List<Map<String, Object>> errors) {
    }

    private record TranslationValidation(List<Map<String, Object>> errors, List<Map<String, Object>> warnings) {
    }

    private record BatchItem(HermesContentType type, String path, String yaml) {
    }

    private record PublishWriteResult(String path, String reloadGroup, List<Map<String, Object>> errors) {
    }

    private record TranslationWriteResult(String pluginName, List<Map<String, Object>> errors) {
    }

    private record TranslationPath(String pluginName, String relativeFolder) {
    }

    private record VirtualPath(Path file, String key, String displayPath) {
    }
}
