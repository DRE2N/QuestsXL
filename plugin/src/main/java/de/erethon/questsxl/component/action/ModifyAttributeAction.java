package de.erethon.questsxl.component.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
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

    @QParamDoc(name = "id", description = "The ID of the attribute to modify.", required = true)
    private Attribute attribute;
    @QParamDoc(name = "amount", description = "The amount to modify the attribute by — supports %variables%", def = "0")
    private String rawAmount;
    @QParamDoc(name = "duration", description = "The duration in ticks the attribute should be modified for — supports %variables%", def = "0")
    private String rawDuration;

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        ExecutionContext ctx = ExecutionContext.current();
        double amount = ctx != null ? ctx.resolveDouble(rawAmount) : parseDouble(rawAmount);
        int duration = ctx != null ? ctx.resolveInt(rawDuration) : parseInt(rawDuration);
        execute(quester, (QPlayer qPlayer) -> {
            Player p = qPlayer.getPlayer();
            if (p.getAttribute(attribute) == null) {
                return;
            }
            NamespacedKey key = new NamespacedKey("qxl", UUID.randomUUID().toString().toLowerCase());
            AttributeModifier modifier = new AttributeModifier(key, amount, AttributeModifier.Operation.ADD_NUMBER);
            p.getAttribute(attribute).addTransientModifier(modifier);
            if (duration > 0) {
                QuestsXL.get().getServer().getScheduler().runTaskLater(QuestsXL.get(), () -> p.getAttribute(attribute).removeModifier(key), duration);
            }
        });
        onFinish(quester);
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
    }

    private static int parseInt(String s) {
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return 0; }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        attribute = Registry.ATTRIBUTE.get(new NamespacedKey("minecraft", cfg.getString("id").toLowerCase(Locale.ROOT)));
        if (attribute == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(cfg.getName(), "Invalid attribute: " + cfg.getString("id"), "Null attribute", "Make sure the attribute is spelled correctly."));
        }
        rawAmount = cfg.getString("amount", "0");
        rawDuration = cfg.getString("duration", "0");
    }
}
