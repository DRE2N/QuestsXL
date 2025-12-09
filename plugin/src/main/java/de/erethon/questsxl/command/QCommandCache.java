package de.erethon.questsxl.command;

import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.plugin.EPlugin;

import java.util.HashSet;
import java.util.Set;

public class QCommandCache extends ECommandCache {

    public static final String LABEL = "qxl";
    public static final Set<String> ALIASES = Set.of("q", "quests", "quest", "questsxl", "questxl");
    EPlugin plugin;

    public QCommandCache(EPlugin plugin) {
            super(LABEL, plugin, ALIASES, new HashSet<>());
            this.plugin = plugin;
            addCommand(new TestCommand());
            addCommand(new AdminCommand());
            addCommand(new RegionCommand());
            addCommand(new QuestCommand());
            addCommand(new MainCommand());
            addCommand(new IBCCommand());
            addCommand(new CutsceneCommand());
            addCommand(new SyncCommand());
            addCommand(new PushCommand());
            addCommand(new ReloadCommand());
            addCommand(new RunCommand());
            addCommand(new DialogueCommand());
            addCommand(new EventCommand());
            addCommand(new ExplorableCommand());
            addCommand(new ExplorationCommand());
            addCommand(new DailyCommand());
            addCommand(new WeeklyCommand());
    }
}
