package de.erethon.questsxl.component.condition;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.Map;

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
public class AttributeCondition extends QBaseCondition implements VariableProvider {

    @QParamDoc(description = "The ID of the attribute to check.", required = true)
    private Attribute attribute;
    @QParamDoc(description = "The minimum value the attribute has to be.", def = "0")
    private double minValue;
    @QParamDoc(description = "The maximum value the attribute has to be.", def = "4096")
    private double maxValue;

    private double lastValue = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        if (!(quester instanceof QPlayer player)) {
            return fail(quester);
        }
        Player p = player.getPlayer();
        if (p.getAttribute(attribute) == null) {
            return fail(quester);
        }
        lastValue = p.getAttribute(attribute).getValue();
        if (lastValue >= minValue && lastValue <= maxValue) {
            return success(quester);
        }
        return fail(quester);
    }

    /** Exposes %attribute_value% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("attribute_value", new QVariable(lastValue));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        attribute = Registry.ATTRIBUTE.get(new NamespacedKey("minecraft", cfg.getString("id").toLowerCase(Locale.ROOT)));
        if (attribute == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(cfg.getName(), "Invalid attribute: " + cfg.getString("id"), "Null attribute", "Make sure the attribute is spelled correctly."));
        }
        minValue = cfg.getDouble("min_value", 0);
        maxValue = cfg.getDouble("max_value", 4096);
    }
}
