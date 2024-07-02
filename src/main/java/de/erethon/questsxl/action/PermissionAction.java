package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

enum EPermissionAction {
    ADD,
    REMOVE,
    ADD_GROUP,
    REMOVE_GROUP
}

public class PermissionAction extends QBaseAction {

    private String permission;
    private transient RegisteredServiceProvider<Permission> rsp;
    private EPermissionAction action;

    public PermissionAction() { }

    public PermissionAction(String id) {
        this.id = id;
    }

    public PermissionAction(String permission, EPermissionAction action) {
        this.permission = permission;
        this.action = action;
        rsp = QuestsXL.getInstance().getServer().getServicesManager().getRegistration(Permission.class);
    }

    @Override
    public void play(QPlayer qplayer) {
        if (!conditions(qplayer)) return;
        Permission provider = rsp.getProvider();
        Player player = qplayer.getPlayer();
        switch (action) {
            case ADD -> provider.playerAdd(player, permission);
            case REMOVE -> provider.playerRemove(player, permission);
            case ADD_GROUP -> provider.playerAddGroup(player, permission);
            case REMOVE_GROUP -> provider.playerRemoveGroup(player, permission);
        }
        onFinish(qplayer);
    }

    @Override
    public Material getIcon() {
        return Material.COMMAND_BLOCK_MINECART;
    }

}
