package de.erethon.questsxl;

import com.sk89q.worldedit.bukkit.WorldEditPlugin;
import de.erethon.aergia.Aergia;
import de.erethon.aergia.scoreboard.EScoreboard;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.compatibility.Internals;
import de.erethon.bedrock.plugin.EPlugin;
import de.erethon.bedrock.plugin.EPluginSettings;
import de.erethon.erethonscript.ast.ScriptNode;
import de.erethon.erethonscript.execution.Executable;
import de.erethon.erethonscript.execution.ExecutionContext;
import de.erethon.erethonscript.execution.ScriptCompiler;
import de.erethon.erethonscript.integration.NativeRegistry;
import de.erethon.erethonscript.parser.ScriptBuilder;
import de.erethon.hephaestus.Hephaestus;
import de.erethon.hephaestus.blocks.HBlockLibrary;
import de.erethon.hephaestus.items.HItemLibrary;
import de.erethon.questsxl.animation.AnimationManager;
import de.erethon.questsxl.command.QCommandCache;
import de.erethon.questsxl.common.CommonMessages;
import de.erethon.questsxl.common.QMessageHandler;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QRegistry;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.common.RuntimeDocGenerator;
import de.erethon.questsxl.common.script.ActionCompiler;
import de.erethon.questsxl.common.script.ConditionCompiler;
import de.erethon.questsxl.common.script.EventCompiler;
import de.erethon.questsxl.common.script.ObjectiveCompiler;
import de.erethon.questsxl.common.script.QuestCompiler;
import de.erethon.questsxl.common.script.StageCompiler;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.dialogue.QDialogueManager;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.global.GlobalObjectives;
import de.erethon.questsxl.instancing.BlockCollectionManager;
import de.erethon.questsxl.listener.PlayerListener;
import de.erethon.questsxl.listener.PluginListener;
import de.erethon.questsxl.livingworld.Exploration;
import de.erethon.questsxl.livingworld.QEventManager;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.objective.event.ObjectiveEventManager;
import de.erethon.questsxl.player.QPlayerCache;
import de.erethon.questsxl.quest.QuestManager;
import de.erethon.questsxl.region.QRegionManager;
import de.erethon.questsxl.respawn.RespawnPointManager;
import de.erethon.questsxl.scoreboard.QuestScoreboardLines;
import de.erethon.questsxl.tool.GitSync;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

public final class QuestsXL extends EPlugin {

    static QuestsXL instance;
    public static String ERROR = "<dark_gray>[<red><bold>!<!bold><dark_gray>]<gray> ";
    public static String EXPLORATION = "<dark_gray>[<yellow>\uD83E\uDDED<dark_gray>]<gray> ";

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

    private final QMessageHandler messageHandler = new QMessageHandler();

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
    private Exploration exploration;
    private ObjectiveEventManager objectiveEventManager;

    private final Map<String, Integer> scores = new HashMap<>();
    private final List<FriendlyError> errors = new ArrayList<>();
    private boolean showStacktraces = true;

    private File gitConfig = new File(Bukkit.getPluginsFolder().getParent(), "gitConfig.yml");
    private List<String> folders;

    private String gitToken;
    private String gitBranch;
    private boolean gitIsPassive = false;

    boolean gitSync = true;

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
        getServer().getPluginManager().registerEvents(new PluginListener(), this);
        instance = this;
        YamlConfiguration gitConfig = YamlConfiguration.loadConfiguration(this.gitConfig);
        gitToken = gitConfig.getString("token");
        gitBranch = gitConfig.getString("branch");
        folders = gitConfig.getStringList("folders");
        gitIsPassive = gitConfig.getBoolean("passive");
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

