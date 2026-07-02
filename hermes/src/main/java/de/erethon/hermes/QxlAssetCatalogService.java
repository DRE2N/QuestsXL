package de.erethon.hermes;

import com.destroystokyo.paper.MaterialSetTag;
import com.destroystokyo.paper.MaterialTags;
import de.erethon.aether.Aether;
import de.erethon.aether.creature.NPCData;
import de.erethon.factions.economy.population.PopulationLevel;
import de.erethon.factions.economy.resource.Resource;
import de.erethon.factions.region.RegionType;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.items.HItem;
import de.erethon.hephaestus.items.HRarity;
import de.erethon.hephaestus.jobs.HJob;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class QxlAssetCatalogService {

    public Map<String, Object> catalog() {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> catalogWarnings = new ArrayList<>(warnings());
        result.put("quests", safeAssets("quests", () -> qxlAssets(de.erethon.questsxl.QuestsXL.QUESTS), catalogWarnings));
        result.put("events", safeAssets("events", () -> qxlAssets(de.erethon.questsxl.QuestsXL.EVENTS), catalogWarnings));
        result.put("items", safeAssets("hephaestus.items", this::items, catalogWarnings));
        result.put("hephaestusUpgrades", safeAssets("hephaestus.upgrades", this::hephaestusUpgrades, catalogWarnings));
        result.put("hephaestusJobs", safeAssets("hephaestus.jobs", this::hephaestusJobs, catalogWarnings));
        result.put("hephaestusVanillaRecipes", safeAssets("hephaestus.vanillaRecipes", this::hephaestusVanillaRecipes, catalogWarnings));
        result.put("hephaestusJobRecipes", safeAssets("hephaestus.jobRecipes", this::hephaestusJobRecipes, catalogWarnings));
        result.put("hephaestusShops", safeAssets("hephaestus.shops", this::hephaestusShops, catalogWarnings));
        result.put("hephaestusRarities", safeAssets("hephaestus.rarities", this::hephaestusRarities, catalogWarnings));
        result.put("hephaestusUpgradeTypes", safeAssets("hephaestus.upgradeTypes", this::hephaestusUpgradeTypes, catalogWarnings));
        result.put("mobs", safeAssets("aether.mobs", this::mobs, catalogWarnings));
        result.put("dialogues", safeAssets("dialogues", this::dialogues, catalogWarnings));
        result.put("hecateClasses", safeAssets("hecate.classes", () -> fileAssets("Hecate", "classes"), catalogWarnings));
        result.put("hecateTraitlines", safeAssets("hecate.traitlines", () -> fileAssets("Hecate", "traitlines"), catalogWarnings));
        result.put("spellbookSpells", safeAssets("spellbook.spells", () -> spellbookAssets("spells"), catalogWarnings));
        result.put("spellbookTraits", safeAssets("spellbook.traits", () -> spellbookAssets("traits"), catalogWarnings));
        result.put("spellbookEffects", safeAssets("spellbook.effects", () -> spellbookAssets("effects"), catalogWarnings));
        result.put("entityTypes", safeAssets("entityTypes", this::entityTypes, catalogWarnings));
        result.put("worlds", safeAssets("worlds", this::worlds, catalogWarnings));
        result.put("materials", safeAssets("materials", this::materials, catalogWarnings));
        result.put("attributes", safeAssets("attributes", this::attributes, catalogWarnings));
        result.put("factionsBuildings", safeAssets("factions.buildings", () -> fileAssets("Factions", "buildings"), catalogWarnings));
        result.put("factionsBuildingEffects", safeAssets("factions.buildingEffects", this::factionsBuildingEffects, catalogWarnings));
        result.put("factionsResources", safeAssets("factions.resources", this::factionsResources, catalogWarnings));
        result.put("factionsPopulationLevels", safeAssets("factions.populationLevels", this::factionsPopulationLevels, catalogWarnings));
        result.put("factionsRegionTypes", safeAssets("factions.regionTypes", this::factionsRegionTypes, catalogWarnings));
        result.put("factionsBuildingTags", safeAssets("factions.buildingTags", this::factionsBuildingTags, catalogWarnings));
        result.put("factionsMinecraftTags", safeAssets("factions.minecraftTags", this::factionsMinecraftTags, catalogWarnings));
        result.put("factionsAttributes", safeAssets("factions.attributes", this::factionsAttributes, catalogWarnings));
        result.put("warnings", catalogWarnings);
        return result;
    }

    public List<Map<String, Object>> items() {
        Plugin hephaestus = Bukkit.getPluginManager().getPlugin("Hephaestus");
        if (!(hephaestus instanceof Hephaestus plugin) || !hephaestus.isEnabled()) {
            return hephaestusFileItems();
        }
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            for (HItem item : plugin.getLibrary().getItems()) {
                Identifier key = item.getKey();
                if (key == null) {
                    continue;
                }
                String id = key.toString();
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", id);
                dto.put("label", id);
                dto.put("baseItem", identifierString(item.getBaseItemKey()));
                dto.put("modelKey", identifierString(item.getEffectiveItemModel()));
                dto.put("tags", item.getTags().stream().sorted().toList());
                result.add(dto);
            }
            List<Map<String, Object>> liveItems = sortedUniqueById(result);
            return liveItems.isEmpty() ? hephaestusFileItems() : liveItems;
        } catch (Throwable e) {
            return hephaestusFileItems();
        }
    }

    private List<Map<String, Object>> hephaestusFileItems() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Hephaestus");
        Path root = (plugin == null ? new java.io.File("plugins", "Hephaestus") : plugin.getDataFolder()).toPath().resolve("items");
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml") || path.getFileName().toString().endsWith(".yaml"))
                    .map(path -> hephaestusFileItem(root, path))
                    .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), this::sortedUniqueById));
        } catch (IOException e) {
            return List.of();
        }
    }

    private Map<String, Object> hephaestusFileItem(Path root, Path path) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
        String file = path.getFileName().toString();
        String fallbackId = file.replaceFirst("\\.ya?ml$", "");
        String id = config.getString("id", fallbackId);
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", id == null || id.isBlank() ? fallbackId : id);
        dto.put("label", root.relativize(path).toString().replace('\\', '/'));
        dto.put("baseItem", config.getString("baseItem", ""));
        dto.put("modelKey", config.getString("itemModel", ""));
        dto.put("tags", config.getStringList("tags"));
        return dto;
    }

    public List<Map<String, Object>> hephaestusUpgrades() {
        Plugin hephaestus = Bukkit.getPluginManager().getPlugin("Hephaestus");
        if (hephaestus instanceof Hephaestus plugin && hephaestus.isEnabled()) {
            return plugin.getLibrary().getUpgradeKeys().stream()
                    .sorted()
                    .map(id -> Map.<String, Object>of("id", id, "label", id))
                    .toList();
        }
        return fileAssets("Hephaestus", "upgrades");
    }

    public List<Map<String, Object>> hephaestusJobs() {
        Plugin hephaestus = Bukkit.getPluginManager().getPlugin("Hephaestus");
        if (hephaestus instanceof Hephaestus plugin && hephaestus.isEnabled() && plugin.getJobManager() != null) {
            return plugin.getJobManager().getAllJobs().stream()
                    .map(this::hephaestusJobAsset)
                    .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), this::sortedUniqueById));
        }
        Path file = hephaestusRoot().resolve("jobs.yml");
        return keyedYamlAssets(file, "jobs");
    }

    private Map<String, Object> hephaestusJobAsset(HJob job) {
        return Map.of(
                "id", job.getId(),
                "label", job.getId(),
                "maxLevel", job.getMaxLevel(),
                "block", job.getCraftingBlock().getKey().toString()
        );
    }

    public List<Map<String, Object>> hephaestusVanillaRecipes() {
        return keyedYamlAssets(hephaestusRoot().resolve("vanilla_recipes.yml"), "recipes");
    }

    public List<Map<String, Object>> hephaestusJobRecipes() {
        Path root = hephaestusRoot().resolve("recipes");
        if (!Files.exists(root)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml") || path.getFileName().toString().endsWith(".yaml"))
                    .forEach(path -> {
                        String relative = root.relativize(path).toString().replace('\\', '/');
                        for (Map<String, Object> asset : keyedYamlAssets(path, "recipes")) {
                            Map<String, Object> dto = new LinkedHashMap<>(asset);
                            dto.put("path", relative + "#" + asset.get("id"));
                            YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
                            String jobId = config.getString("recipes." + asset.get("id") + ".jobId", "");
                            if (jobId != null && !jobId.isBlank()) {
                                dto.put("label", jobId + "/" + asset.get("id"));
                            }
                            result.add(dto);
                        }
                    });
        } catch (IOException ignored) {
        }
        result.sort(Comparator.comparing(row -> String.valueOf(row.getOrDefault("label", row.get("id")))));
        return result;
    }

    public List<Map<String, Object>> hephaestusShops() {
        Plugin hephaestus = Bukkit.getPluginManager().getPlugin("Hephaestus");
        if (hephaestus instanceof Hephaestus plugin && hephaestus.isEnabled() && plugin.getShopManager() != null) {
            return plugin.getShopManager().getShops().values().stream()
                    .map(shop -> {
                        Map<String, Object> dto = new LinkedHashMap<>();
                        dto.put("id", shop.getId());
                        dto.put("label", shop.getDisplayName() == null || shop.getDisplayName().isBlank() ? shop.getId() : shop.getDisplayName());
                        return dto;
                    })
                    .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), this::sortedUniqueById));
        }
        return fileAssets("Hephaestus", "shops");
    }

    public List<Map<String, Object>> hephaestusRarities() {
        return Arrays.stream(HRarity.values())
                .map(Enum::name)
                .sorted()
                .map(id -> Map.<String, Object>of("id", id, "label", id))
                .toList();
    }

    public List<Map<String, Object>> hephaestusUpgradeTypes() {
        return List.of(Map.of("id", "attribute_modifying", "label", "attribute_modifying"));
    }

    private List<Map<String, Object>> keyedYamlAssets(Path file, String rootKey) {
        if (!Files.exists(file)) {
            return List.of();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        var root = config.getConfigurationSection(rootKey);
        if (root == null) {
            return List.of();
        }
        return root.getKeys(false).stream()
                .sorted()
                .map(id -> Map.<String, Object>of("id", id, "label", id))
                .toList();
    }

    private Path hephaestusRoot() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Hephaestus");
        return (plugin == null ? new java.io.File("plugins", "Hephaestus") : plugin.getDataFolder()).toPath();
    }

    public List<Map<String, Object>> mobs() {
        Plugin aether = Bukkit.getPluginManager().getPlugin("Aether");
        if (!(aether instanceof Aether plugin) || !aether.isEnabled()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> result = new ArrayList<>();
            for (NPCData mob : plugin.getCreatureManager().getCreatures()) {
                String id = mob.getID();
                if (id == null || id.isBlank()) {
                    continue;
                }
                Map<String, Object> dto = new LinkedHashMap<>();
                dto.put("id", id);
                dto.put("label", id);
                dto.put("displayName", mob.getDisplayName() == null ? "" : PlainTextComponentSerializer.plainText().serialize(mob.getDisplayName()));
                dto.put("displayType", mob.getDisplayType() == null ? "" : BuiltInRegistries.ENTITY_TYPE.getKey(mob.getDisplayType()).toString());
                dto.put("category", mob.getMobCategoryOverride() == null ? "" : mob.getMobCategoryOverride().getName());
                dto.put("version", mob.getCurrentVersion());
                result.add(dto);
            }
            return sortedUniqueById(result);
        } catch (Throwable e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> dialogues() {
        Path root = de.erethon.questsxl.QuestsXL.DIALOGUES.toPath();
        return qxlAssets(root);
    }

    public List<Map<String, Object>> quests() {
        return qxlAssets(de.erethon.questsxl.QuestsXL.QUESTS);
    }

    public List<Map<String, Object>> events() {
        return qxlAssets(de.erethon.questsxl.QuestsXL.EVENTS);
    }

    private List<Map<String, Object>> qxlAssets(java.io.File folder) {
        return folder == null ? List.of() : qxlAssets(folder.toPath());
    }

    private List<Map<String, Object>> qxlAssets(Path root) {
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml") || path.getFileName().toString().endsWith(".yaml"))
                    .map(path -> {
                        String file = path.getFileName().toString();
                        String id = file.replaceFirst("\\.ya?ml$", "");
                        String label = root.relativize(path).toString().replace('\\', '/');
                        return Map.<String, Object>of("id", id, "label", label);
                    })
                    .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), this::sortedUniqueById));
        } catch (IOException e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> hecateClasses() {
        return fileAssets("Hecate", "classes");
    }

    public List<Map<String, Object>> hecateTraitlines() {
        return fileAssets("Hecate", "traitlines");
    }

    public List<Map<String, Object>> spellbookSpells() {
        return spellbookAssets("spells");
    }

    public List<Map<String, Object>> spellbookTraits() {
        return spellbookAssets("traits");
    }

    public List<Map<String, Object>> spellbookEffects() {
        return spellbookAssets("effects");
    }

    private List<Map<String, Object>> spellbookAssets(String folder) {
        return fileAssets("Spellbook", folder);
    }

    private List<Map<String, Object>> fileAssets(String pluginName, String folder) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        Path root = (plugin == null ? new java.io.File("plugins", pluginName) : plugin.getDataFolder()).toPath().resolve(folder);
        if (!Files.exists(root)) {
            return List.of();
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".yml") || path.getFileName().toString().endsWith(".yaml"))
                    .map(path -> fileAsset(root, path))
                    .collect(java.util.stream.Collectors.collectingAndThen(java.util.stream.Collectors.toList(), this::sortedUniqueById));
        } catch (IOException e) {
            return List.of();
        }
    }

    private Map<String, Object> fileAsset(Path root, Path path) {
        String file = path.getFileName().toString();
        String id = file.replaceFirst("\\.ya?ml$", "");
        String label = root.relativize(path).toString().replace('\\', '/');
        return Map.of("id", id, "label", label);
    }

    public List<Map<String, Object>> worlds() {
        try {
            return Bukkit.getWorlds().stream()
                    .map(World::getName)
                    .sorted()
                    .map(name -> Map.<String, Object>of("id", name, "label", name))
                    .toList();
        } catch (Throwable e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> materials() {
        try {
            return Arrays.stream(Material.values())
                    .map(this::materialName)
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .sorted()
                    .map(name -> Map.<String, Object>of("id", name, "label", name))
                    .toList();
        } catch (Throwable e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> entityTypes() {
        try {
            return Arrays.stream(EntityType.values())
                    .map(this::entityTypeName)
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .sorted()
                    .map(name -> Map.<String, Object>of("id", name, "label", name))
                    .toList();
        } catch (Throwable e) {
            return List.of();
        }
    }

    private String entityTypeName(EntityType type) {
        try {
            return type.getKey().toString();
        } catch (Throwable e) {
            return "";
        }
    }

    public List<Map<String, Object>> attributes() {
        try {
            return Arrays.stream(Attribute.values())
                    .map(this::attributeName)
                    .filter(name -> !name.isBlank())
                    .distinct()
                    .sorted()
                    .map(name -> Map.<String, Object>of("id", name, "label", name))
                    .toList();
        } catch (Throwable e) {
            return List.of();
        }
    }

    public List<Map<String, Object>> factionsBuildingEffects() {
        return List.of(
                effect("AddHappiness"), effect("AddHousing"), effect("AdditionalEntityDrops"), effect("AddMemberPermission"),
                effect("AddPolicy"), effect("BlockDependentResourceProduction"), effect("ChangeAttribute"), effect("DecreaseHunger"),
                effect("EnemyRadar"), effect("EntityDependentResourceProduction"), effect("IncreaseResourceStorage"), effect("ItemConversion"),
                effect("ItemProduction"), effect("MoneyConsumption"), effect("PopulationTax"), effect("RegeneratingMine"),
                effect("Regeneration"), effect("ResourceConsumption"), effect("ResourceProduction"), effect("SetFHome"),
                effect("SpawnHorses"), effect("SpawnNPC"), effect("SpeedBoost"), effect("WateringEffect")
        );
    }

    public List<Map<String, Object>> factionsResources() {
        return Arrays.stream(Resource.values())
                .map(Resource::getId)
                .sorted()
                .map(id -> Map.<String, Object>of("id", id, "label", id))
                .toList();
    }

    public List<Map<String, Object>> factionsPopulationLevels() {
        return Arrays.stream(PopulationLevel.values())
                .map(level -> level.name().toLowerCase(java.util.Locale.ROOT))
                .sorted()
                .map(id -> Map.<String, Object>of("id", id, "label", id))
                .toList();
    }

    public List<Map<String, Object>> factionsRegionTypes() {
        return Arrays.stream(RegionType.values())
                .map(type -> type.name().toLowerCase(java.util.Locale.ROOT))
                .sorted()
                .map(id -> Map.<String, Object>of("id", id, "label", id))
                .toList();
    }

    public List<Map<String, Object>> factionsBuildingTags() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Factions");
        Path root = (plugin == null ? new java.io.File("plugins", "Factions") : plugin.getDataFolder()).toPath();
        Path file = root.resolve("buildingTags.yml");
        if (!Files.exists(file)) {
            return List.of();
        }
        YamlConfiguration config = YamlConfiguration.loadConfiguration(file.toFile());
        var tags = config.getConfigurationSection("tags");
        if (tags == null) {
            return List.of();
        }
        return tags.getKeys(false).stream()
                .map(id -> {
                    String normalized = id.toUpperCase(java.util.Locale.ROOT);
                    Map<String, Object> dto = new LinkedHashMap<>();
                    dto.put("id", normalized);
                    dto.put("label", normalized);
                    dto.put("category", "tag");
                    dto.put("minecraftTags", tags.getStringList(id + ".minecraftTags"));
                    dto.put("materials", tags.getStringList(id + ".materials"));
                    dto.put("references", tags.getStringList(id + ".references"));
                    return dto;
                })
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("id"))))
                .toList();
    }

    public List<Map<String, Object>> factionsMinecraftTags() {
        Set<String> result = new HashSet<>();
        collectMaterialTagFields(MaterialSetTag.class, MaterialSetTag.class, result);
        collectMaterialTagFields(MaterialTags.class, Tag.class, result);
        collectMaterialTagFields(Tag.class, Tag.class, result);
        return result.stream()
                .sorted()
                .map(id -> Map.<String, Object>of("id", id, "label", id))
                .toList();
    }

    private void collectMaterialTagFields(Class<?> owner, Class<?> expectedType, Set<String> result) {
        for (Field field : owner.getDeclaredFields()) {
            try {
                Object value = field.get(null);
                if (expectedType.isInstance(value)) {
                    result.add(field.getName().toUpperCase(java.util.Locale.ROOT));
                }
            } catch (Throwable ignored) {
            }
        }
    }

    public List<Map<String, Object>> factionsAttributes() {
        Set<String> attributes = new HashSet<>(Set.of(
                "production_rate",
                "tax_rate",
                "happiness",
                "unrest",
                "storage",
                "population",
                "housing_beggar",
                "housing_peasant",
                "housing_citizen",
                "housing_patrician",
                "housing_noblemen"
        ));
        for (Resource resource : Resource.values()) {
            attributes.add(resource.getId() + "_production");
            attributes.add(resource.getId() + "_storage");
            attributes.add(resource.getId() + "_consumption");
        }
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Factions");
        Path buildings = (plugin == null ? new java.io.File("plugins", "Factions") : plugin.getDataFolder()).toPath().resolve("buildings");
        if (Files.exists(buildings)) {
            try (var stream = Files.walk(buildings)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".yml") || path.getFileName().toString().endsWith(".yaml"))
                        .forEach(path -> collectFactionAttributes(path, attributes));
            } catch (IOException ignored) {
            }
        }
        return attributes.stream()
                .filter(id -> !id.isBlank())
                .sorted()
                .map(id -> Map.<String, Object>of("id", id, "label", id))
                .toList();
    }

    private void collectFactionAttributes(Path path, Set<String> attributes) {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(path.toFile());
        var effects = config.getConfigurationSection("effects");
        if (effects == null) {
            return;
        }
        for (String key : effects.getKeys(false)) {
            String attribute = effects.getString(key + ".attribute");
            if (attribute != null && !attribute.isBlank()) {
                attributes.add(attribute);
            }
        }
    }

    private Map<String, Object> effect(String id) {
        return Map.of("id", id, "label", id);
    }

    private String materialName(Material material) {
        try {
            return material.getKey().toString();
        } catch (Throwable e) {
            return "";
        }
    }

    private String attributeName(Attribute attribute) {
        try {
            Method getter = attribute.getClass().getMethod("getKey");
            Object value = getter.invoke(attribute);
            if (value != null) {
                return value.toString().toLowerCase(java.util.Locale.ROOT);
            }
        } catch (Throwable ignored) {
        }
        return attribute.toString().toLowerCase(java.util.Locale.ROOT);
    }

    private List<Map<String, Object>> warnings() {
        List<Map<String, Object>> result = new ArrayList<>();
        if (Bukkit.getPluginManager().getPlugin("Aether") == null) {
            result.add(Map.of("source", "aether", "message", "Aether is not enabled; mob pickers are manual text fields."));
        }
        if (Bukkit.getPluginManager().getPlugin("Hephaestus") == null) {
            result.add(Map.of("source", "hephaestus", "message", "Hephaestus is not enabled; item pickers are manual text fields."));
        }
        if (Bukkit.getPluginManager().getPlugin("Hecate") == null) {
            result.add(Map.of("source", "hecate", "message", "Hecate is not enabled; class and traitline pickers use files from plugins/Hecate if available."));
        }
        if (!Files.exists(new java.io.File("plugins", "Spellbook").toPath())) {
            result.add(Map.of("source", "spellbook", "message", "plugins/Spellbook was not found; Spellbook pickers are manual text fields."));
        }
        return result;
    }

    private List<Map<String, Object>> sortedUniqueById(List<? extends Map<String, Object>> rows) {
        Map<String, Map<String, Object>> byId = new LinkedHashMap<>();
        for (Map<String, Object> row : rows) {
            String id = String.valueOf(row.getOrDefault("id", ""));
            if (id.isBlank()) {
                continue;
            }
            byId.putIfAbsent(id, row);
        }
        return byId.values().stream()
                .sorted(Comparator.comparing(row -> String.valueOf(row.get("id"))))
                .toList();
    }

    private static String identifierString(Identifier identifier) {
        return identifier == null ? "" : identifier.toString();
    }

    private List<Map<String, Object>> safeAssets(String source, Supplier<List<Map<String, Object>>> supplier, List<Map<String, Object>> catalogWarnings) {
        try {
            return supplier.get();
        } catch (Throwable e) {
            catalogWarnings.add(Map.of("source", source, "message", e.getMessage() == null ? e.toString() : e.getMessage()));
            return List.of();
        }
    }
}
