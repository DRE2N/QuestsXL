package de.erethon.questsxl.common;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Set;

public class QConfigurationSection extends YamlConfiguration implements QConfig {

    public QConfigurationSection(ConfigurationSection section) {
        for (String key : section.getKeys(true)) {
            set(key, section.get(key));
        }
    }

    @Override
    public String[] getStringArray(String path) {
        return getStringList(path).toArray(new String[0]);
    }

    @Override
    public String[] getStringArray(String path, String[] def) {
        if (contains(path)) {
            return getStringArray(path);
        }
        return def;
    }

    @Override
    public QLocation getQLocation(String path) {
        return new QLocation(getConfigurationSection(path));
    }

    @Override
    public QLocation getQLocation(String path, QLocation def) {
        return contains(path) ? new QLocation(getConfigurationSection(path)) : def;
    }

    @Override
    public Set<QAction> getActions(String path) {
        return (Set<QAction>) QConfigLoader.load(path, this, QRegistries.ACTIONS);
    }

    @Override
    public Set<QCondition> getConditions(String path) {
        return (Set<QCondition>) QConfigLoader.load(path, this, QRegistries.CONDITIONS);
    }

    @Override
    public Set<QObjective> getObjectives(String path) {
        return (Set<QObjective>) QConfigLoader.load(path, this, QRegistries.OBJECTIVES);
    }

    @Override
    public QEvent getQEvent(String event) {
        return QuestsXL.getInstance().getEventManager().getByID(event);
    }

    @Override
    public QQuest getQuest(String quest) {
        return QuestsXL.getInstance().getQuestManager().getByName(quest);
    }
}
