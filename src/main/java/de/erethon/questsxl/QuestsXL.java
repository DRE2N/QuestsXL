package de.erethon.questsxl;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import de.erethon.aergia.Aergia;
import de.erethon.aergia.scoreboard.ScoreboardLines;
import de.erethon.aether.Aether;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.blocks.HBlockLibrary;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.animation.AnimationManager;
import de.erethon.questsxl.command.QCommandCache;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QRegistry;
import de.erethon.questsxl.dialogue.QDialogueManager;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.global.GlobalObjectives;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.listener.PlayerJobListener;
import de.erethon.questsxl.listener.PlayerListener;
import de.erethon.questsxl.livingworld.QEventManager;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.quest.QuestManager;
import de.erethon.questsxl.region.QRegionManager;
import de.erethon.questsxl.respawn.RespawnPointManager;
//import de.erethon.questsxl.scoreboard.QuestScoreboardLines;
import de.erethon.questsxl.tool.GitSync;
import de.fyreum.jobsxl.JobsXL;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class QuestsXL extends EPlugin implements Listener {

    static QuestsXL instance;
    public static String ERROR = "<dark_gray>[<red><bold>!<!bold><dark_gray>]<gray> ";

    public static File ANIMATIONS;
    public static File QUESTS;
    public static File EVENTS;
    public static File PLAYERS;
    public static File REGIONS;
    public static File RESPAWNS;
    public static File GLOBAL_OBJ;
    public static File IBCS;
    public static File SCHEMATICS;
    public static File DIALOGUES;
    public long lastSync = 0;

    private QPlayerCache qPlayerCache;
    private RespawnPointManager respawnPointManager;
    private QuestManager questManager;
    private QEventManager eventManager;
    private QRegionManager regionManager;
    private AnimationManager animationManager;
    private BlockCollectionManager blockCollectionManager;
    private QDialogueManager dialogueManager;
    private QCommandCache commandCache;
    private GlobalObjectives globalObjectives;
    private PlayerListener playerListener;
    private PlayerJobListener playerJobListener;

    private final Map<String, Integer> scores = new HashMap<>();
    private final List<FriendlyError> errors = new ArrayList<>();
    private boolean showStacktraces = true;

    private File gitConfig = new File(Bukkit.getPluginsFolder().getParent(), "gitConfig.yml");
    private List<String> folders;

    private String gitToken;
    private String gitBranch;

    boolean gitSync = true;

    private Aergia aergia;
    private Aether aether;
    private JobsXL jobsXL;

    //DungeonsAPI dungeonsAPI;
    private WorldEditPlugin worldEditPlugin;
    private Hephaestus hephaestus;

    public QuestsXL() {
        settings = EPluginSettings.builder()
                .internals(Internals.v1_19_R1)
                .forcePaper(true)
                .build();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        instance = this;
        YamlConfiguration gitConfig = YamlConfiguration.loadConfiguration(this.gitConfig);
        gitToken = gitConfig.getString("token");
        gitBranch = gitConfig.getString("branch");
        folders = gitConfig.getStringList("folders");
        QRegistries.init();
        initFolder(getDataFolder());
        initFolder(QUESTS = new File(getDataFolder(), "quests"));
        initFolder(EVENTS = new File(getDataFolder(), "events"));
        initFolder(PLAYERS = new File(getDataFolder(), "players"));
        initFolder(ANIMATIONS = new File(getDataFolder(), "animations"));
        initFolder(IBCS = new File(getDataFolder(), "blocks"));
        initFolder(SCHEMATICS = new File(getDataFolder(), "schematics"));
        initFolder(DIALOGUES = new File(getDataFolder(), "dialogues"));

        initFile(REGIONS = new File(getDataFolder(), "regions.yml"));
        initFile(RESPAWNS = new File(getDataFolder(), "respawnPoints.yml"));
        initFile(GLOBAL_OBJ = new File(getDataFolder(), "globalObjectives.yml"));
        if (getServer().getPluginManager().getPlugin("Aergia") != null) {
            aergia = (Aergia) getServer().getPluginManager().getPlugin("Aergia");
        }
        if (getServer().getPluginManager().getPlugin("Aether") != null) {
            aether = (Aether) getServer().getPluginManager().getPlugin("Aether");
        }
        if (getServer().getPluginManager().getPlugin("JobsXL") != null) {
            jobsXL = (JobsXL) getServer().getPluginManager().getPlugin("JobsXL");
        }
        if (getServer().getPluginManager().getPlugin("Hephaestus") != null) {
            hephaestus = (Hephaestus) getServer().getPluginManager().getPlugin("Hephaestus");
        }
        qPlayerCache = new QPlayerCache(this);
        MessageUtil.log(" ");
        MessageUtil.log(" ");
        MessageUtil.log(" --- Sync ---");
        if (gitToken == null || gitBranch == null) {
            MessageUtil.log("Environment: OFFLINE");
            gitSync = false;
            loadCore();
        } else {
            MessageUtil.log("Environment: " + gitBranch);
            sync();
        }
        MessageUtil.log(" ");
        MessageUtil.log(" ");
    }

    public void initFolder(File folder) {
        if (!folder.exists()) {
            folder.mkdir();
        }
    }

    private void initFile(File file) {
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void loadCore() {
        respawnPointManager = new RespawnPointManager(RESPAWNS);
        regionManager = new QRegionManager(REGIONS);
        blockCollectionManager = new BlockCollectionManager(IBCS);
        animationManager = new AnimationManager(ANIMATIONS);
        questManager = new QuestManager(); // Load after sync
        questManager.load();
        eventManager = new QEventManager();
        eventManager.load(EVENTS);
        dialogueManager = new QDialogueManager(DIALOGUES);
        dialogueManager.load();
        try {
            MessageUtil.log("Loading global objectives...");
            globalObjectives = new GlobalObjectives(GLOBAL_OBJ);
        } catch (Exception e) {
            errors.add(new FriendlyError("Global", "Failed to load global objectives", e.getMessage(), "Schaue im Stacktrace nach dem Fehler.").addStacktrace(e.getStackTrace()));
            MessageUtil.broadcastMessageIf("Errors: " + QuestsXL.getInstance().getErrors().size(), p -> p.hasPermission("qxl.admin.info"));
        }
        commandCache = new QCommandCache(this);
        commandCache.register(this);
        setCommandCache(commandCache);

        playerListener = new PlayerListener();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(playerListener, this);

        // dependency listeners
        if (jobsXL != null) {
            playerJobListener = new PlayerJobListener();
            getServer().getPluginManager().registerEvents(playerJobListener, this);
        }
        if (aergia != null) {
            //aergia.getEScoreboard().addScores(new QuestScoreboardLines());
        }
    }

    @Override
    public void onDisable() {
        regionManager.save();
        blockCollectionManager.save();
        animationManager.save();
        eventManager.save();
        HandlerList.unregisterAll((Plugin) this);
    }

    public void addScore(String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0));
    }

    public void removeScore(String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) - amount);
    }

    public void setScore(String score, int amount) {
        scores.put(score, amount);
    }

    public int getScore(String id) {
        return scores.getOrDefault(id, 0);
    }

    public QPlayerCache getPlayerCache() {
        return qPlayerCache;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public QEventManager getEventManager() {
        return eventManager;
    }

    public static QuestsXL getInstance() {
        return instance;
    }

    public Aether getAether() {
        return aether;
    }

    public JobsXL getJobsXL() {
        return jobsXL;
    }

    public HItemLibrary getItemLibrary() {
        return hephaestus.getLibrary();
    }

    public HBlockLibrary getBlockLibrary() {
        return hephaestus.getBlockLibrary();
    }

    public QRegionManager getRegionManager() {
        return regionManager;
    }

    public BlockCollectionManager getBlockCollectionManager() {
        return blockCollectionManager;
    }

    public AnimationManager getAnimationManager() {
        return animationManager;
    }

    public QDialogueManager getDialogueManager() {
        return dialogueManager;
    }

    public GlobalObjectives getGlobalObjectives() {
        return globalObjectives;
    }

    public void reload() {
        errors.clear();
        onDisable();
        loadCore();
    }

    public boolean isWEEnabled() {
        return worldEditPlugin != null && worldEditPlugin.isEnabled();
    }

    public boolean isAergiaEnabled() {
        return aergia != null && aergia.isEnabled();
    }

    public boolean isAetherEnabled() {
        return aether != null && aether.isEnabled();
    }

    public boolean isJXLEnabled() {
        return jobsXL != null && jobsXL.isEnabled();
    }

    public boolean isHephaestusEnabled() {
        return hephaestus != null && hephaestus.isEnabled();
    }

    public boolean isRunningPapyrus() {
        return Bukkit.getName().equalsIgnoreCase("Papyrus");
    }

    public List<FriendlyError> getErrors() {
        return errors;
    }

    public void setShowStacktraces(boolean showStacktraces) {
        this.showStacktraces = showStacktraces;
    }

    public boolean isShowStacktraces() {
        return showStacktraces;
    }

    public void sync() {
        BukkitRunnable updateGit = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    MessageUtil.log("Syncing...");
                    MessageUtil.log("Included folders: " + Arrays.toString(folders.toArray()));
                    GitSync sync = new GitSync(folders);
                    sync.update();
                } catch (IOException | InterruptedException | GitAPIException e) {
                    MessageUtil.broadcastMessageIf("&cGithub-Sync-Error: " + e.getMessage(), p -> p.hasPermission("qxl.admin.sync"));
                    e.printStackTrace();
                }
                BukkitRunnable waitForCopy = new BukkitRunnable() {
                    @Override
                    public void run() {
                        loadCore();
                        MessageUtil.broadcastMessageIf("&aGitHub-Sync abgeschlossen!", p -> p.hasPermission("qxl.admin.sync"));
                    }
                };
                waitForCopy.runTaskLaterAsynchronously(QuestsXL.getInstance(), 60);
                lastSync = System.currentTimeMillis();
            }
        };
        updateGit.runTaskAsynchronously(this);
    }

    public void debug(String msg) {
         // check for debug mode here
        MessageUtil.log(msg);
    }

    @Contract("_ -> new")
    public static @NotNull File getPlayerFile(@NotNull UUID uuid) {
        return new File(PLAYERS, uuid + ".yml");
    }

    public String getGitToken() {
        return gitToken;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public boolean isGitSync() {
        return gitSync;
    }
}
