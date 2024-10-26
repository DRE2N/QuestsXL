package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.player.QPlayer;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Locale;

enum EPermissionAction {
    ADD,
    REMOVE,
    ADD_GROUP,
    REMOVE_GROUP
}

@QLoadableDoc(
        value = "permission",
        description = "Adds or removes a permission or group from a player.",
        shortExample = "- 'permission: permission=example_permission; action=ADD'",
        longExample = {
                "permission:",
                "  permission: example_group # Yes, its called permission, but it can also be a group",
                "  action: add_group"
        }
)
public class PermissionAction extends QBaseAction {

    @QParamDoc(name = "permission", description = "The permission or group to add or remove", required = true)
    private String permission;
    @QParamDoc(name = "action", description = "The action to perform. Can be `add`, `remove`, `add_group` or `remove_group`", def = "`add`")
    private EPermissionAction action;

    private transient RegisteredServiceProvider<Permission> rsp;

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

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        permission = cfg.getString("permission");
        action = EPermissionAction.valueOf(cfg.getString("action", "ADD").toUpperCase(Locale.ROOT));
    }
}
