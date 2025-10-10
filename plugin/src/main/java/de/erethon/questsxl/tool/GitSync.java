package de.erethon.questsxl.tool;

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
        "**/respawnPoints.yml",
        "**/explorables.yml",
        "**/explorationSets.yml",
        "Aergia/**"
    );

    // Files to exclude from sync
    private static final List<String> EXCLUSIONS = List.of(
        "players", "playerdata", "inventories", "skinCache.yml", "backups", "output", "docs", "users", "ips", "debug.txt"
    );

    private File tempBackupDir;

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

        // Don't immediately sync from plugins to repo - let the remote be the source of truth
        // The sync() method will handle copying files properly
        QuestsXL.log("Repository cloned successfully");
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
        if (plugin.isPassiveSync()) {
            QuestsXL.log("Passive sync is enabled, skipping push of server changes");
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

    /**
     * Backup server-modifiable files to a temporary location before git operations
     */
    private void backupServerModifiableFiles() throws IOException {
        // Create temporary backup directory
        tempBackupDir = new File(repoFolder.getParent(), "temp_server_backup_" + System.currentTimeMillis());
        if (!tempBackupDir.mkdirs()) {
            throw new IOException("Could not create temporary backup directory: " + tempBackupDir.getAbsolutePath());
        }

        QuestsXL.log("Backing up server-modifiable files to: " + tempBackupDir.getAbsolutePath());

        // Backup server-modifiable files from the git repo
        for (String folderName : foldersToSync) {
            File repoSourceFolder = new File(repoFolder, folderName);
            File backupFolder = new File(tempBackupDir, folderName);

            if (repoSourceFolder.exists()) {
                copyServerModifiableFilesOnly(repoSourceFolder.toPath(), backupFolder.toPath());
            }
        }

        QuestsXL.log("Server-modifiable files backed up successfully");
    }

    /**
     * Restore server-modifiable files from temporary backup after git operations
     */
    private void restoreServerModifiableFiles() throws IOException {
        if (tempBackupDir == null || !tempBackupDir.exists()) {
            QuestsXL.log("No backup directory found, skipping restore");
            return;
        }

        QuestsXL.log("Restoring server-modifiable files from backup...");

        // Restore server-modifiable files back to the git repo
        for (String folderName : foldersToSync) {
            File backupFolder = new File(tempBackupDir, folderName);
            File repoTargetFolder = new File(repoFolder, folderName);

            if (backupFolder.exists()) {
                copyServerModifiableFilesOnly(backupFolder.toPath(), repoTargetFolder.toPath());
            }
        }

        // Clean up temporary backup directory
        try {
            deleteDirectory(tempBackupDir);
            QuestsXL.log("Temporary backup cleaned up");
        } catch (IOException e) {
            QuestsXL.log("Warning: Could not delete temporary backup directory: " + e.getMessage());
        }

        tempBackupDir = null;
        QuestsXL.log("Server-modifiable files restored successfully");
    }

    /**
     * Recursively delete a directory and all its contents
     */
    private void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    if (!file.delete()) {
                        throw new IOException("Could not delete file: " + file.getAbsolutePath());
                    }
                }
            }
        }

        if (!directory.delete()) {
            throw new IOException("Could not delete directory: " + directory.getAbsolutePath());
        }
    }

    private void pullFromRemote() throws GitAPIException, IOException {
        QuestsXL.log("Pulling latest changes from remote...");

        String token = plugin.getGitToken();
        String branch = plugin.getGitBranch();

        // Fetch latest changes first
        QuestsXL.log("Fetching from remote...");
        git.fetch()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .call();

        // Check current HEAD and remote HEAD
        String localRef = repository.resolve("HEAD").getName();
        String remoteRef = repository.resolve("origin/" + branch).getName();
        QuestsXL.log("Local HEAD: " + localRef);
        QuestsXL.log("Remote HEAD: " + remoteRef);

        if (localRef.equals(remoteRef)) {
            QuestsXL.log("Repository is already up to date");
            return;
        }

        // Check if there are local changes
        Status status = git.status().call();
        if (!status.isClean()) {
            QuestsXL.log("Local changes detected. Backing up server-modifiable files...");

            // Backup server-modifiable files before any git operations
            backupServerModifiableFiles();

            // Stash all changes temporarily
            try {
                git.stashCreate().setIndexMessage("Auto-stash before sync").call();
                QuestsXL.log("Local changes stashed");
            } catch (Exception e) {
                QuestsXL.log("Could not stash changes: " + e.getMessage());
            }
        }

        // Now do a clean pull to get remote changes
        QuestsXL.log("Performing clean pull from remote...");
        PullResult pullResult = git.pull()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(token, ""))
                .setRemoteBranchName(branch)
                .setStrategy(MergeStrategy.THEIRS) // Prefer remote changes for conflicts
                .call();

        MergeResult mergeResult = pullResult.getMergeResult();
        if (mergeResult != null) {
            QuestsXL.log("Pull result: " + mergeResult.getMergeStatus());

            if (!mergeResult.getMergeStatus().isSuccessful()) {
                QuestsXL.log("Merge conflicts detected. Resolving conflicts...");
                // If there are still conflicts after using THEIRS strategy, force reset
                git.reset()
                        .setMode(ResetCommand.ResetType.HARD)
                        .setRef("origin/" + branch)
                        .call();
                QuestsXL.log("Forced reset to remote state to resolve conflicts");
            }
        }

        // Restore server-modifiable files from backup
        if (!status.isClean()) {
            QuestsXL.log("Restoring server-modifiable files from backup...");
            restoreServerModifiableFiles();
        }

        // Verify the update worked
        String newLocalRef = repository.resolve("HEAD").getName();
        QuestsXL.log("New local HEAD: " + newLocalRef);
        if (newLocalRef.equals(remoteRef)) {
            QuestsXL.log("Successfully updated to remote changes");
        } else {
            QuestsXL.log("Warning: Local HEAD still doesn't match remote HEAD");
        }
    }

    private void copyFromRepoToPlugins() throws IOException {
        QuestsXL.log("Copying files from repository to plugins...");

        for (String folderName : foldersToSync) {
            File sourceFolder = new File(repoFolder, folderName);
            File targetFolder = new File(pluginsFolder, folderName);

            if (sourceFolder.exists()) {
                copyDirWithExclusions(sourceFolder, targetFolder, EXCLUSIONS);
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
                copyDirWithExclusions(sourceFolder, targetFolder, EXCLUSIONS);
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
                    .filter(path -> !shouldExcludePath(path, source)) // Add exclusion check
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

    /**
     * Check if a path should be excluded based on the exclusions list
     */
    private boolean shouldExcludePath(Path filePath, Path basePath) {
        Path relativePath = basePath.relativize(filePath);
        String pathString = relativePath.toString().replace('\\', '/');

        // Check each path component against exclusions
        for (String pathComponent : pathString.split("/")) {
            if (EXCLUSIONS.contains(pathComponent)) {
                return true;
            }
        }

        // Also check if any parent directory matches exclusions
        Path currentPath = relativePath;
        while (currentPath != null) {
            String currentName = currentPath.getFileName() != null ? currentPath.getFileName().toString() : "";
            if (EXCLUSIONS.contains(currentName)) {
                return true;
            }
            currentPath = currentPath.getParent();
        }

        return false;
    }

    private boolean isServerModifiable(Path filePath) {
        String pathString = filePath.toString().replace('\\', '/');
        return SERVER_MODIFIABLE_PATTERNS.stream()
                .anyMatch(pattern -> pathMatches(pathString, pattern));
    }

    private boolean pathMatches(String path, String pattern) {
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
     * Custom directory copy method that properly handles exclusions for subdirectories
     */
    private void copyDirWithExclusions(File source, File target, List<String> exclusions) throws IOException {
        if (!source.exists()) {
            return;
        }

        if (!target.exists() && !target.mkdirs()) {
            QuestsXL.log("Warning: Could not create target directory: " + target.getAbsolutePath());
            return;
        }

        File[] files = source.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            String fileName = file.getName();

            // Check if this file/directory should be excluded
            boolean shouldExclude = exclusions.stream().anyMatch(exclusion ->
                fileName.equals(exclusion) || fileName.contains(exclusion)
            );

            if (shouldExclude) {
                QuestsXL.log("Excluding: " + file.getAbsolutePath());
                continue;
            }

            File targetFile = new File(target, fileName);

            if (file.isDirectory()) {
                // Recursively copy directory
                copyDirWithExclusions(file, targetFile, exclusions);
            } else {
                // Copy file
                try {
                    Files.copy(file.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    QuestsXL.log("Failed to copy file: " + file.getAbsolutePath() + " - " + e.getMessage());
                }
            }
        }
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
