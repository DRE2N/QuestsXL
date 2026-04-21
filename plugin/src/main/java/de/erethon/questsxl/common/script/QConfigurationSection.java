package de.erethon.questsxl.common.script;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.component.action.QAction;
import de.erethon.questsxl.component.condition.QCondition;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.component.objective.QObjective;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Set;

public class QConfigurationSection extends YamlConfiguration implements QConfig {

    private final String source;

    public QConfigurationSection(ConfigurationSection section) {
        this(section, "unknown");
    }

    public QConfigurationSection(ConfigurationSection section, String source) {
        this.source = source;
        for (String key : section.getKeys(true)) {
            set(key, section.get(key));
        }
    }

    // ------------------------------------------------------------------
    // Resolution helpers
    // ------------------------------------------------------------------

    private String resolve(String raw) {
        if (raw == null) return null;
        ExecutionContext ctx = ExecutionContext.current();
        if (ctx != null && raw.contains("%")) {
            return ctx.resolveString(raw);
        }
        return raw;
    }

    // ------------------------------------------------------------------
    // QConfig scalar overrides with variable resolution
    // ------------------------------------------------------------------

    @Override
    public String getString(String path) {
        return resolve(super.getString(path));
    }

    @Override
    public String getString(String path, String def) {
        return resolve(super.getString(path, def));
    }

    @Override
    public int getInt(String path) {
        String raw = super.getString(path);
        if (raw != null && raw.contains("%")) {
            ExecutionContext ctx = ExecutionContext.current();
            if (ctx != null) return ctx.resolveInt(raw);
        }
        return super.getInt(path);
    }

    @Override
    public int getInt(String path, int def) {
        String raw = super.getString(path);
        if (raw != null && raw.contains("%")) {
            ExecutionContext ctx = ExecutionContext.current();
            if (ctx != null) return ctx.resolveInt(raw);
        }
        return super.getInt(path, def);
    }

    @Override
    public double getDouble(String path) {
        String raw = super.getString(path);
        if (raw != null && raw.contains("%")) {
            ExecutionContext ctx = ExecutionContext.current();
            if (ctx != null) return ctx.resolveDouble(raw);
        }
        return super.getDouble(path);
    }

    @Override
    public double getDouble(String path, double def) {
        String raw = super.getString(path);
        if (raw != null && raw.contains("%")) {
            ExecutionContext ctx = ExecutionContext.current();
            if (ctx != null) return ctx.resolveDouble(raw);
        }
        return super.getDouble(path, def);
    }

    @Override
    public long getLong(String path) {
        String raw = super.getString(path);
        if (raw != null && raw.contains("%")) {
            ExecutionContext ctx = ExecutionContext.current();
            if (ctx != null) return ctx.resolveLong(raw);
        }
        return super.getLong(path);
    }

    @Override
    public long getLong(String path, long def) {
        String raw = super.getString(path);
        if (raw != null && raw.contains("%")) {
            ExecutionContext ctx = ExecutionContext.current();
            if (ctx != null) return ctx.resolveLong(raw);
        }
        return super.getLong(path, def);
    }

    @Override
    public boolean getBoolean(String path) {
        String raw = super.getString(path);
        if (raw != null && raw.contains("%")) {
            ExecutionContext ctx = ExecutionContext.current();
            if (ctx != null) return ctx.resolveBoolean(raw);
        }
        return super.getBoolean(path);
    }

    @Override
    public boolean getBoolean(String path, boolean def) {
        String raw = super.getString(path);
        if (raw != null && raw.contains("%")) {
            ExecutionContext ctx = ExecutionContext.current();
            if (ctx != null) return ctx.resolveBoolean(raw);
        }
        return super.getBoolean(path, def);
    }

    // ------------------------------------------------------------------
    // Remaining QConfig methods
    // ------------------------------------------------------------------

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
    public Set<QAction> getActions(QComponent component, String path) {
        if (!contains(path)) {
            return Set.of();
        }
        Set<QAction> result = (Set<QAction>) QConfigLoader.load(component, path, this, QRegistries.ACTIONS, source);
        return result != null ? result : Set.of();
    }

    @Override
    public Set<QCondition> getConditions(QComponent component, String path) {
        if (!contains(path)) {
            return Set.of();
        }
        Set<QCondition> result = (Set<QCondition>) QConfigLoader.load(component, path, this, QRegistries.CONDITIONS, source);
        return result != null ? result : Set.of();
    }

    @Override
    public Set<QObjective> getObjectives(QComponent component, String path) {
        if (!contains(path)) {
            return Set.of();
        }
        Set<QObjective> result = (Set<QObjective>) QConfigLoader.load(component, path, this, QRegistries.OBJECTIVES, source);
        return result != null ? result : Set.of();
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public QEvent getQEvent(String event) {
        return QuestsXL.get().getEventManager().getByID(event);
    }

    @Override
    public QQuest getQuest(String quest) {
        return QuestsXL.get().getQuestManager().getByName(quest);
    }
}
