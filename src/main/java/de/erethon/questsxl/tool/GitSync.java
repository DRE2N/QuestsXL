package de.erethon.questsxl.tool;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;

public class GitSync {

    String loc = QuestsXL.getInstance().getDataFolder().getAbsolutePath() + "/git_cache";


    public void setupRepo() throws IOException, InterruptedException {
        String[] commands = new String[] {
                "git",
                "clone",
                "https://github.com/DRE2N/Erethon",
                loc
        };
        BukkitRunnable wait = new BukkitRunnable() {
            @Override
            public void run() {
                MessageUtil.log("Syncing from GitHub...");
                ProcessBuilder builder = new ProcessBuilder(commands);
                try {
                    builder.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        wait.runTaskLaterAsynchronously(QuestsXL.getInstance(), 20);
    }

    public void update() throws IOException, InterruptedException {
        String[] commands = new String[]{
                "git",
                "-C",
                loc,
                "pull",
        };
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.redirectErrorStream(true);
        final Process git = builder.start();
        BukkitRunnable watcher = new BukkitRunnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(git.getInputStream()));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        MessageUtil.log(line);
                    }
                } catch (IOException e) {
                    MessageUtil.log(e.getMessage());
                }
            }
        };
        watcher.runTaskAsynchronously(QuestsXL.getInstance());
        git.waitFor();
        copyQuests();
    }

    private void copyQuests() throws IOException, InterruptedException {
        delete(QuestsXL.QUESTS);
        QuestsXL.QUESTS.mkdir();
        MessageUtil.log("Copying...");
        String[] commands = new String[]{
                "cmd",
                "/c",
                "xcopy",
                "\"" + loc + "/quests" + "\"",
                "\"" + QuestsXL.getInstance().getDataFolder().getAbsolutePath() + "/quests" + "\"",
                "/sy"
        };
        ProcessBuilder builder = new ProcessBuilder(commands);
        builder.redirectErrorStream(true);
        final Process git = builder.start();
        BukkitRunnable watcher = new BukkitRunnable() {
            @Override
            public void run() {
                BufferedReader reader = new BufferedReader(new InputStreamReader(git.getInputStream()));
                String line;
                try {
                    while ((line = reader.readLine()) != null) {
                        MessageUtil.log(line);
                    }
                } catch (IOException e) {
                    MessageUtil.log(e.getMessage());
                }
            }
        };
        watcher.runTaskAsynchronously(QuestsXL.getInstance());
        git.waitFor();
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
