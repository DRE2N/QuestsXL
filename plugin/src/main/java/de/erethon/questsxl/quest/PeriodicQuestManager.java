package de.erethon.questsxl.quest;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

/**
 * Manages daily and weekly quests that rotate on a schedule.
 * All players share the same active daily/weekly quests during each period.
 */
public class PeriodicQuestManager {

    private final QuestsXL plugin;
    private final File configFile;
    private YamlConfiguration config;

    // Daily quest configuration
    private boolean dailyEnabled = true;
    private int dailyCount = 3;
    private LocalTime dailyResetTime = LocalTime.of(6, 0); // Default: 6:00
    private List<String> dailyQuestPool = new ArrayList<>();
    private final Set<QAction> dailyCompletionRewards = new HashSet<>();

    // Weekly quest configuration
    private boolean weeklyEnabled = true;
    private int weeklyCount = 3;
    private DayOfWeek weeklyResetDay = DayOfWeek.MONDAY;
    private LocalTime weeklyResetTime = LocalTime.of(6, 0); // Default: Monday 6:00
    private List<String> weeklyQuestPool = new ArrayList<>();
    private final Set<QAction> weeklyCompletionRewards = new HashSet<>();

    // Current active quests (same for all players)
    private final List<QQuest> activeDailyQuests = new ArrayList<>();
    private final List<QQuest> activeWeeklyQuests = new ArrayList<>();
    private long lastDailyReset = 0;
    private long lastWeeklyReset = 0;


    private BukkitRunnable resetTask;

    public PeriodicQuestManager(QuestsXL plugin, File configFile) {
        this.plugin = plugin;
        this.configFile = configFile;
        load();
        startResetScheduler();
    }

    public void load() {
        if (!configFile.exists()) {
            createDefaultConfig();
        }
        config = YamlConfiguration.loadConfiguration(configFile);

        try {
            loadDailyConfig();
            loadWeeklyConfig();
            loadStateFromDatabase();
            selectQuests();
        } catch (Exception e) {
            plugin.getErrors().add(new FriendlyError("PeriodicQuests", "Failed to load periodic quests", e.getMessage(), "Check your periodicQuests.yml configuration"));
            e.printStackTrace();
        }
    }

    private void loadDailyConfig() {
        ConfigurationSection dailySection = config.getConfigurationSection("daily");
        if (dailySection == null) return;

        dailyEnabled = dailySection.getBoolean("enabled", true);
        dailyCount = dailySection.getInt("count", 3);

        String timeStr = dailySection.getString("resetTime", "06:00");
        dailyResetTime = LocalTime.parse(timeStr);

        dailyQuestPool = dailySection.getStringList("questPool");

        // Load completion rewards
        if (dailySection.contains("completionRewards")) {
            @SuppressWarnings("unchecked")
            Collection<? extends QAction> rewards = (Collection<? extends QAction>) QConfigLoader.load(
                null, "completionRewards", dailySection, QRegistries.ACTIONS);
            if (rewards != null) {
                dailyCompletionRewards.addAll(rewards);
            }
        }

        lastDailyReset = dailySection.getLong("lastReset", 0);
    }

    private void loadWeeklyConfig() {
        ConfigurationSection weeklySection = config.getConfigurationSection("weekly");
        if (weeklySection == null) return;

        weeklyEnabled = weeklySection.getBoolean("enabled", true);
        weeklyCount = weeklySection.getInt("count", 3);

        String timeStr = weeklySection.getString("resetTime", "06:00");
        weeklyResetTime = LocalTime.parse(timeStr);

        String dayStr = weeklySection.getString("resetDay", "MONDAY");
        weeklyResetDay = DayOfWeek.valueOf(dayStr.toUpperCase());

        weeklyQuestPool = weeklySection.getStringList("questPool");

        // Load completion rewards
        if (weeklySection.contains("completionRewards")) {
            @SuppressWarnings("unchecked")
            Collection<? extends QAction> rewards = (Collection<? extends QAction>) QConfigLoader.load(
                null, "completionRewards", weeklySection, QRegistries.ACTIONS);
            if (rewards != null) {
                weeklyCompletionRewards.addAll(rewards);
            }
        }

        lastWeeklyReset = weeklySection.getLong("lastReset", 0);
    }

