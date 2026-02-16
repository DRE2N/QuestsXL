package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.instancing.InstanceManager;
import de.erethon.questsxl.instancing.InstanceTemplate;
import de.erethon.questsxl.instancing.InstancedArea;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

import java.util.HashSet;
import java.util.Set;

@QLoadableDoc(
        value = "enter_instance",
        description = "Enters the player(s) into an instanced area. The instance is created from a template and persisted per character.",
        shortExample = "enter_instance: template=dungeon_1",
        longExample = {
                "enter_instance:",
                "  template: dungeon_1",
                "  shared: true  # If true, event participants share the same instance"
        }
)
public class EnterInstanceAction extends QBaseAction {

    private final InstanceManager instanceManager = QuestsXL.get().getInstanceManager();

    @QParamDoc(name = "template", description = "The ID of the instance template to use", required = true)
    private String templateId;

    @QParamDoc(name = "shared", description = "Whether event participants share a single instance (default: false)")
    private boolean shared = false;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;

        if (instanceManager == null) {
            QuestsXL.log("Instance manager not initialized, cannot enter instance");
            onFinish(quester);
            return;
        }

        InstanceTemplate template = instanceManager.getTemplate(templateId);
        if (template == null) {
            QuestsXL.log("Instance template '" + templateId + "' not found");
            onFinish(quester);
            return;
        }

        if (quester instanceof QPlayer player) {
            instanceManager.enterInstance(player, templateId);
        } else if (quester instanceof QEvent event) {
            if (shared) {
                // All participants share the same instance
                Set<QPlayer> participants = new HashSet<>(event.getPlayersInRange());
                if (!participants.isEmpty()) {
                    instanceManager.enterInstance(participants, templateId);
                }
            } else {
                // Each participant gets their own instance
                for (QPlayer player : event.getPlayersInRange()) {
                    instanceManager.enterInstance(player, templateId);
                }
            }
        }

        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        templateId = cfg.getString("template");
        if (templateId == null) {
            throw new RuntimeException("enter_instance action requires 'template' parameter");
        }
        shared = cfg.getBoolean("shared", false);
    }
}

