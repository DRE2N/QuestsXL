package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.fyreum.jobsxl.JobsXL;
import de.fyreum.jobsxl.job.PlayerJob;
import de.fyreum.jobsxl.user.User;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Fyreum
 */
public class JobLevelCondition extends QBaseCondition {

    JobsXL jobsXL = QuestsXL.getInstance().getJobsXL();
    String job;
    int level;

    @Override
    public boolean check(QPlayer player) {
        if (jobsXL == null) {
            MessageUtil.log("Job level conditions doesn't work while JobsXL is not on the server");
            return success(player);
        }
        User user = jobsXL.getUserCache().getByPlayer(player.getPlayer());
        PlayerJob playerJob = user.getData().getJob(job);
        if (playerJob != null && playerJob.getLevel() >= level) {
            return success(player);
        }
        return fail(player);
    }

    @Override
    public boolean check(QEvent event) {
        return fail(event);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        job = section.getString("job");
        level = section.getInt("level");
    }

    @Override
    public void load(QLineConfig section) {
        job = section.getString("job");
        level = section.getInt("level");
    }
}
