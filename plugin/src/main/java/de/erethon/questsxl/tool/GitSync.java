package de.erethon.questsxl.tool;

import de.erethon.bedrock.misc.FileUtil;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.Bukkit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GitSync {

    private final File pluginsFolder = Bukkit.getPluginsFolder();
    private final File repoFolder;
    private final Set<String> foldersToSync = new HashSet<>();
    private Repository repository;
    private Git git;
    private final QuestsXL plugin = QuestsXL.get();

    // Files that can be modified by the server and need special handling.
    private static final List<String> SERVER_MODIFIABLE_PATTERNS = List.of(
        // Hephaestus
        "**/items/**/*.yml",
        // Factions
        "**/alliances/*.yml",
        "**/factions/*.yml",
        "**/regions/**/*.yml",
        "**/war/**",
        "**/sites/**",
        // Aether
        "**/spawners/*.yml",
        "**/mob-spawns.yml",
        // Hecate
        "**/lobbies/*.yml",
        // QXL
        "**/regions.yml",
        "**/respawnPoints.yml"
    );

    // Files to exclude from sync
    private static final List<String> EXCLUSIONS = List.of(
        "players", "playerdata", "inventories", "backups", "output", "docs", "users", "ips", "debug.txt"
    );

    public GitSync(List<String> folderNames) throws IOException, GitAPIException {
        this.foldersToSync.addAll(folderNames);
        this.repoFolder = new File(pluginsFolder.getParent(), "git_repo");

        validateFolders();
        initializeRepository();
    }

    private void validateFolders() {
        Set<String> validFolders = new HashSet<>();
        for (String name : foldersToSync) {
            File folder = new File(pluginsFolder, name);
            if (folder.exists() && folder.isDirectory()) {
                validFolders.add(name);
            } else {
                QuestsXL.log("Warning: Folder '" + name + "' not found in plugins directory. Skipping.");
            }
        }
        foldersToSync.clear();
        foldersToSync.addAll(validFolders);
    }

    private void initializeRepository() throws IOException, GitAPIException {
        if (repoFolder.exists()) {
            QuestsXL.log("Using existing repository at " + repoFolder.getAbsolutePath());
            git = Git.open(repoFolder);
            repository = git.getRepository();
        } else {
            setupNewRepository();
        }
    }

    private void setupNewRepository() throws GitAPIException, IOException {
        if (!plugin.isGitSync()) {
            throw new IllegalStateException("Git sync is not enabled in configuration");
        }

        String token = plugin.getGitToken();
        if (token == null || token.isEmpty()) {
            throw new IllegalStateException("Git token is not configured");
        }

        QuestsXL.log("Cloning repository to " + repoFolder.getAbsolutePath());
        git = Git.cloneRepository()
                .setURI("https://github.com/DRE2N/Erethon")
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .setBranch(plugin.getGitBranch())
                .setDirectory(repoFolder)
                .call();
        repository = git.getRepository();

        // Initial sync from plugins to repo
        copyFromPluginsToRepo();
        commitChanges("Initial sync from server");
    }

    /**
     * Main sync operation: pulls from remote, handles conflicts, and updates plugins
     */
    public void sync() throws IOException, GitAPIException {
        if (!plugin.isGitSync()) {
            QuestsXL.log("Git sync is disabled in configuration");
            return;
        }

        QuestsXL.log("Starting Git sync...");

        // First, backup server changes for files that might be modified by server
        backupServerChanges();

        // Pull latest changes from remote
        pullFromRemote();

        // Copy from repo to plugins
        copyFromRepoToPlugins();

        QuestsXL.log("Git sync completed successfully");
    }

    /**
     * Full update operation: pushes server changes first, then syncs from remote
     * This ensures server-modified files are preserved in GitHub before pulling new changes
     */
    public void fullSync() throws IOException, GitAPIException {
        if (!plugin.isGitSync()) {
            QuestsXL.log("Git sync is disabled in configuration");
            return;
        }

        QuestsXL.log("Starting full sync (push + pull)...");

        // First push any server changes to keep GitHub up to date
        pushServerChanges();

        // Then do the regular sync
        sync();

        QuestsXL.log("Full sync completed successfully");
    }

    /**
     * Push server changes to remote (for files that server modifies)
     */
    public void pushServerChanges() throws IOException, GitAPIException {
        pushServerChanges(false);
    }

    /**
     * Push server changes to remote (for files that server modifies)
     * @param force If true, forces push even if it would overwrite remote changes
     */
    public void pushServerChanges(boolean force) throws IOException, GitAPIException {
        if (!plugin.isGitSync()) {
            return;
        }

        QuestsXL.log("Pushing server changes to remote" + (force ? " (FORCE)" : "") + "...");

        // Copy server-modifiable files from plugins to repo
        copyServerModifiableFiles();

        // Check if there are changes to commit
        Status status = git.status().call();
        if (status.isClean()) {
            QuestsXL.log("No server changes to push");
            return;
        }

        // Commit changes
        String commitMessage = "Automated server update - " + getCurrentTimestamp() + (force ? " (FORCE PUSH)" : "");
        commitChanges(commitMessage);

        // Push with or without force
        pushToRemote(force);

        QuestsXL.log("Server changes pushed successfully" + (force ? " (forced)" : ""));
    }

    private void backupServerChanges() throws IOException {
        QuestsXL.log("Backing up server-modified files...");

        File backupDir = new File(repoFolder.getParent(), "server_backup_" + getCurrentTimestamp().replace(":", "-"));
        if (!backupDir.mkdirs() && !backupDir.exists()) {
            QuestsXL.log("Warning: Could not create backup directory: " + backupDir.getAbsolutePath());
        }

        for (String folderName : foldersToSync) {
            File sourceFolder = new File(pluginsFolder, folderName);
            File backupFolder = new File(backupDir, folderName);

            if (sourceFolder.exists()) {
                copyServerModifiableFilesOnly(sourceFolder.toPath(), backupFolder.toPath());
            }
        }
    }

    private void pullFromRemote() throws GitAPIException {
        QuestsXL.log("Pulling latest changes from remote...");

        String token = plugin.getGitToken();
        String branch = plugin.getGitBranch();

        // Fetch latest changes
        git.fetch()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call();

        // Check if there are local changes that could conflict
        Status status = git.status().call();
        if (!status.isClean()) {
            QuestsXL.log("Local changes detected. Resetting to prioritize remote changes...");
            // Reset hard to remote state (GitHub is source of truth)
            git.reset()
                    .setMode(ResetCommand.ResetType.HARD)
                    .setRef("origin/" + branch)
                    .call();
        } else {
            // Clean pull
            PullResult pullResult = git.pull()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                    .setRemoteBranchName(branch)
                    .setStrategy(MergeStrategy.OURS) // Prefer remote changes
                    .call();

            MergeResult mergeResult = pullResult.getMergeResult();
            if (mergeResult != null) {
                QuestsXL.log("Pull result: " + mergeResult.getMergeStatus());

                if (!mergeResult.getMergeStatus().isSuccessful()) {
                    QuestsXL.log("Merge conflicts detected. Resolving by preferring remote...");
                    git.reset()
                            .setMode(ResetCommand.ResetType.HARD)
                            .setRef("origin/" + branch)
                            .call();
                }
            }
        }
    }

    private void copyFromRepoToPlugins() throws IOException {
        QuestsXL.log("Copying files from repository to plugins...");

        for (String folderName : foldersToSync) {
            File sourceFolder = new File(repoFolder, folderName);
            File targetFolder = new File(pluginsFolder, folderName);

            if (sourceFolder.exists()) {
                // Ensure target directory exists
                if (!targetFolder.mkdirs() && !targetFolder.exists()) {
                    QuestsXL.log("Warning: Could not create target directory: " + targetFolder.getAbsolutePath());
                }

                // Copy with exclusions
                FileUtil.copyDir(sourceFolder, targetFolder, EXCLUSIONS.toArray(new String[0]));
                QuestsXL.log("Copied " + folderName + " from repository to plugins");
            }
        }
    }

    private void copyFromPluginsToRepo() throws IOException {
        QuestsXL.log("Copying files from plugins to repository...");

        for (String folderName : foldersToSync) {
            File sourceFolder = new File(pluginsFolder, folderName);
            File targetFolder = new File(repoFolder, folderName);

            if (sourceFolder.exists()) {
                FileUtil.copyDir(sourceFolder, targetFolder, EXCLUSIONS.toArray(new String[0]));
            }
        }
    }

    private void copyServerModifiableFiles() throws IOException {
        for (String folderName : foldersToSync) {
            File sourceFolder = new File(pluginsFolder, folderName);
            File targetFolder = new File(repoFolder, folderName);

            if (sourceFolder.exists()) {
                copyServerModifiableFilesOnly(sourceFolder.toPath(), targetFolder.toPath());
            }
        }
    }

    private void copyServerModifiableFilesOnly(Path source, Path target) throws IOException {
        if (!Files.exists(source)) {
            return;
        }

        try (var pathStream = Files.walk(source)) {
            pathStream
                    .filter(Files::isRegularFile)
                    .filter(this::isServerModifiable)
                    .forEach(sourcePath -> {
                        try {
                            Path relativePath = source.relativize(sourcePath);
                            Path targetPath = target.resolve(relativePath);
                            Files.createDirectories(targetPath.getParent());
                            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            QuestsXL.log("Failed to copy server-modifiable file: " + sourcePath + " - " + e.getMessage());
                        }
                    });
        }
    }

    private boolean isServerModifiable(Path filePath) {
        String pathString = filePath.toString().replace('\\', '/');
        return SERVER_MODIFIABLE_PATTERNS.stream()
                .anyMatch(pattern -> pathMatches(pathString, pattern));
    }

    private boolean pathMatches(String path, String pattern) {
        // Simple pattern matching for now - can be enhanced with proper glob matching
        pattern = pattern.replace("**", ".*").replace("*", "[^/]*");
        return path.matches(".*" + pattern + ".*");
    }

    private void commitChanges(String message) throws GitAPIException {
        git.add().addFilepattern(".").call();
        git.add().addFilepattern(".").setUpdate(true).call();

        Status status = git.status().call();
        if (status.isClean()) {
            QuestsXL.log("No changes to commit");
            return;
        }

        git.commit().setMessage(message).call();
        QuestsXL.log("Changes committed: " + message);
    }

    private void pushToRemote() throws GitAPIException {
        String token = plugin.getGitToken();
        git.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call();
        QuestsXL.log("Changes pushed to remote");
    }

    private void pushToRemote(boolean force) throws GitAPIException {
        String token = plugin.getGitToken();
        git.push()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .setForce(force)
                .call();
        QuestsXL.log("Changes pushed to remote" + (force ? " (forced)" : ""));
    }

    private String getCurrentTimestamp() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * Clean up resources
     */
    public void close() {
        if (git != null) {
            git.close();
        }
    }

}