    private void loadStateFromDatabase() {
        var dbManager = plugin.getDatabaseManager();
        if (dbManager == null) return;

        var playerDao = dbManager.getPlayerDao();
        lastDailyReset = playerDao.getLastDailyReset().orElse(0L);
        lastWeeklyReset = playerDao.getLastWeeklyReset().orElse(0L);
        String dailyQuestsStr = playerDao.getActiveDailyQuests().orElse("");
        String weeklyQuestsStr = playerDao.getActiveWeeklyQuests().orElse("");

        activeDailyQuests.clear();
        if (!dailyQuestsStr.isEmpty()) {
            for (String name : dailyQuestsStr.split(",")) {
                QQuest quest = plugin.getQuestManager().getByName(name.trim());
                if (quest != null) {
                    activeDailyQuests.add(quest);
                }
            }
        }

        activeWeeklyQuests.clear();
        if (!weeklyQuestsStr.isEmpty()) {
            for (String name : weeklyQuestsStr.split(",")) {
                QQuest quest = plugin.getQuestManager().getByName(name.trim());
                if (quest != null) {
                    activeWeeklyQuests.add(quest);
                }
            }
        }
    }

    public void save() {
        saveStateToDatabase();
    }

    private void saveStateToDatabase() {
        var dbManager = plugin.getDatabaseManager();
        if (dbManager == null) return;

        var playerDao = dbManager.getPlayerDao();

        // Save daily state
        List<String> dailyNames = new ArrayList<>();
        for (QQuest quest : activeDailyQuests) {
            dailyNames.add(quest.getName());
        }
        playerDao.updateDailyQuestState(lastDailyReset, String.join(",", dailyNames));

        // Save weekly state
        List<String> weeklyNames = new ArrayList<>();
        for (QQuest quest : activeWeeklyQuests) {
            weeklyNames.add(quest.getName());
        }
        playerDao.updateWeeklyQuestState(lastWeeklyReset, String.join(",", weeklyNames));
    }

