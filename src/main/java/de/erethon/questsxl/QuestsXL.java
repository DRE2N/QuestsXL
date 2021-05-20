package de.erethon.questsxl;

import com.google.gson.Gson;
import de.erethon.aether.Aether;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.compatibility.Internals;
import de.erethon.commons.javaplugin.DREPlugin;
import de.erethon.commons.javaplugin.DREPluginSettings;
import de.erethon.questsxl.animation.AnimationManager;
import de.erethon.questsxl.commands.QCommandCache;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.global.GlobalObjectives;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.listener.PacketListener;
import de.erethon.questsxl.listener.PlayerListener;
import de.erethon.questsxl.players.QPlayerCache;
import de.erethon.questsxl.quest.QuestManager;
import de.erethon.questsxl.regions.QRegionManager;
import de.erethon.questsxl.respawn.RespawnPointManager;
import de.erethon.questsxl.tools.GitSync;
import de.erethon.vignette.api.VignetteAPI;
import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public final class QuestsXL extends DREPlugin implements Listener {

    static QuestsXL instance;
    public static String ERROR = "<dark_gray>[<red><bold>!<reset><dark_gray>]<gray> ";

    public static File ANIMATIONS;
    public static File QUESTS;
    public static File PLAYERS;
    public static File REGIONS;
    public static File RESPAWNS;
    public static File GLOBAL_OBJ;
    public static File IBCS;
    public long lastSync = 0;

    QPlayerCache qPlayerCache;
    RespawnPointManager respawnPointManager;
    QuestManager questManager;
    QRegionManager regionManager;
    AnimationManager animationManager;
    BlockCollectionManager blockCollectionManager;
    QCommandCache commandCache;
    GlobalObjectives globalObjectives;
    PlayerListener playerListener;
    PacketListener packetListener;

    private List<FriendlyError> errors = new ArrayList<>();
    private boolean showStacktraces = false;

    Aether aether;

    public QuestsXL() {
        settings = DREPluginSettings.builder()
                .paper(true)
                .internals(Internals.v1_16_R3)
                .build();
    }

    @Override
    public void onEnable() {
        super.onEnable();
        if (!compat.isPaper()) {
            MessageUtil.log("Please use Paper. https://papermc.io/");
            Bukkit.getPluginManager().disablePlugin(this);
        }
        instance = this;
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }
        QUESTS = new File(getDataFolder(), "quests");
        if (!QUESTS.exists()) {
            QUESTS.mkdir();
        }
        PLAYERS = new File(getDataFolder(), "players");
        if (!PLAYERS.exists()) {
            PLAYERS.mkdir();
        }
        ANIMATIONS = new File(getDataFolder(), "animations");
        if (!ANIMATIONS.exists()) {
            ANIMATIONS.mkdir();
        }
        IBCS = new File(getDataFolder(), "blocks");
        if (!IBCS.exists()) {
            IBCS.mkdir();
        }
        REGIONS = new File(getDataFolder(), "regions.yml");
        if (!REGIONS.exists()) {
            try {
                REGIONS.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        RESPAWNS = new File(getDataFolder(), "respawnPoints.yml");
        if (!RESPAWNS.exists()) {
            try {
                RESPAWNS.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        GLOBAL_OBJ = new File(getDataFolder(), "globalObjectives.yml");
        if (!GLOBAL_OBJ.exists()) {
            try {
                GLOBAL_OBJ.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        VignetteAPI.init(this);
        aether = (Aether) Bukkit.getPluginManager().getPlugin("Aether");
        qPlayerCache = new QPlayerCache();
        getServer().getPluginManager().registerEvents(qPlayerCache, this);

        load();
    }

    public void load() {
        respawnPointManager = new RespawnPointManager(RESPAWNS);
        regionManager = new QRegionManager(REGIONS);
        blockCollectionManager = new BlockCollectionManager(IBCS);
        animationManager = new AnimationManager(ANIMATIONS);
        questManager = new QuestManager(); // Load after sync
        questManager.load();
        try {
            MessageUtil.log("Loading global objectives...");
            globalObjectives = new GlobalObjectives(GLOBAL_OBJ);
        } catch (Exception e) {
            errors.add(new FriendlyError("Global", "Failed to load global objectives", e.getMessage(), "Schaue im Stacktrace nach dem Fehler.").addStacktrace(e.getStackTrace()));
            MessageUtil.broadcastMessage("Errors: " + QuestsXL.getInstance().getErrors().size());
        }
        commandCache = new QCommandCache(this);
        commandCache.register(this);
        setCommandCache(commandCache);

        playerListener = new PlayerListener();
        packetListener = new PacketListener();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(playerListener, this);
    }


    @Override
    public void onDisable() {
        regionManager.save();
        blockCollectionManager.save();
        animationManager.save();
    }

    public QPlayerCache getPlayerCache() {
        return qPlayerCache;
    }

    public QuestManager getQuestManager() {
        return questManager;
    }

    public static QuestsXL getInstance() {
        return instance;
    }

    public Aether getAether() {
        return aether;
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

    public GlobalObjectives getGlobalObjectives() {
        return globalObjectives;
    }

    public void reload() {
        errors.clear();
        onDisable();
        load();
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
                    GitSync sync = new GitSync();
                    MessageUtil.log("[Git] Setting up...");
                    sync.setupRepo();
                    MessageUtil.log("[Git] Pulling...");
                    sync.update();
                } catch (IOException | InterruptedException e) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.hasPermission("qxl.admin.sync")) {
                            MessageUtil.sendMessage(player, "&cGithub-Sync-Error: " + e.getMessage());
                        }
                    }
                    e.printStackTrace();
                }
                BukkitRunnable waitForCopy = new BukkitRunnable() {
                    @Override
                    public void run() {
                        load();
                        for (Player player : Bukkit.getOnlinePlayers()) {
                            if (player.hasPermission("qxl.admin.sync")) {
                                MessageUtil.sendMessage(player, "&aGitHub-Sync abgeschlossen!");
                            }
                        }
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
}
