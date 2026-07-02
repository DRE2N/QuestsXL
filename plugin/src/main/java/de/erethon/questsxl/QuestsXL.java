package de.erethon.questsxl;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import de.erethon.aergia.Aergia;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.database.BedrockDBConnection;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.blocks.HBlockLibrary;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.hephaestus.jobs.JobDatabaseManager;
import de.erethon.questsxl.animation.AnimationManager;
import de.erethon.questsxl.command.QCommandCache;
import de.erethon.questsxl.common.CommonMessages;
import de.erethon.questsxl.common.script.MacroRegistry;
import de.erethon.questsxl.common.script.QMessageHandler;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QRegistry;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.doc.RuntimeDocGenerator;
import de.erethon.questsxl.common.data.QDatabaseManager;
import de.erethon.questsxl.dialogue.QDialogueManager;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.global.GlobalObjectives;
import de.erethon.questsxl.livingworld.instancing.BlockCollectionManager;
import de.erethon.questsxl.livingworld.instancing.InstanceManager;
import de.erethon.questsxl.livingworld.instancing.apartment.ApartmentService;
import de.erethon.questsxl.livingworld.instancing.apartment.ApartmentSignListener;
import de.erethon.questsxl.livingworld.instancing.apartment.TychePaymentHandler;
import de.erethon.questsxl.livingworld.interaction.InteractionManager;
import de.erethon.questsxl.listener.PlayerListener;
import de.erethon.questsxl.listener.PluginListener;
import de.erethon.questsxl.livingworld.AutoTrackingManager;
import de.erethon.questsxl.livingworld.Exploration;
import de.erethon.questsxl.livingworld.ExplorationManager;
import de.erethon.questsxl.livingworld.LootChestManager;
import de.erethon.questsxl.livingworld.QEventManager;
import de.erethon.questsxl.component.objective.event.ObjectiveEventManager;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.PeriodicQuestManager;
import de.erethon.questsxl.quest.QuestManager;
import de.erethon.questsxl.livingworld.region.QRegionManager;
import de.erethon.questsxl.livingworld.region.RegionInstanceService;
import de.erethon.questsxl.livingworld.respawn.RespawnPointManager;
import de.erethon.questsxl.scoreboard.QuestScoreboardLines;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class QuestsXL extends EPlugin {

    private static QuestsXL plugin;
    public static String ERROR = "<dark_gray>[<red><bold>!<!bold><dark_gray>]<gray> ";
    public static String EXPLORATION = "<dark_gray>[<yellow>\uD83E\uDDED<dark_gray>]<gray> ";

    public static File ANIMATIONS;
    public static File QUESTS;
    public static File EVENTS;
    public static File PLAYERS;
    public static File REGIONS;
    public static File RESPAWNS;
    public static File EXPLORABLES;
    public static File EXPLORATION_SETS;
    public static File GLOBAL_OBJ;
    public static File IBCS;
    public static File SCHEMATICS;
    public static File DIALOGUES;
    public static File INTERACTIONS;
    public static File PERIODIC_QUESTS;
    public static File MACROS;

    private final QMessageHandler messageHandler = new QMessageHandler();

    private MacroRegistry macroRegistry;
    private QDatabaseManager databaseManager;
    private RespawnPointManager respawnPointManager;
    private QuestManager questManager;
    private QEventManager eventManager;
    private QRegionManager regionManager;
    private AnimationManager animationManager;
    private BlockCollectionManager blockCollectionManager;
    private InstanceManager instanceManager;
    private ApartmentService apartmentService;
    private QDialogueManager dialogueManager;
    private QCommandCache commandCache;
    private GlobalObjectives globalObjectives;
    private PlayerListener playerListener;
    private Exploration exploration;
    private ExplorationManager explorationManager;
    private ObjectiveEventManager objectiveEventManager;
    private AutoTrackingManager autoTrackingManager;
    private LootChestManager lootChestManager;
    private InteractionManager interactionManager;
    private PeriodicQuestManager periodicQuestManager;

    private QuestScoreboardLines scoreboardLines;

    private final Map<String, Integer> scores = new HashMap<>();
    private final List<FriendlyError> errors = new ArrayList<>();
    private boolean showStacktraces = true;

    private boolean internalReload = false;

    private Aergia aergia;

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
        plugin = this;
        ensureConfigDefaults();
        getServer().getPluginManager().registerEvents(new PluginListener(), this);
        // Check for dependencies
        if (getServer().getPluginManager().getPlugin("Aergia") != null) {
            aergia = (Aergia) getServer().getPluginManager().getPlugin("Aergia");
        }
        if (getServer().getPluginManager().getPlugin("Hephaestus") != null) {
            hephaestus = (Hephaestus) getServer().getPluginManager().getPlugin("Hephaestus");
        }
        // Register the common messages. All other translations come from QComponents themselves
        new CommonMessages();
        // Initialize QRegistries
        QRegistries.init();

        // Now we can start loading content
        initFolder(getDataFolder());
        initFolder(QUESTS = new File(getDataFolder(), "quests"));
        initFolder(EVENTS = new File(getDataFolder(), "events"));
        initFolder(PLAYERS = new File(getDataFolder(), "players"));
        initFolder(ANIMATIONS = new File(getDataFolder(), "animations"));
        initFolder(IBCS = new File(getDataFolder(), "blocks"));
        initFolder(SCHEMATICS = new File(getDataFolder(), "schematics"));
        initFolder(DIALOGUES = new File(getDataFolder(), "dialogues"));
        initFolder(INTERACTIONS = new File(getDataFolder(), "interactions"));
        initFolder(MACROS = new File(getDataFolder(), "macros"));

        initFile(REGIONS = new File(getDataFolder(), "regions.yml"));
        initFile(RESPAWNS = new File(getDataFolder(), "respawnPoints.yml"));
        initFile(EXPLORABLES = new File(getDataFolder(), "explorables.yml"));
        initFile(EXPLORATION_SETS = new File(getDataFolder(), "explorationSets.yml"));
        initFile(GLOBAL_OBJ = new File(getDataFolder(), "globalObjectives.yml"));
        initFile(PERIODIC_QUESTS = new File(getDataFolder(), "periodicQuests.yml"));
        YamlConfiguration environmentConfig = YamlConfiguration.loadConfiguration(new File(Bukkit.getWorldContainer(), "environment.yml"));
        try {
            BedrockDBConnection connection = new BedrockDBConnection(environmentConfig.getString("dbUrl"),
                    environmentConfig.getString("dbUser"),
                    environmentConfig.getString("dbPassword"),
                    "org.postgresql.ds.PGSimpleDataSource");
            databaseManager = new QDatabaseManager(connection);
        }
        catch (Exception e) {
            QuestsXL.log("Failed to connect to database. QXL will not work.");
            e.printStackTrace();
            return;
        }
        exploration = new Exploration();
    }

    private void ensureConfigDefaults() {
        saveDefaultConfig();
        FileConfiguration config = getConfig();
        config.addDefault("apartments.rentCost", 1);
        config.addDefault("apartments.rentDurationMinutes", 1440L);
        config.options().copyDefaults(true);
        saveConfig();
        reloadConfig();
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

    /**
     * Registers a component to a registry
     * @param registry
     * @param id
     * @param supplier
     */
    public void registerComponent(QRegistry<?> registry, String id, Supplier supplier) {
        QuestsXL.log("Registering " + id + " with " + supplier.get().getClass().getSimpleName());
        registry.register(id, supplier);
    }

    // Notably, this is only called on reload and in the ServerLoadEvent (the "Done" message on startup). This is to allow other
    // plugins to load their components before QuestsXL loads the content that uses them.
    public void loadCore() {
        // Load global macros first — quests and events will use them during load
        macroRegistry = new MacroRegistry();
        macroRegistry.loadFromDirectory(MACROS);

        respawnPointManager = new RespawnPointManager(RESPAWNS);
        regionManager = new QRegionManager(REGIONS);
        blockCollectionManager = new BlockCollectionManager(IBCS);
        animationManager = new AnimationManager(ANIMATIONS);

        // Initialize instance manager
        instanceManager = new InstanceManager(this);
        instanceManager.initialize();
        QuestsXL.log("Instance manager initialized");

        apartmentService = new ApartmentService(this, instanceManager);
        apartmentService.setPaymentHandler(new TychePaymentHandler()); // let's hope this works for dependency reasons
        getServer().getPluginManager().registerEvents(new ApartmentSignListener(this, apartmentService, instanceManager), this);
        QuestsXL.log("Apartment service initialized");

        // Initialize region instance service
        RegionInstanceService regionInstanceService = new RegionInstanceService(this, instanceManager, regionManager);
        regionManager.setInstanceService(regionInstanceService);
        QuestsXL.log("Region instance service initialized");

        questManager = new QuestManager(); // Load after sync
        if (eventManager != null) {
            eventManager.shutdown();
        }
        eventManager = new QEventManager();
        dialogueManager = new QDialogueManager(DIALOGUES);
        objectiveEventManager = new ObjectiveEventManager(this);
        try {
            QuestsXL.log("Loading global objectives...");
            globalObjectives = new GlobalObjectives(GLOBAL_OBJ);
        } catch (Exception e) {
            errors.add(new FriendlyError("Global", "Failed to load global objectives", e.getMessage(), "Schaue im Stacktrace nach dem Fehler.").addStacktrace(e.getStackTrace()));
            MessageUtil.broadcastMessageIf("Errors: " + QuestsXL.get().getErrors().size(), p -> p.hasPermission("qxl.admin.info"));
        }
        commandCache = new QCommandCache(this);
        commandCache.register(this);
        setCommandCache(commandCache);

        playerListener = new PlayerListener();

        getServer().getPluginManager().registerEvents(playerListener, this);

        if (isAergiaEnabled()) {
            if (scoreboardLines != null) {
                aergia.getEScoreboard().removeScores(scoreboardLines);
            }
            scoreboardLines = new QuestScoreboardLines();
            aergia.getEScoreboard().addScores(scoreboardLines);
        }
        QuestsXL.log("Loading QComponents...");
        dialogueManager.load();
        questManager.load();
        eventManager.load(EVENTS);

        // Initialize automatic tracking manager
        autoTrackingManager = new AutoTrackingManager(this);
        QuestsXL.log("Automatic tracking manager initialized");

        // Initialize exploration manager
        QuestsXL.log("Loading exploration data...");
        explorationManager = new ExplorationManager(EXPLORABLES, EXPLORATION_SETS);
        QuestsXL.log("Exploration manager initialized");

        // Initialize loot chest manager
        QuestsXL.log("Loading loot chest system...");
        lootChestManager = new LootChestManager(this);
        QuestsXL.log("Loot chest manager initialized");

        // Register loot chest listener
        getServer().getPluginManager().registerEvents(new de.erethon.questsxl.listener.LootChestListener(this), this);

        // Initialize interaction manager
        QuestsXL.log("Loading world interactions...");
        interactionManager = new InteractionManager(this);
        interactionManager.loadInteractions(INTERACTIONS);
        QuestsXL.log("Interaction manager initialized");

        // Initialize periodic quest manager
        QuestsXL.log("Loading periodic quests...");
        periodicQuestManager = new PeriodicQuestManager(this, PERIODIC_QUESTS);
        QuestsXL.log("Periodic quest manager initialized");

        exploration.initializeVFX(); // Initialize exploration VFX after exploration manager is loaded

        // Generate docs
        QuestsXL.log("Generating documentation...");
        Path docPath = getDataFolder().toPath().resolve("docs");
        if (!docPath.toFile().exists()) {
            docPath.toFile().mkdirs();
        }
        RuntimeDocGenerator docGen = new RuntimeDocGenerator(this, docPath);
        try {
            docGen.generate();
        } catch (Exception e) {
            errors.add(new FriendlyError("Docs", "Failed to generate runtime documentation", e.getMessage(), "Schaue im Stacktrace nach dem Fehler.").addStacktrace(e.getStackTrace()));
            MessageUtil.broadcastMessageIf("Errors: " + QuestsXL.get().getErrors().size(), p -> p.hasPermission("qxl.admin.info"));
        }
    }

    @Override
    public void onDisable() {
        // Shutdown instance manager
        if (instanceManager != null) {
            instanceManager.shutdown();
        }
        // Shutdown interaction manager
        if (interactionManager != null) {
            interactionManager.shutdown();
        }
        // Shutdown periodic quest manager
        if (periodicQuestManager != null) {
            periodicQuestManager.shutdown();
        }
        regionManager.save();
        blockCollectionManager.save();
        animationManager.save();
        for (QPlayer qPlayer : databaseManager.getPlayers()) {
            qPlayer.saveToDatabase();
        }
        if (explorationManager != null) {
            explorationManager.save();
        }
        HandlerList.unregisterAll(this);
    }

    public void addScore(String score, int amount) {
        setScore(score, scores.getOrDefault(score, 0) + amount);
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

    public QuestManager getQuestManager() {
        return questManager;
    }

    public QEventManager getEventManager() {
        return eventManager;
    }

    public static QuestsXL get() {
        return plugin;
    }

    public HItemLibrary getItemLibrary() {
        return hephaestus.getLibrary();
    }

    public HBlockLibrary getBlockLibrary() {
        return hephaestus.getBlockLibrary();
    }

    public JobDatabaseManager getJobDatabaseManager() {
        return hephaestus.getJobDatabaseManager();
    }

    public QRegionManager getRegionManager() {
        return regionManager;
    }

    public RespawnPointManager getRespawnPointManager() {
        return respawnPointManager;
    }

    public BlockCollectionManager getBlockCollectionManager() {
        return blockCollectionManager;
    }

    public ApartmentService getApartmentService() {
        return apartmentService;
    }

    public InstanceManager getInstanceManager() {
        return instanceManager;
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

    public Exploration getExploration() {
        return exploration;
    }

    public ObjectiveEventManager getObjectiveEventManager() {
        return objectiveEventManager;
    }

    public QDatabaseManager getDatabaseManager() {
       return databaseManager;
    }

    public MacroRegistry getMacroRegistry() {
        return macroRegistry;
    }
    public AutoTrackingManager getAutoTrackingManager() {
        return autoTrackingManager;
    }

    public ExplorationManager getExplorationManager() {
        return explorationManager;
    }

    public LootChestManager getLootChestManager() {
        return lootChestManager;
    }

    public InteractionManager getInteractionManager() {
        return interactionManager;
    }

    public PeriodicQuestManager getPeriodicQuestManager() {
        return periodicQuestManager;
    }

    public void reload() {
        errors.clear();
        if (objectiveEventManager != null) {
            objectiveEventManager.unregisterAll();
        }
        for (QPlayer qPlayer : databaseManager.getPlayers()) {
            qPlayer.saveToDatabase();
        }
        databaseManager.clearPlayers();
        internalReload = true;
        onDisable();
        internalReload = false;
        loadCore();
    }

    public boolean isWEEnabled() {
        return worldEditPlugin != null && worldEditPlugin.isEnabled();
    }

    public boolean isAergiaEnabled() {
        return aergia != null && aergia.isEnabled();
    }

    public boolean isHephaestusEnabled() {
        return hephaestus != null;
    }

    public boolean isRunningPapyrus() {
        return Bukkit.getName().equalsIgnoreCase("Papyrus");
    }

    public List<FriendlyError> getErrors() {
        return errors;
    }

    public void addRuntimeError(FriendlyError error) {
        errors.add(error);
        for (FriendlyError e : errors) {
            MessageUtil.broadcastMessageIf(e.getMessage(), p -> p.hasPermission("qxl.admin.info"));
        }
    }

    public void setShowStacktraces(boolean showStacktraces) {
        this.showStacktraces = showStacktraces;
    }

    public boolean isShowStacktraces() {
        return showStacktraces;
    }

    public void debug(String msg) {
         // check for debug mode here
        log(msg);
    }
    
    public static void log(String msg) {
        plugin.getLogger().info(msg);
    }

    public void registerTranslation(QTranslatable translatable) {
        messageHandler.registerTranslation(translatable);
    }

    @Contract("_ -> new")
    public static @NotNull File getPlayerFile(@NotNull UUID uuid) {
        return new File(PLAYERS, uuid + ".yml");
    }

}
