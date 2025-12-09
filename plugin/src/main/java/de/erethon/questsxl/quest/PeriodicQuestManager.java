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

    // Player tracking
    private final Map<UUID, Set<String>> playerDailyCompleted = new HashMap<>();
    private final Map<UUID, Set<String>> playerWeeklyCompleted = new HashMap<>();
    private final Map<UUID, Boolean> playerDailyBonusClaimed = new HashMap<>();
    private final Map<UUID, Boolean> playerWeeklyBonusClaimed = new HashMap<>();

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
            loadPlayerData();
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

    private void loadPlayerData() {
        ConfigurationSection playerSection = config.getConfigurationSection("playerData");
        if (playerSection == null) return;

        for (String uuidStr : playerSection.getKeys(false)) {
            UUID uuid = UUID.fromString(uuidStr);
            ConfigurationSection playerData = playerSection.getConfigurationSection(uuidStr);
            if (playerData == null) continue;

            playerDailyCompleted.put(uuid, new HashSet<>(playerData.getStringList("dailyCompleted")));
            playerWeeklyCompleted.put(uuid, new HashSet<>(playerData.getStringList("weeklyCompleted")));
            playerDailyBonusClaimed.put(uuid, playerData.getBoolean("dailyBonusClaimed", false));
            playerWeeklyBonusClaimed.put(uuid, playerData.getBoolean("weeklyBonusClaimed", false));
        }

        // Load active quests
        List<String> dailyQuestNames = config.getStringList("activeDailyQuests");
        List<String> weeklyQuestNames = config.getStringList("activeWeeklyQuests");

        activeDailyQuests.clear();
        for (String name : dailyQuestNames) {
            QQuest quest = plugin.getQuestManager().getByName(name);
            if (quest != null) {
                activeDailyQuests.add(quest);
            }
        }

        activeWeeklyQuests.clear();
        for (String name : weeklyQuestNames) {
            QQuest quest = plugin.getQuestManager().getByName(name);
            if (quest != null) {
                activeWeeklyQuests.add(quest);
            }
        }
    }

    public void save() {
        try {
            // Save daily config
            config.set("daily.lastReset", lastDailyReset);

            // Save weekly config
            config.set("weekly.lastReset", lastWeeklyReset);

            // Save active quests
            List<String> dailyNames = new ArrayList<>();
            for (QQuest quest : activeDailyQuests) {
                dailyNames.add(quest.getName());
            }
            config.set("activeDailyQuests", dailyNames);

            List<String> weeklyNames = new ArrayList<>();
            for (QQuest quest : activeWeeklyQuests) {
                weeklyNames.add(quest.getName());
            }
            config.set("activeWeeklyQuests", weeklyNames);

            // Save player data
            for (Map.Entry<UUID, Set<String>> entry : playerDailyCompleted.entrySet()) {
                String path = "playerData." + entry.getKey().toString();
                config.set(path + ".dailyCompleted", new ArrayList<>(entry.getValue()));
                config.set(path + ".dailyBonusClaimed", playerDailyBonusClaimed.getOrDefault(entry.getKey(), false));
            }

            for (Map.Entry<UUID, Set<String>> entry : playerWeeklyCompleted.entrySet()) {
                String path = "playerData." + entry.getKey().toString();
                config.set(path + ".weeklyCompleted", new ArrayList<>(entry.getValue()));
                config.set(path + ".weeklyBonusClaimed", playerWeeklyBonusClaimed.getOrDefault(entry.getKey(), false));
            }

            config.save(configFile);
        } catch (IOException e) {
            plugin.getErrors().add(new FriendlyError("PeriodicQuests", "Failed to save periodic quests", e.getMessage(), "Check file permissions"));
            e.printStackTrace();
        }
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

        // Calculate the most recent reset time
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

        // Check every 5 minutes
        resetTask.runTaskTimer(plugin, 20L * 60 * 5, 20L * 60 * 5);
    }

    private void resetDaily() {
        QuestsXL.log("Resetting daily quests...");

        // Clear player progress
        playerDailyCompleted.clear();
        playerDailyBonusClaimed.clear();

        // Select new quests
        selectDailyQuests();

        Bukkit.getServer().broadcast(Component.translatable("qxl.daily.reset.broadcast"));
    }

    private void resetWeekly() {
        QuestsXL.log("Resetting weekly quests...");

        // Clear player progress
        playerWeeklyCompleted.clear();
        playerWeeklyBonusClaimed.clear();

        // Select new quests
        selectWeeklyQuests();

        Bukkit.getServer().broadcast(Component.translatable("qxl.weekly.reset.broadcast"));
    }

    public void onQuestComplete(QPlayer player, QQuest quest) {
        UUID uuid = player.getPlayer().getUniqueId();
        boolean isDaily = activeDailyQuests.contains(quest);
        boolean isWeekly = activeWeeklyQuests.contains(quest);

        if (isDaily) {
            playerDailyCompleted.computeIfAbsent(uuid, k -> new HashSet<>()).add(quest.getName());

            // Check if all daily quests completed
            if (hasCompletedAllDaily(player) && !playerDailyBonusClaimed.getOrDefault(uuid, false)) {
                grantDailyBonus(player);
            }
            save();
        }

        if (isWeekly) {
            playerWeeklyCompleted.computeIfAbsent(uuid, k -> new HashSet<>()).add(quest.getName());

            // Check if all weekly quests completed
            if (hasCompletedAllWeekly(player) && !playerWeeklyBonusClaimed.getOrDefault(uuid, false)) {
                grantWeeklyBonus(player);
            }
            save();
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
        playerDailyBonusClaimed.put(player.getPlayer().getUniqueId(), true);
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
        playerWeeklyBonusClaimed.put(player.getPlayer().getUniqueId(), true);
        player.getPlayer().sendMessage(Component.translatable("qxl.weekly.bonus.received"));
    }

    public boolean hasCompletedAllDaily(QPlayer player) {
        if (!dailyEnabled || activeDailyQuests.isEmpty()) return false;

        Set<String> completed = playerDailyCompleted.get(player.getPlayer().getUniqueId());
        if (completed == null) return false;

        for (QQuest quest : activeDailyQuests) {
            if (!completed.contains(quest.getName())) {
                return false;
            }
        }
        return true;
    }

    public boolean hasCompletedAllWeekly(QPlayer player) {
        if (!weeklyEnabled || activeWeeklyQuests.isEmpty()) return false;

        Set<String> completed = playerWeeklyCompleted.get(player.getPlayer().getUniqueId());
        if (completed == null) return false;

        for (QQuest quest : activeWeeklyQuests) {
            if (!completed.contains(quest.getName())) {
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
        Set<String> completed = playerDailyCompleted.get(player.getPlayer().getUniqueId());
        return completed != null ? completed.size() : 0;
    }

    public int getWeeklyProgress(QPlayer player) {
        Set<String> completed = playerWeeklyCompleted.get(player.getPlayer().getUniqueId());
        return completed != null ? completed.size() : 0;
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