    private void createDefaultConfig() {
        config = new YamlConfiguration();

        // Daily defaults
        config.set("daily.enabled", true);
        config.set("daily.count", 3);
        config.set("daily.resetTime", "06:00");
        config.set("daily.questPool", Arrays.asList("example_daily_1", "example_daily_2", "example_daily_3"));
        config.set("daily.completionRewards", Collections.singletonList("message: &aYou completed all daily quests!"));

        // Weekly defaults
        config.set("weekly.enabled", true);
        config.set("weekly.count", 3);
        config.set("weekly.resetDay", "MONDAY");
        config.set("weekly.resetTime", "06:00");
        config.set("weekly.questPool", Arrays.asList("example_weekly_1", "example_weekly_2", "example_weekly_3"));
        config.set("weekly.completionRewards", Collections.singletonList("message: &aYou completed all weekly quests!"));

        try {
            config.save(configFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void selectQuests() {
        if (dailyEnabled && (activeDailyQuests.isEmpty() || shouldResetDaily())) {
            selectDailyQuests();
        }

        if (weeklyEnabled && (activeWeeklyQuests.isEmpty() || shouldResetWeekly())) {
            selectWeeklyQuests();
        }
    }

    private void selectDailyQuests() {
        activeDailyQuests.clear();
        List<String> pool = new ArrayList<>(dailyQuestPool);
        Collections.shuffle(pool);

        int selected = 0;
        for (String questName : pool) {
            if (selected >= dailyCount) break;

            QQuest quest = plugin.getQuestManager().getByName(questName);
            if (quest != null) {
                activeDailyQuests.add(quest);
                selected++;
            }
        }

        lastDailyReset = System.currentTimeMillis();
        QuestsXL.log("Selected " + activeDailyQuests.size() + " daily quests");
        save();
    }

    private void selectWeeklyQuests() {
        activeWeeklyQuests.clear();
        List<String> pool = new ArrayList<>(weeklyQuestPool);
        Collections.shuffle(pool);

        int selected = 0;
        for (String questName : pool) {
            if (selected >= weeklyCount) break;

            QQuest quest = plugin.getQuestManager().getByName(questName);
            if (quest != null) {
                activeWeeklyQuests.add(quest);
                selected++;
            }
        }

        lastWeeklyReset = System.currentTimeMillis();
        QuestsXL.log("Selected " + activeWeeklyQuests.size() + " weekly quests");
        save();
    }

    private boolean shouldResetDaily() {
        LocalDateTime lastReset = LocalDateTime.ofInstant(
            new Date(lastDailyReset).toInstant(),
            ZoneId.systemDefault()
        );
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayReset = now.with(dailyResetTime);

        if (now.isBefore(todayReset)) {
            todayReset = todayReset.minusDays(1);
        }

        return lastReset.isBefore(todayReset);
    }

    private boolean shouldResetWeekly() {
        LocalDateTime lastReset = LocalDateTime.ofInstant(
            new Date(lastWeeklyReset).toInstant(),
            ZoneId.systemDefault()
        );
        LocalDateTime now = LocalDateTime.now();

        LocalDateTime thisWeekReset = now.with(weeklyResetDay).with(weeklyResetTime);
        if (now.isBefore(thisWeekReset)) {
            thisWeekReset = thisWeekReset.minusWeeks(1);
        }

        return lastReset.isBefore(thisWeekReset);
    }

    private void startResetScheduler() {
        resetTask = new BukkitRunnable() {
            @Override
            public void run() {
                boolean resetOccurred = false;

                if (dailyEnabled && shouldResetDaily()) {
                    resetDaily();
                    resetOccurred = true;
                }

                if (weeklyEnabled && shouldResetWeekly()) {
                    resetWeekly();
                    resetOccurred = true;
                }

                if (resetOccurred) {
                    save();
                }
            }
        };
        resetTask.runTaskTimer(plugin, 20L * 60 * 5, 20L * 60 * 5);
    }

    private void resetDaily() {
        QuestsXL.log("Resetting daily quests...");

        var dbManager = plugin.getDatabaseManager();
        if (dbManager != null) {
            var playerDao = dbManager.getPlayerDao();
            playerDao.clearAllPlayersPeriodicProgress("DAILY");
        }

        selectDailyQuests();

        Bukkit.getServer().broadcast(Component.translatable("qxl.daily.reset.broadcast"));
    }

    private void resetWeekly() {
        QuestsXL.log("Resetting weekly quests...");

        var dbManager = plugin.getDatabaseManager();
        if (dbManager != null) {
            var playerDao = dbManager.getPlayerDao();
            playerDao.clearAllPlayersPeriodicProgress("WEEKLY");
        }

        selectWeeklyQuests();

        Bukkit.getServer().broadcast(Component.translatable("qxl.weekly.reset.broadcast"));
    }

    public void onQuestComplete(QPlayer player, QQuest quest) {
        UUID uuid = player.getPlayer().getUniqueId();
        boolean isDaily = activeDailyQuests.contains(quest);
        boolean isWeekly = activeWeeklyQuests.contains(quest);

        var dbManager = plugin.getDatabaseManager();
        if (dbManager == null) return;

        var playerDao = dbManager.getPlayerDao();

        if (isDaily) {
            playerDao.savePeriodicQuestProgress(uuid, "DAILY", quest.getName(), System.currentTimeMillis(), false);
            if (hasCompletedAllDaily(player)) {
                Boolean bonusClaimed = playerDao.getPeriodicBonusClaimed(uuid, "DAILY").orElse(false);
                if (!bonusClaimed) {
                    grantDailyBonus(player);
                }
            }
        }

        if (isWeekly) {
            playerDao.savePeriodicQuestProgress(uuid, "WEEKLY", quest.getName(), System.currentTimeMillis(), false);
            if (hasCompletedAllWeekly(player)) {
                Boolean bonusClaimed = playerDao.getPeriodicBonusClaimed(uuid, "WEEKLY").orElse(false);
                if (!bonusClaimed) {
                    grantWeeklyBonus(player);
                }
            }
        }
    }

    private void grantDailyBonus(QPlayer player) {
        for (QAction action : dailyCompletionRewards) {
            try {
                action.play(player);
            } catch (Exception e) {
                plugin.getErrors().add(new FriendlyError("PeriodicQuests", "Failed to grant daily bonus", e.getMessage(), "Player: " + player.getName()));
            }
        }

        var dbManager = plugin.getDatabaseManager();
        if (dbManager != null) {
            var playerDao = dbManager.getPlayerDao();
            playerDao.setPeriodicBonusClaimed(player.getPlayer().getUniqueId(), "DAILY", true);
        }

        player.getPlayer().sendMessage(Component.translatable("qxl.daily.bonus.received"));
    }

    private void grantWeeklyBonus(QPlayer player) {
        for (QAction action : weeklyCompletionRewards) {
            try {
                action.play(player);
            } catch (Exception e) {
                plugin.getErrors().add(new FriendlyError("PeriodicQuests", "Failed to grant weekly bonus", e.getMessage(), "Player: " + player.getName()));
            }
        }

        // Mark bonus as claimed in database
        var dbManager = plugin.getDatabaseManager();
        if (dbManager != null) {
            var playerDao = dbManager.getPlayerDao();
            playerDao.setPeriodicBonusClaimed(player.getPlayer().getUniqueId(), "WEEKLY", true);
        }

        player.getPlayer().sendMessage(Component.translatable("qxl.weekly.bonus.received"));
    }

    public boolean hasCompletedAllDaily(QPlayer player) {
        if (!dailyEnabled || activeDailyQuests.isEmpty()) return false;

        var dbManager = plugin.getDatabaseManager();
        if (dbManager == null) return false;

        var playerDao = dbManager.getPlayerDao();
        List<de.erethon.questsxl.player.QPlayerDao.PeriodicQuestProgressData> completed =
            playerDao.getPeriodicQuestProgress(player.getPlayer().getUniqueId(), "DAILY");

        Set<String> completedQuestNames = new HashSet<>();
        for (var data : completed) {
            completedQuestNames.add(data.questId);
        }

        for (QQuest quest : activeDailyQuests) {
            if (!completedQuestNames.contains(quest.getName())) {
                return false;
            }
        }
        return true;
    }

    public boolean hasCompletedAllWeekly(QPlayer player) {
        if (!weeklyEnabled || activeWeeklyQuests.isEmpty()) return false;

        var dbManager = plugin.getDatabaseManager();
        if (dbManager == null) return false;

        var playerDao = dbManager.getPlayerDao();
        List<de.erethon.questsxl.player.QPlayerDao.PeriodicQuestProgressData> completed =
            playerDao.getPeriodicQuestProgress(player.getPlayer().getUniqueId(), "WEEKLY");

        Set<String> completedQuestNames = new HashSet<>();
        for (var data : completed) {
            completedQuestNames.add(data.questId);
        }

        for (QQuest quest : activeWeeklyQuests) {
            if (!completedQuestNames.contains(quest.getName())) {
                return false;
            }
        }
        return true;
    }

    public boolean isDailyQuest(QQuest quest) {
        return activeDailyQuests.contains(quest);
    }

    public boolean isWeeklyQuest(QQuest quest) {
        return activeWeeklyQuests.contains(quest);
    }

    public List<QQuest> getActiveDailyQuests() {
        return new ArrayList<>(activeDailyQuests);
    }

    public List<QQuest> getActiveWeeklyQuests() {
        return new ArrayList<>(activeWeeklyQuests);
    }

    public int getDailyProgress(QPlayer player) {
        var dbManager = plugin.getDatabaseManager();
        if (dbManager == null) return 0;

        var playerDao = dbManager.getPlayerDao();
        List<de.erethon.questsxl.player.QPlayerDao.PeriodicQuestProgressData> completed =
            playerDao.getPeriodicQuestProgress(player.getPlayer().getUniqueId(), "DAILY");
        return completed.size();
    }

    public int getWeeklyProgress(QPlayer player) {
        var dbManager = plugin.getDatabaseManager();
        if (dbManager == null) return 0;

        var playerDao = dbManager.getPlayerDao();
        List<de.erethon.questsxl.player.QPlayerDao.PeriodicQuestProgressData> completed =
            playerDao.getPeriodicQuestProgress(player.getPlayer().getUniqueId(), "WEEKLY");
        return completed.size();
    }

    public void shutdown() {
        if (resetTask != null) {
            resetTask.cancel();
        }
        save();
    }

    public boolean isDailyEnabled() {
        return dailyEnabled;
    }

    public boolean isWeeklyEnabled() {
        return weeklyEnabled;
    }

    public LocalDateTime getNextDailyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReset = now.with(dailyResetTime);

        if (now.isAfter(nextReset) || now.equals(nextReset)) {
            nextReset = nextReset.plusDays(1);
        }

        return nextReset;
    }

    public LocalDateTime getNextWeeklyReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime nextReset = now.with(weeklyResetDay).with(weeklyResetTime);

        if (now.isAfter(nextReset) || now.equals(nextReset)) {
            nextReset = nextReset.plusWeeks(1);
        }

        return nextReset;
    }
}

