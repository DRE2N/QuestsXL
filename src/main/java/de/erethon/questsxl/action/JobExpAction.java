package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.fyreum.jobsxl.job.ExperienceGainReason;
import de.fyreum.jobsxl.user.User;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

/**
 * @author Fyreum
 */
public class JobExpAction extends QBaseAction {

    Random random = new Random();
    int min;
    int max;
    double chance;

    @Override
    public void play(QPlayer player) {
        if (chance != 1 && Math.random() > chance) {
            return;
        }
        User user = plugin.getJobsXL().getUserCache().getByPlayer(player.getPlayer());
        if (user == null) {
            MessageUtil.log("Job exp actions doesn't work while JobsXL is not on the server");
            return;
        }
        int exp = min == max ? min : random.nextInt(max - min + 1) + min;
        user.addExp(exp, ExperienceGainReason.QUEST);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        min = section.getInt("min");
        max = section.getInt("max");
        chance = Math.min(section.getDouble("chance", 1), 1);
    }

    @Override
    public void load(QLineConfig cfg) {
        min = cfg.getInt("min");
        max = cfg.getInt("max");
        chance = Math.min(cfg.getDouble("chance", 1), 1);

    }
}
