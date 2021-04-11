package de.erethon.questsxl.condition;

import org.bukkit.configuration.ConfigurationSection;

public abstract class QBaseCondition implements QCondition {

    String display = "";

    @Override
    public String getDisplayText() {
        return display;
    }

    @Override
    public void load(ConfigurationSection section) {
        if (section.getString("displayText") == null || section.getString("displayText").equals("none")) {
            display = null;
            return;
        }
        display = section.getString("displayText");
    }
    @Override
    public void load(String[] c) {

    }

}
