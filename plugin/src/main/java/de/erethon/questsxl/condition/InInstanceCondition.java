package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.instancing.InstanceManager;
import de.erethon.questsxl.instancing.InstancedArea;
import de.erethon.questsxl.player.QPlayer;

@QLoadableDoc(
        value = "in_instance",
        description = "Checks if the player is currently in an instanced area. Optionally checks for a specific template.",
        shortExample = "in_instance:",
        longExample = {
                "in_instance:",
                "  template: dungeon_1  # Optional: check for specific instance template"
        }
)
public class InInstanceCondition extends QBaseCondition {

    private final InstanceManager instanceManager = QuestsXL.get().getInstanceManager();

    @QParamDoc(name = "template", description = "Optional: The specific template ID to check for")
    private String templateId;

    @Override
    public boolean check(Quester quester) {
        if (instanceManager == null) {
            return false;
        }

        if (!(quester instanceof QPlayer player)) {
            return false;
        }

        InstancedArea instance = instanceManager.getActiveInstance(player);
        if (instance == null) {
            return false;
        }

        // If no specific template is required, just check if in any instance
        if (templateId == null || templateId.isEmpty()) {
            return true;
        }

        // Check if in the specific template
        return instance.getTemplate().getId().equals(templateId);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        templateId = cfg.getString("template", null);
    }
}

