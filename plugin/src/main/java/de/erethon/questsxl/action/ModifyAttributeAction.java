package de.erethon.questsxl.action;

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
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.Locale;
import java.util.UUID;

@QLoadableDoc(
        value = "modify_attribute",
        description = "This action modifies a player's attribute.",
        shortExample = "modify_attribute: id=max_health; amount=200; duration=20",
        longExample = {
                "modify_attribute:",
                "  id: max_health",
                "  amount: 2",
                "  duration: 20"
        }
)
public class ModifyAttributeAction extends QBaseAction {

    @QParamDoc(description = "The ID of the attribute to modify.", required = true)
    private Attribute attribute;
    @QParamDoc(description = "The amount to modify the attribute by.", def = "0")
    private double amount;
    @QParamDoc(description = "The duration in ticks the attribute should be modified for", def = "0")
    private int duration;

    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, (QPlayer qPlayer) -> {
            Player p = qPlayer.getPlayer();
            if (p.getAttribute(attribute) == null) {
                return;
            }
            NamespacedKey key = new NamespacedKey("qxl", UUID.randomUUID().toString().toLowerCase());
            AttributeModifier modifier = new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER);
            p.getAttribute(attribute).addTransientModifier(modifier);
            if (duration > 0) {
                QuestsXL.getInstance().getServer().getScheduler().runTaskLater(QuestsXL.getInstance(), () -> p.getAttribute(attribute).removeModifier(key), duration);
            }
        });
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        attribute = Registry.ATTRIBUTE.get(new NamespacedKey("minecraft", cfg.getString("id").toLowerCase(Locale.ROOT)));
        if (attribute == null) {
            QuestsXL.getInstance().addRuntimeError(new FriendlyError(cfg.getName(), "Invalid attribute: " + cfg.getString("id"), "Null attribute", "Make sure the attribute is spelled correctly."));
        }
        amount = cfg.getDouble("amount", 0);
        duration = cfg.getInt("duration", 0);
    }
}
