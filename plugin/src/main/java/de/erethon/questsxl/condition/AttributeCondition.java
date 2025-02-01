package de.erethon.questsxl.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Locale;

@QLoadableDoc(
        value = "attribute",
        description = "This condition is successful if the player's attribute value is within a certain range.",
        shortExample = "attribute: id=advantage_physical; min_value=10; max_value=999",
        longExample = {
                "attribute:",
                "  id: max_health",
                "  min_value: 10",
                "  max_value: 20"
        }
)
public class AttributeCondition extends QBaseCondition {

    @QParamDoc(description = "The ID of the attribute to check.", required = true)
    private Attribute attribute;
    @QParamDoc(description = "The minimum value the attribute has to be.", def = "0")
    private double minValue;
    @QParamDoc(description = "The maximum value the attribute has to be.", def = "4096")
    private double maxValue;

    @Override
    public boolean check(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        Player p = player.getPlayer();
        if (p.getAttribute(attribute) == null) {
            return fail(quester);
        }
        double value = p.getAttribute(attribute).getValue();
        if (value >= minValue && value <= maxValue) {
            return success(quester);
        }
        return fail(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        attribute = Registry.ATTRIBUTE.get(new NamespacedKey("minecraft", cfg.getString("id").toLowerCase(Locale.ROOT)));
        if (attribute == null) {
            QuestsXL.getInstance().addRuntimeError(new FriendlyError(cfg.getName(), "Invalid attribute: " + cfg.getString("id"), "Null attribute", "Make sure the attribute is spelled correctly."));
        }
        minValue = cfg.getDouble("min_value", 0);
        maxValue = cfg.getDouble("max_value", 4096);
    }
}
