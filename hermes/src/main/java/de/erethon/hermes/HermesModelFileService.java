package de.erethon.hermes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.magmaguy.freeminecraftmodels.Daedalus;
import com.magmaguy.freeminecraftmodels.config.ModelsFolder;
import com.magmaguy.freeminecraftmodels.dataconverter.FileModelConverter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HermesModelFileService {

    private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Hermes plugin;
    private final Path backupRoot;
    private final long maxUploadBytes;

    public HermesModelFileService(Hermes plugin, long maxUploadBytes) {
        this.plugin = plugin;
        this.maxUploadBytes = maxUploadBytes;
        this.backupRoot = plugin.getDataFolder().toPath().resolve("web-backups").resolve("daedalusModels");
    }

    public List<Map<String, Object>> listModels() throws IOException {
        Path root = root();
        if (!Files.exists(root)) {
            Files.createDirectories(root);
        }
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".bbmodel"))
                    .sorted(Comparator.comparing(path -> normalize(root.relativize(path))))
                    .map(path -> modelDto(root, path))
                    .toList();
        }
    }

    public byte[] read(String itemPath) throws IOException {
        return Files.readAllBytes(resolveModelPath(itemPath, false));
    }

    public Map<String, Object> validate(String itemPath, byte[] bytes) {
        try {
            validateModelPath(itemPath);
            return validateModelBytes(bytes, itemPath);
        } catch (Throwable e) {
            return Map.of("valid", false, "errors", List.of(error("Invalid model", e.getMessage(), itemPath)));
        }
    }

    public CompletableFuture<Map<String, Object>> upload(String itemPath, byte[] bytes, boolean overwrite) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                if (bytes.length > maxUploadBytes) {
                    throw new IllegalArgumentException("Model is larger than the configured upload limit of " + maxUploadBytes + " bytes.");
                }
                Map<String, Object> validation = validate(itemPath, bytes);
                if (!Boolean.TRUE.equals(validation.get("valid"))) {
                    future.complete(validation);
                    return;
                }
                Path target = resolveModelPath(itemPath, true);
                if (Files.exists(target) && !overwrite) {
                    throw new IllegalArgumentException("Target model already exists. Enable overwrite to replace it.");
                }
                backup(target, itemPath);
                Files.createDirectories(target.getParent());
                Files.write(target, bytes);
                List<Map<String, Object>> warnings = reloadDaedalus();
                List<Map<String, Object>> errors = new ArrayList<>(warnings);
                errors.addAll(plugin.getGitExportService().markDirty(HermesContentType.DAEDALUS_MODELS, displayPath(itemPath), target, false));
                future.complete(Map.of("success", warnings.isEmpty(), "valid", true, "errors", errors, "path", displayPath(itemPath)));
            } catch (Throwable e) {
                future.complete(Map.of("success", false, "valid", false, "errors", List.of(exceptionError("Upload failed", e, displayPath(itemPath)))));
            }
        });
        return future;
    }

    public CompletableFuture<Map<String, Object>> move(String itemPath, String nextPath) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Path source = resolveModelPath(itemPath, false);
                Path target = resolveModelPath(nextPath, true);
                if (Files.exists(target)) {
                    throw new IllegalArgumentException("Target model already exists: " + displayPath(nextPath));
                }
                backup(source, itemPath);
                Files.createDirectories(target.getParent());
                Files.move(source, target);
                List<Map<String, Object>> warnings = reloadDaedalus();
                List<Map<String, Object>> errors = new ArrayList<>(warnings);
                errors.addAll(plugin.getGitExportService().markDirty(HermesContentType.DAEDALUS_MODELS, displayPath(itemPath), source, true));
                errors.addAll(plugin.getGitExportService().markDirty(HermesContentType.DAEDALUS_MODELS, displayPath(nextPath), target, false));
                future.complete(Map.of("success", warnings.isEmpty(), "errors", errors, "path", displayPath(nextPath), "oldPath", displayPath(itemPath)));
            } catch (Throwable e) {
                future.complete(Map.of("success", false, "errors", List.of(exceptionError("Move failed", e, displayPath(itemPath)))));
            }
        });
        return future;
    }

    public CompletableFuture<Map<String, Object>> delete(String itemPath) {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                Path target = resolveModelPath(itemPath, false);
                backup(target, itemPath);
                Files.delete(target);
                List<Map<String, Object>> warnings = reloadDaedalus();
                List<Map<String, Object>> errors = new ArrayList<>(warnings);
                errors.addAll(plugin.getGitExportService().markDirty(HermesContentType.DAEDALUS_MODELS, displayPath(itemPath), target, true));
                future.complete(Map.of("success", warnings.isEmpty(), "errors", errors, "path", displayPath(itemPath)));
            } catch (Throwable e) {
                future.complete(Map.of("success", false, "errors", List.of(exceptionError("Delete failed", e, displayPath(itemPath)))));
            }
        });
        return future;
    }

    public CompletableFuture<Map<String, Object>> reload() {
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        Bukkit.getScheduler().runTask(plugin, () -> {
            List<Map<String, Object>> warnings = reloadDaedalus();
            future.complete(Map.of("success", warnings.isEmpty(), "errors", warnings));
        });
        return future;
    }

    private Map<String, Object> modelDto(Path root, Path path) {
        Map<String, Object> dto = new HashMap<>();
        String relative = normalize(root.relativize(path));
        dto.put("type", "daedalusModels");
        dto.put("path", relative);
        dto.put("id", idFromPath(relative));
        try {
            dto.put("updatedAt", Files.getLastModifiedTime(path).toInstant().toString());
            dto.put("size", Files.size(path));
            dto.putAll(modelMetadata(Files.readString(path, StandardCharsets.UTF_8)));
            FileModelConverter converted = FileModelConverter.getConvertedFileModels().get(idFromPath(relative));
            if (converted != null) {
                dto.put("loadedId", converted.getID());
                dto.put("loadedName", converted.getModelName());
            }
        } catch (Throwable e) {
            dto.put("valid", false);
            dto.put("validationMessage", e.getMessage() == null ? e.toString() : e.getMessage());
        }
        return dto;
    }

    private Map<String, Object> validateModelBytes(byte[] bytes, String itemPath) {
        if (bytes.length > maxUploadBytes) {
            return Map.of("valid", false, "errors", List.of(error("Model too large", "Configured upload limit is " + maxUploadBytes + " bytes.", itemPath)));
        }
        try {
            Map<String, Object> metadata = modelMetadata(new String(bytes, StandardCharsets.UTF_8));
            List<Map<String, Object>> errors = new ArrayList<>();
            for (String key : List.of("textures", "elements", "outliner")) {
                Object count = metadata.get(key + "Count");
                if (!(count instanceof Number)) {
                    errors.add(error("Missing " + key, "Blockbench .bbmodel files must contain a " + key + " array.", itemPath));
                }
            }
            return errors.isEmpty()
                    ? Map.of("valid", true, "errors", List.of(), "metadata", metadata)
                    : Map.of("valid", false, "errors", errors, "metadata", metadata);
        } catch (Throwable e) {
            return Map.of("valid", false, "errors", List.of(error("Invalid .bbmodel JSON", e.getMessage(), itemPath)));
        }
    }

    private Map<String, Object> modelMetadata(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, Object> dto = new HashMap<>();
        dto.put("valid", true);
        putArrayCount(dto, root, "textures");
        putArrayCount(dto, root, "elements");
        putArrayCount(dto, root, "outliner");
        putArrayCount(dto, root, "animations");
        if (root.has("name") && root.get("name").isJsonPrimitive()) {
            dto.put("modelName", root.get("name").getAsString());
        }
        if (root.has("meta") && root.get("meta").isJsonObject()) {
            JsonObject meta = root.getAsJsonObject("meta");
            if (meta.has("model_identifier") && meta.get("model_identifier").isJsonPrimitive()) {
                dto.put("modelId", meta.get("model_identifier").getAsString());
            }
            if (meta.has("format_version") && meta.get("format_version").isJsonPrimitive()) {
                dto.put("formatVersion", meta.get("format_version").getAsString());
            }
        }
        return dto;
    }

    private static void putArrayCount(Map<String, Object> dto, JsonObject root, String key) {
        JsonElement element = root.get(key);
        if (element instanceof JsonArray array) {
            dto.put(key + "Count", array.size());
        }
    }

    private List<Map<String, Object>> reloadDaedalus() {
        Plugin target = Bukkit.getPluginManager().getPlugin("Daedalus");
        if (!(target instanceof Daedalus) || !target.isEnabled()) {
            return List.of(error("Reload skipped", "Daedalus is not enabled on this server.", "Daedalus"));
        }
        try {
            ModelsFolder.initializeConfig();
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Reload failed", e, "Daedalus"));
        }
    }

    private void backup(Path live, String itemPath) throws IOException {
        if (!Files.exists(live)) {
            return;
        }
        String safeName = displayPath(itemPath).replace('\\', '_').replace('/', '_').replace(':', '_');
        Path backup = backupRoot.resolve(LocalDateTime.now().format(BACKUP_FORMAT) + "-" + safeName);
        Files.createDirectories(backup.getParent());
        Files.copy(live, backup, StandardCopyOption.REPLACE_EXISTING);
    }

    private Path resolveModelPath(String itemPath, boolean allowCreate) throws IOException {
        Path root = root();
        String normalized = validateModelPath(itemPath);
        Path target = root.resolve(normalized).normalize();
        if (!target.startsWith(root.normalize())) {
            throw new IllegalArgumentException("Invalid model path");
        }
        if (!allowCreate && !Files.exists(target)) {
            throw new IllegalArgumentException("Model file does not exist: " + normalized);
        }
        return target;
    }

    private String validateModelPath(String itemPath) {
        String normalized = displayPath(itemPath);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Missing model path");
        }
        if (!normalized.toLowerCase().endsWith(".bbmodel")) {
            throw new IllegalArgumentException("Daedalus model paths must end with .bbmodel");
        }
        return normalized;
    }

    private Path root() {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Daedalus");
        Path dataFolder = plugin == null ? Path.of("plugins", "Daedalus") : plugin.getDataFolder().toPath();
        return dataFolder.resolve("models");
    }

    private static String displayPath(String itemPath) {
        return itemPath == null ? "" : itemPath.replace('\\', '/').trim();
    }

    private static String normalize(Path path) {
        return path.toString().replace('\\', '/');
    }

    private static String idFromPath(String path) {
        String file = path.replace('\\', '/');
        int slash = file.lastIndexOf('/');
        if (slash >= 0) {
            file = file.substring(slash + 1);
        }
        return file.replaceFirst("(?i)\\.bbmodel$", "");
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
}
