package de.erethon.questsxl.objective;

import de.erethon.bedrock.misc.EnumUtil;
import de.erethon.questsxl.common.QLineConfig;
import de.fyreum.jobsxl.job.ExperienceGainReason;
import de.fyreum.jobsxl.user.event.UserGainJobExperienceEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public class JobExpObjective extends QBaseObjective {

    int alreadyGained = 0;
    int amount;
    ExperienceGainReason reason;

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
            checkCompletion(active, this);
        }
    }

    @Override
    public void load(QLineConfig section) {
        amount = section.getInt("amount");
        if (amount <= 0) {
            throw new RuntimeException("The job exp objective in " + section + " contains a negative experience amount.");
        }
        String reasonString = section.getString("reason");
        if (reasonString == null) {
            return;
        }
        this.reason = EnumUtil.getEnumIgnoreCase(ExperienceGainReason.class, reasonString);
        if (this.reason == null) {
            throw new RuntimeException("The job exp objective in " + section + " contains an unknown experience gain reason.");
        }
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        amount = section.getInt("amount");
        if (amount <= 0) {
            throw new RuntimeException("The job exp objective in " + section.getName() + " contains a negative experience amount.");
        }
        String reasonString = section.getString("reason");
        if (reasonString == null) {
            return;
        }
        this.reason = EnumUtil.getEnumIgnoreCase(ExperienceGainReason.class, reasonString);
        if (this.reason == null) {
            throw new RuntimeException("The job exp objective in " + section.getName() + " contains an unknown experience gain reason.");
        }
    }
}
