package de.erethon.questsxl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.erethon.aether.Aether;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.commons.compatibility.Internals;
import de.erethon.commons.javaplugin.DREPlugin;
import de.erethon.commons.javaplugin.DREPluginSettings;
import de.erethon.questsxl.global.GlobalObjectives;
import de.erethon.questsxl.commands.QCommandCache;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.json.ItemstackTypeAdapter;
import de.erethon.questsxl.json.LocationTypeAdapter;
import de.erethon.questsxl.listener.PacketListener;
import de.erethon.questsxl.listener.PlayerListener;
import de.erethon.questsxl.players.QPlayerCache;
import de.erethon.questsxl.quest.QuestManager;
import de.erethon.questsxl.regions.QRegionManager;
import de.erethon.questsxl.tools.GitSync;
import de.erethon.vignette.api.VignetteAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;

public final class QuestsXL extends DREPlugin implements Listener {

    static QuestsXL instance;
    public static String ERROR = "<dark_gray>[<red><bold>!<reset><dark_gray>]<gray> ";

    public static File QUESTS;
    public static File PLAYERS;
    public static File REGIONS;
    public static File GLOBAL_OBJ;
    public static File IBCS;
    public long lastSync = 0;

    Gson gson =  new GsonBuilder()
            .registerTypeAdapter(Location.class, new LocationTypeAdapter())
            .registerTypeAdapter(ItemStack.class, new ItemstackTypeAdapter())
            .create();
    QPlayerCache qPlayerCache;
    QRegionManager regionManager;
    QCommandCache commandCache;
    GlobalObjectives globalObjectives;
    BlockCollectionManager blockCollectionManager;
    PlayerListener playerListener;
    PacketListener packetListener;
    QuestManager questManager;
    Aether aether;

    public QuestsXL() {
        settings = DREPluginSettings.builder()
                .paper(true)
                .economy(true)
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
        questManager = new QuestManager();
        regionManager = new QRegionManager(REGIONS);
        blockCollectionManager = new BlockCollectionManager(IBCS);
        globalObjectives = new GlobalObjectives(GLOBAL_OBJ);

        commandCache = new QCommandCache(this);
        commandCache.register(this);
        setCommandCache(commandCache);

        qPlayerCache = new QPlayerCache();
        playerListener = new PlayerListener();
        packetListener = new PacketListener();

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(qPlayerCache, this);
        getServer().getPluginManager().registerEvents(playerListener, this);
        sync();
    }


    @Override
    public void onDisable() {
        regionManager.save();
    }


    public Gson getGson() {
        return gson;
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

    public GlobalObjectives getGlobalObjectives() {
        return globalObjectives;
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
                    e.printStackTrace();
                }
                BukkitRunnable waitForCopy = new BukkitRunnable() {
                    @Override
                    public void run() {
                        questManager.load();
                    }
                };
                waitForCopy.runTaskLaterAsynchronously(QuestsXL.getInstance(), 120);
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
