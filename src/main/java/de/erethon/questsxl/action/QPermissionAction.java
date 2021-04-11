package de.erethon.questsxl.action;

import com.google.gson.Gson;
import de.erethon.questsxl.QuestsXL;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
enum PermissionAction {
    ADD,
    REMOVE,
    ADD_GROUP,
    REMOVE_GROUP
}
public class QPermissionAction extends QBaseAction {

    private String permission;
    private transient RegisteredServiceProvider<Permission> rsp;
    private PermissionAction action;

    public QPermissionAction() { }
    public QPermissionAction(String id) {
        this.id = id;
    }
    public QPermissionAction(String permission, PermissionAction action) {
        this.permission = permission;
        this.action = action;
        rsp = QuestsXL.getInstance().getServer().getServicesManager().getRegistration(Permission.class);
    }

    @Override
    public void play(Player player) {
        if (!conditions(player)) return;
        Permission provider = rsp.getProvider();
        switch (action) {
            case ADD -> {
                provider.playerAdd(player, permission);
            }
            case REMOVE -> {
                provider.playerRemove(player, permission);
            }
            case ADD_GROUP -> {
                provider.playerAddGroup(player, permission);
            }
            case REMOVE_GROUP -> {
                provider.playerRemoveGroup(player, permission);
            }
        }
        onFinish(player);
    }

    @Override
    public Material getIcon() {
        return Material.COMMAND_BLOCK_MINECART;
    }

}