        initFile(REGIONS = new File(getDataFolder(), "regions.yml"));
        initFile(RESPAWNS = new File(getDataFolder(), "respawnPoints.yml"));
        initFile(GLOBAL_OBJ = new File(getDataFolder(), "globalObjectives.yml"));
        qPlayerCache = new QPlayerCache(this);
        exploration = new Exploration();
        MessageUtil.log(" ");
        MessageUtil.log(" ");
        MessageUtil.log(" --- Sync ---");
        if (gitToken == null || gitBranch == null) {
            MessageUtil.log("Environment: OFFLINE");
            gitSync = false;
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

    /**
     * Registers a component to a registry
     * @param registry
     * @param id
     * @param supplier
     */
    public void registerComponent(QRegistry<?> registry, String id, Supplier supplier) {
        MessageUtil.log("Registering " + id + " with " + supplier.get().getClass().getSimpleName());
        registry.register(id, supplier);
    }

    public void loadCore() {
        respawnPointManager = new RespawnPointManager(RESPAWNS);
        regionManager = new QRegionManager(REGIONS);
        blockCollectionManager = new BlockCollectionManager(IBCS);
        animationManager = new AnimationManager(ANIMATIONS);
        questManager = new QuestManager(); // Load after sync
        eventManager = new QEventManager();
        dialogueManager = new QDialogueManager(DIALOGUES);
        objectiveEventManager = new ObjectiveEventManager(this);
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

        getServer().getPluginManager().registerEvents(playerListener, this);

        if (isAergiaEnabled()) {
            aergia.getEScoreboard().addScores(new QuestScoreboardLines());
        }
        MessageUtil.log("Loading QComponents...");
        questManager.load();
        eventManager.load(EVENTS);
        dialogueManager.load();
        // Generate docs
        MessageUtil.log("Generating documentation...");
        Path docPath = getDataFolder().toPath().resolve("docs");
        if (!docPath.toFile().exists()) {
            docPath.toFile().mkdirs();
        }
        RuntimeDocGenerator docGen = new RuntimeDocGenerator(this, docPath);
        try {
            docGen.generate();
        } catch (Exception e) {
            errors.add(new FriendlyError("Docs", "Failed to generate runtime documentation", e.getMessage(), "Schaue im Stacktrace nach dem Fehler.").addStacktrace(e.getStackTrace()));
            MessageUtil.broadcastMessageIf("Errors: " + QuestsXL.getInstance().getErrors().size(), p -> p.hasPermission("qxl.admin.info"));
        }
        MessageUtil.log("Script test...");
        NativeRegistry registry = new NativeRegistry();
        registerScriptComponents(registry);
        // Test file
        File testFile = new File(getDataFolder(), "testScript.es");
        if (!testFile.exists()) {
            try {
                testFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            String fileContent = Files.readString(testFile.toPath());
            // Parse
            ScriptBuilder scriptBuilder = new ScriptBuilder();
            ScriptNode parsed = scriptBuilder.build(fileContent);
            // Compile
            ScriptCompiler compiler = new ScriptCompiler(registry);
            Executable compiled = compiler.compile(parsed);
            // Execute
            ExecutionContext context = new ExecutionContext();
            compiled.execute(context);
        } catch (Exception e) {
            throw new RuntimeException(e);
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

    public void registerScriptComponents(NativeRegistry registry) {

        ActionCompiler actionCompiler = new ActionCompiler(QRegistries.ACTIONS);

        registry.registerComponent("quest", new QuestCompiler(actionCompiler));
        registry.registerComponent("event", new EventCompiler(actionCompiler));

        registry.registerComponent("objective", new ObjectiveCompiler(QRegistries.OBJECTIVES, actionCompiler));
        registry.registerComponent("condition", new ConditionCompiler(QRegistries.CONDITIONS));
        registry.registerComponent("stage", new StageCompiler(actionCompiler));

        registry.registerComponent("action", actionCompiler);
        registry.registerComponent("completeAction", actionCompiler);
        registry.registerComponent("failAction", actionCompiler);
    }

    public void addAergiaScoreboardIntegration() {
        if (!isAergiaEnabled()) {
            return;
        }
        EScoreboard eScoreboard = aergia.getEScoreboard();
        eScoreboard.addScores(new QuestScoreboardLines());
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

    public Exploration getExploration() {
        return exploration;
    }

    public ObjectiveEventManager getObjectiveEventManager() {
        return objectiveEventManager;
    }

    public void reload() {
        getPlayerCache().saveAll();
        errors.clear();
        onDisable();
        loadCore();
        MessageUtil.log("Loading QComponents...");
        questManager.load();
        eventManager.load(EVENTS);
        dialogueManager.load();
    }

    public boolean isWEEnabled() {
        return worldEditPlugin != null && worldEditPlugin.isEnabled();
    }

    public boolean isAergiaEnabled() {
        return aergia != null && aergia.isEnabled();
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

    public void sync() {
        BukkitRunnable updateGit = new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    MessageUtil.log("Syncing...");
                    MessageUtil.log("Included folders: " + Arrays.toString(folders.toArray()));
                    GitSync sync = new GitSync(folders, isPassiveSync());
                    sync.update();
                } catch (IOException | InterruptedException | GitAPIException e) {
                    MessageUtil.broadcastMessageIf("&cGithub-Sync-Error: " + e.getMessage(), p -> p.hasPermission("qxl.admin.sync"));
                    e.printStackTrace();
                }
                BukkitRunnable waitForCopy = new BukkitRunnable() {
                    @Override
                    public void run() {
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

    public void registerTranslation(QTranslatable translatable) {
        messageHandler.registerTranslation(translatable);
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

    public boolean isPassiveSync() {
        return gitIsPassive;
    }
}
