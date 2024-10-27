package de.erethon.questsxl.objective;

import de.erethon.bedrock.misc.EnumUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.fyreum.jobsxl.job.ExperienceGainReason;
import de.fyreum.jobsxl.user.event.UserGainJobExperienceEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

@QLoadableDoc(
        value = "job_exp",
        description = "The player has to gain a certain amount of job experience.",
        shortExample = "job_exp: amount=100",
        longExample = {
                "job_exp:",
                "  amount: 100",
                "  reason: mob"
        }
)
public class JobExpObjective extends QBaseObjective {

    @QParamDoc(name = "amount", description = "The amount of experience that needs to be gained.", def = "1")
    int amount;
    @QParamDoc(name = "reason", description = "The reason for the experience gain. One of `command`, `crafting`, `dungeon`, `item`, `mob`, `quest`, `unknown`", def = "`crafting`")
    ExperienceGainReason reason;

    int alreadyGained = 0;

    @Override
    public void check(ActiveObjective active, Event e) {
        if (!(e instanceof UserGainJobExperienceEvent event)) {
            return;
        }
        Player player = event.getUser().getPlayer();
        if (!conditions(player) || (reason != null && event.getReason() != reason)) {
            return;
        }
        alreadyGained += event.getAmount();
        if (alreadyGained >= amount) {
            checkCompletion(active, this, plugin.getPlayerCache().getByPlayer(player));
        }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        amount = cfg.getInt("amount", 1);
        if (amount <= 0) {
            throw new RuntimeException("The job exp objective in " + cfg.getName() + " contains a negative experience amount.");
        }
        String reasonString = cfg.getString("reason", "crafting");
        this.reason = EnumUtil.getEnumIgnoreCase(ExperienceGainReason.class, reasonString);
        if (this.reason == null) {
            throw new RuntimeException("The job exp objective in " + cfg.getName() + " contains an unknown experience gain reason.");
        }
    }
}
