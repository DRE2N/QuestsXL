package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.fyreum.jobsxl.job.ExperienceGainReason;
import de.fyreum.jobsxl.user.User;
import org.bukkit.configuration.ConfigurationSection;

import java.util.Random;

/**
 * @author Fyreum
 */
@QLoadableDoc(
        value = "job_exp",
        description = "Gives the player job experience.",
        shortExample = "- 'job_exp: min=1; max=10; chance=0.5'",
        longExample = {
                "job_exp:",
                "  min: 1",
                "  max: 10",
                "  chance: 0.5"
        }
)
public class JobExpAction extends QBaseAction {

    private Random random = new Random();
    @QParamDoc(name = "min", description = "The minimum amount of experience to give", required = true)
    private int min;
    @QParamDoc(name = "max", description = "The maximum amount of experience to give", required = true)
    private int max;
    @QParamDoc(name = "chance", description = "The chance of the action being executed. 1 is 100% chance, 0 is 0% chance", def="1")
    private double chance;

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
    public void load(QConfig cfg) {
        super.load(cfg);
        min = cfg.getInt("min");
        max = cfg.getInt("max");
        chance = Math.min(cfg.getDouble("chance", 1), 1);
    }

}
