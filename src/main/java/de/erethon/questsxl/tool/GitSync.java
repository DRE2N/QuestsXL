package de.erethon.questsxl.tool;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.FileUtil;
import de.erethon.questsxl.QuestsXL;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import oshi.util.FileSystemUtil;

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

    public GitSync(List<String> names) throws IOException, GitAPIException, InterruptedException {
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
        copyFromPlugins();
        commit();
        push();
        PullResult pullResult = git.pull()
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider(plugin.getGitToken(), ""))
                .setRemoteBranchName(plugin.getGitBranch())
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
            FileUtil.copyDir(file, new File(pluginsFolder + "/" + file.getName() + "/"));
        }
    }

    public void copyFromPlugins() throws IOException {
        File pluginsFolder = Bukkit.getPluginsFolder();
        for (File file : foldersInPlugins) {
            FileUtil.copyDir(file, new File(pluginsFolder + "/" + file.getName() + "/"));
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
