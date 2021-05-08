package de.erethon.questsxl.commands;

import de.erethon.commons.command.DRECommandCache;
import de.erethon.commons.javaplugin.DREPlugin;

public class QCommandCache extends DRECommandCache {

    public static final String LABEL = "qxl";
    DREPlugin plugin;

    public QCommandCache(DREPlugin plugin) {
            super(LABEL, plugin);
            this.plugin = plugin;
            addCommand(new TestCommand());
            addCommand(new AdminCommand());
            addCommand(new RegionCommand());
            addCommand(new QuestCommand());
            addCommand(new MainCommand());
    }
}
