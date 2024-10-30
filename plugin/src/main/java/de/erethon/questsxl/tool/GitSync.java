package de.erethon.questsxl.tool;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.FileUtil;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.Bukkit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.merge.ContentMergeStrategy;
import org.eclipse.jgit.merge.MergeStrategy;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.sql.Date;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GitSync {

    File cache = new File(Bukkit.getWorldContainer() + "/git_cache/");
    Set<String> foldersToSync = new HashSet<>();
    Set<File> foldersInCache = new HashSet<>();
    Set<File> foldersInPlugins = new HashSet<>();
    Repository repository;
    Git git;
    QuestsXL plugin = QuestsXL.getInstance();

    private boolean isSyncOnly;

    private static final List<String> EXCLUSIONS = List.of("players", "playerdata", "inventories", "backups", "factions", "users", "ips", "debug.txt");

    public GitSync(List<String> names, boolean syncOnly) throws IOException, GitAPIException, InterruptedException {
        foldersToSync = new HashSet<>(names);
        for (String name : foldersToSync) {
            File file = new File(Bukkit.getPluginsFolder() + "/" + name);
            if (!file.exists()) {
                MessageUtil.log("Could not find folder " + name + " in plugins folder. Skipping.");
                continue;
            }
            foldersInPlugins.add(new File(Bukkit.getPluginsFolder() + "/" + name));
            foldersInCache.add(new File(cache + "/" + name));
        }
        if (cache.exists()) {
            MessageUtil.log("Found existing repo at " + cache);
            git = Git.open(cache);
            repository = git.getRepository();
            return;
        }
        cache.mkdir();
        this.isSyncOnly = syncOnly;
        setupRepo();
    }

    public void setupRepo() throws GitAPIException {
        if (!plugin.isGitSync()) {
            return;
        }
        String token = QuestsXL.getInstance().getGitToken();
        CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider(token, "");
        git = Git.cloneRepository()
                .setURI("https://github.com/DRE2N/Erethon")
                .setCredentialsProvider(credentialsProvider)
                .setBranch(plugin.getGitBranch())
                .setDirectory(cache)
                .call();
        repository = git.getRepository();
    }

    public void update() throws IOException, InterruptedException, GitAPIException {
        if (!plugin.isGitSync()) {
            return;
        }
        if (!isSyncOnly) {
            copyFromPlugins();
            commit();
            push();
        }
        PullResult pullResult = git.pull()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(plugin.getGitToken(), ""))
                .setRemoteBranchName(plugin.getGitBranch())
                .setContentMergeStrategy(ContentMergeStrategy.THEIRS)
                .setStrategy(MergeStrategy.RESOLVE)
                .call();
        MergeResult mergeResult = pullResult.getMergeResult();
        MessageUtil.log("Git sync merge result: " + mergeResult.getMergeStatus());
        copyToPlugins();
    }

    public void commit() throws GitAPIException {
        if (!plugin.isGitSync()) {
            return;
        }
        git.add().addFilepattern(".").call();
        git.add().addFilepattern(".").setUpdate(true).call();
        if (git.status().call().isClean()) {
            MessageUtil.log("No changes to commit.");
            return;
        }
        git.commit()
                .setMessage("Automated server commit - " + Date.from(Instant.now()))
                .call();
    }

    public void push() throws GitAPIException {
        git.push().setCredentialsProvider(new UsernamePasswordCredentialsProvider(plugin.getGitToken(), "")).call();
    }

    public void copyToPlugins() throws IOException, InterruptedException {
        File pluginsFolder = Bukkit.getPluginsFolder();
        for (File file : foldersInCache) {
            if (!file.exists()) {
                MessageUtil.log("Could not find folder " + file.getName() + " in cache. Skipping.");
                continue;
            }
            FileUtil.copyDir(file, new File(pluginsFolder + "/" + file.getName() + "/"));
        }
    }

    public void copyFromPlugins() {
        try {
            for (File file : foldersInPlugins) {
                if (!file.exists()) {
                    MessageUtil.log("Could not find folder " + file.getName() + " in plugins folder. Skipping.");
                    continue;
                }
                FileUtil.copyDir(file, new File(cache + "/" + file.getName() + "/"), EXCLUSIONS.toArray(new String[0]));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private boolean delete(File file) {

        File[] flist = null;

        if(file == null){
            return false;
        }

        if (file.isFile()) {
            return file.delete();
        }

        if (!file.isDirectory()) {
            return false;
        }

        flist = file.listFiles();
        if (flist != null && flist.length > 0) {
            for (File f : flist) {
                if (!delete(f)) {
                    return false;
                }
            }
        }

        return file.delete();
    }

}
