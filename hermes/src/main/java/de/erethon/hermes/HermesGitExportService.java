package de.erethon.hermes;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class HermesGitExportService {

    private final Hermes plugin;
    private final Path queueFile;

    public HermesGitExportService(Hermes plugin) {
        this.plugin = plugin;
        this.queueFile = plugin.getDataFolder().toPath().resolve("git-dirty.yml");
    }

    public synchronized List<Map<String, Object>> markDirty(HermesContentType type, String itemPath, Path livePath, boolean deleted) {
        return markDirtyPath(type.gitPath(itemPath), livePath, deleted);
    }

    public synchronized List<Map<String, Object>> markDirtyPath(String gitPath, Path livePath, boolean deleted) {
        if (!enabled()) {
            return List.of();
        }
        try {
            DirtyQueue queue = readQueue();
            queue.entries.put(gitPath, new DirtyEntry(gitPath, livePath == null ? "" : livePath.toAbsolutePath().toString(), deleted, Instant.now().toString()));
            writeQueue(queue);
            return List.of();
        } catch (Throwable e) {
            return List.of(exceptionError("Git queue failed", e, gitPath));
        }
    }

    public synchronized Map<String, Object> status() {
        DirtyQueue queue = readQueueSafely();
        List<Map<String, Object>> errors = new ArrayList<>();
        boolean configured = configured();
        if (enabled() && !configured) {
            errors.add(error("Git not configured", "Set repoUrl, branch, and token before pushing.", "Hermes git"));
        }
        return statusDto(queue, errors);
    }

    public synchronized Map<String, Object> updateConfig(Map<String, String> values) {
        ConfigurationSection git = config();
        setIfPresent(git, values, "enabled");
        setIfPresent(git, values, "repoUrl");
        setIfPresent(git, values, "branch");
        setIfPresent(git, values, "token");
        setIfPresent(git, values, "authorName");
        setIfPresent(git, values, "authorEmail");
        setIfPresent(git, values, "clonePath");
        plugin.saveConfig();
        plugin.reloadConfig();
        return status();
    }

    public CompletableFuture<Map<String, Object>> refresh() {
        return CompletableFuture.supplyAsync(this::status);
    }

    public synchronized Map<String, Object> clearQueue() {
        DirtyQueue queue = readQueueSafely();
        queue.entries.clear();
        queue.lastError = "";
        writeQueue(queue);
        return statusDto(queue, List.of());
    }

    public CompletableFuture<Map<String, Object>> push(String commitMessage, String authorName) {
        return CompletableFuture.supplyAsync(() -> {
            synchronized (this) {
                DirtyQueue queue = readQueueSafely();
                if (!enabled()) {
                    return statusDto(queue, List.of(error("Git disabled", "Enable Hermes git export before pushing.", "Hermes git")));
                }
                if (!configured()) {
                    return statusDto(queue, List.of(error("Git not configured", "Set repoUrl, branch, and token before pushing.", "Hermes git")));
                }
                if (queue.entries.isEmpty()) {
                    queue.lastError = "";
                    writeQueueUnchecked(queue);
                    return statusDto(queue, List.of());
                }
                try (Git git = ensureClone()) {
                    resetToRemote(git);
                    for (DirtyEntry entry : queue.entries.values()) {
                        Path target = clonePath().resolve(entry.gitPath()).normalize();
                        if (!target.startsWith(clonePath().normalize())) {
                            throw new IllegalArgumentException("Invalid git path: " + entry.gitPath());
                        }
                        if (entry.deleted()) {
                            Files.deleteIfExists(target);
                            continue;
                        }
                        Path source = Path.of(entry.livePath());
                        if (!Files.exists(source)) {
                            Files.deleteIfExists(target);
                            continue;
                        }
                        Files.createDirectories(target.getParent());
                        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
                    }
                    git.add().addFilepattern(".").call();
                    git.add().addFilepattern(".").setUpdate(true).call();
                    Status status = git.status().call();
                    if (status.isClean()) {
                        queue.entries.clear();
                        queue.lastError = "";
                        writeQueue(queue);
                        return statusDto(queue, List.of());
                    }
                    Instant now = Instant.now();
                    String message = commitMessage(commitMessage, now);
                    String resolvedAuthor = authorName == null || authorName.isBlank()
                            ? configString("authorName", "Hermes")
                            : authorName.trim() + " and Hermes";
                    ObjectId commit = git.commit()
                            .setMessage(message)
                            .setAuthor(resolvedAuthor, configString("authorEmail", "hermes@erethon.de"))
                            .call()
                            .getId();
                    git.push()
                            .setCredentialsProvider(credentials())
                            .call();
                    queue.entries.clear();
                    queue.lastCommit = commit.name();
                    queue.lastPushAt = now.toString();
                    queue.lastError = "";
                    writeQueue(queue);
                    return statusDto(queue, List.of());
                } catch (Throwable e) {
                    queue.lastError = e.getMessage() == null ? e.toString() : e.getMessage();
                    writeQueueUnchecked(queue);
                    return statusDto(queue, List.of(exceptionError("Git push failed", e, "Hermes git")));
                }
            }
        });
    }

    private String commitMessage(String requested, Instant timestamp) {
        String base = requested == null ? "" : requested.trim();
        if (base.isBlank()) {
            base = "Content changes";
        }
        return base + " - Hermes Content Update " + timestamp;
    }

    private Git ensureClone() throws Exception {
        Path clone = clonePath();
        Files.createDirectories(clone.getParent());
        if (Files.exists(clone.resolve(".git"))) {
            return Git.open(clone.toFile());
        }
        if (Files.exists(clone)) {
            try (var stream = Files.list(clone)) {
                if (stream.findAny().isPresent()) {
                    throw new IllegalStateException("Git clone path exists and is not an empty git repository: " + clone);
                }
            }
        }
        return Git.cloneRepository()
                .setURI(configString("repoUrl", ""))
                .setBranch(configString("branch", "dev"))
                .setDirectory(clone.toFile())
                .setCredentialsProvider(credentials())
                .call();
    }

    private void resetToRemote(Git git) throws Exception {
        String branch = configString("branch", "dev");
        git.fetch().setCredentialsProvider(credentials()).call();
        if (branchExists(git, branch)) {
            git.checkout().setName(branch).call();
        } else {
            git.checkout().setName(branch).setCreateBranch(true).setStartPoint("origin/" + branch).call();
        }
        git.reset().setMode(ResetCommand.ResetType.HARD).setRef("origin/" + branch).call();
        git.clean().setCleanDirectories(true).call();
    }

    private boolean branchExists(Git git, String branch) throws Exception {
        return git.branchList().call().stream().anyMatch(ref -> ref.getName().endsWith("/" + branch));
    }

    private UsernamePasswordCredentialsProvider credentials() {
        return new UsernamePasswordCredentialsProvider(configString("token", ""), "");
    }

    private Map<String, Object> statusDto(DirtyQueue queue, List<Map<String, Object>> errors) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("enabled", enabled());
        dto.put("configured", configured());
        dto.put("repoUrl", configString("repoUrl", ""));
        dto.put("branch", configString("branch", "dev"));
        dto.put("authorName", configString("authorName", "Hermes"));
        dto.put("authorEmail", configString("authorEmail", "hermes@erethon.de"));
        dto.put("clonePath", configString("clonePath", "git-export"));
        dto.put("dirtyFiles", queue.entries.values().stream().map(entry -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("path", entry.gitPath());
            row.put("livePath", entry.livePath());
            row.put("deleted", entry.deleted());
            row.put("updatedAt", entry.updatedAt());
            return row;
        }).toList());
        dto.put("lastPushAt", queue.lastPushAt);
        dto.put("lastCommit", queue.lastCommit);
        dto.put("lastError", queue.lastError);
        dto.put("errors", errors);
        dto.put("warnings", List.of());
        return dto;
    }

    private DirtyQueue readQueueSafely() {
        try {
            return readQueue();
        } catch (Throwable e) {
            DirtyQueue queue = new DirtyQueue();
            queue.lastError = e.getMessage() == null ? e.toString() : e.getMessage();
            return queue;
        }
    }

    private DirtyQueue readQueue() {
        DirtyQueue queue = new DirtyQueue();
        if (!Files.exists(queueFile)) {
            return queue;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(queueFile.toFile());
        queue.lastPushAt = yaml.getString("lastPushAt", "");
        queue.lastCommit = yaml.getString("lastCommit", "");
        queue.lastError = yaml.getString("lastError", "");
        ConfigurationSection entries = yaml.getConfigurationSection("entries");
        if (entries != null) {
            for (String key : entries.getKeys(false)) {
                String path = entries.getString(key + ".path", key);
                queue.entries.put(path, new DirtyEntry(
                        path,
                        entries.getString(key + ".livePath", ""),
                        entries.getBoolean(key + ".deleted", false),
                        entries.getString(key + ".updatedAt", "")
                ));
            }
        }
        return queue;
    }

    private void writeQueueUnchecked(DirtyQueue queue) {
        try {
            writeQueue(queue);
        } catch (Throwable e) {
            plugin.getLogger().warning("Failed to write Hermes git queue: " + e.getMessage());
        }
    }

    private void writeQueue(DirtyQueue queue) {
        try {
            Files.createDirectories(queueFile.getParent());
            YamlConfiguration yaml = new YamlConfiguration();
            yaml.set("lastPushAt", queue.lastPushAt);
            yaml.set("lastCommit", queue.lastCommit);
            yaml.set("lastError", queue.lastError);
            int index = 0;
            for (DirtyEntry entry : queue.entries.values()) {
                String key = "entries." + index++;
                yaml.set(key + ".path", entry.gitPath());
                yaml.set(key + ".livePath", entry.livePath());
                yaml.set(key + ".deleted", entry.deleted());
                yaml.set(key + ".updatedAt", entry.updatedAt());
            }
            yaml.save(queueFile.toFile());
        } catch (Exception e) {
            throw new IllegalStateException("Could not write git dirty queue", e);
        }
    }

    private boolean enabled() {
        return config().getBoolean("enabled", false);
    }

    private boolean configured() {
        return enabled()
                && !configString("repoUrl", "").isBlank()
                && !configString("branch", "").isBlank()
                && !configString("token", "").isBlank();
    }

    private Path clonePath() {
        String configured = configString("clonePath", "git-export");
        Path path = Path.of(configured);
        return path.isAbsolute() ? path : plugin.getDataFolder().toPath().resolve(path);
    }

    private ConfigurationSection config() {
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("git");
        return section == null ? plugin.getConfig().createSection("git") : section;
    }

    private String configString(String key, String fallback) {
        String value = config().getString(key, fallback);
        return value == null ? fallback : value;
    }

    private void setIfPresent(ConfigurationSection section, Map<String, String> values, String key) {
        if (!values.containsKey(key)) {
            return;
        }
        if (key.equals("enabled")) {
            section.set(key, Boolean.parseBoolean(values.get(key)));
        } else {
            section.set(key, values.get(key));
        }
    }

    private static Map<String, Object> error(String title, String message, String location) {
        return Map.of("title", title, "message", message == null ? "" : message, "location", location == null ? "" : location);
    }

    private static Map<String, Object> exceptionError(String title, Throwable throwable, String location) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("title", title);
        dto.put("message", throwable.getMessage() == null ? throwable.toString() : throwable.getMessage());
        dto.put("location", location == null ? "" : location);
        dto.put("hint", "");
        dto.put("stacktrace", stacktrace(throwable, 32));
        dto.put("stackPreview", stacktrace(throwable, 3));
        return dto;
    }

    private static String stacktrace(Throwable throwable, int maxLines) {
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        String[] lines = writer.toString().split("\\R");
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < Math.min(lines.length, maxLines); i++) {
            if (!builder.isEmpty()) {
                builder.append('\n');
            }
            builder.append(lines[i]);
        }
        return builder.toString();
    }

    private static class DirtyQueue {
        private final Map<String, DirtyEntry> entries = new LinkedHashMap<>();
        private String lastPushAt = "";
        private String lastCommit = "";
        private String lastError = "";
    }

    private record DirtyEntry(String gitPath, String livePath, boolean deleted, String updatedAt) {
    }
}
