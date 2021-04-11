package de.erethon.questsxl.condition;

import de.erethon.questsxl.players.QPlayer;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;

public class PermissionCondition extends QBaseCondition {

    String permission;

    @Override
    public boolean check(QPlayer player) {
        return player.getPlayer().hasPermission(permission);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
    }

}
