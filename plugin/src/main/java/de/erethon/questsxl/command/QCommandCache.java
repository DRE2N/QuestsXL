package de.erethon.questsxl.command;

import de.erethon.bedrock.command.ECommandCache;
import de.erethon.bedrock.plugin.EPlugin;

public class QCommandCache extends ECommandCache {

    public static final String LABEL = "qxl";
    EPlugin plugin;

    public QCommandCache(EPlugin plugin) {
            super(LABEL, plugin);
            this.plugin = plugin;
            addCommand(new TestCommand());
            addCommand(new AdminCommand());
            addCommand(new RegionCommand());
            addCommand(new QuestCommand());
            addCommand(new MainCommand());
            addCommand(new IBCCommand());
            addCommand(new CutsceneCommand());
            addCommand(new SyncCommand());
            addCommand(new ReloadCommand());
            addCommand(new RunCommand());
            addCommand(new DialogueCommand());
            addCommand(new EventCommand());
            addCommand(new ExplorableCommand());
    }
}
