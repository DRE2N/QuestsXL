package de.erethon.questsxl.condition;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.fyreum.jobsxl.JobsXL;
import de.fyreum.jobsxl.job.PlayerJob;
import de.fyreum.jobsxl.user.User;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Fyreum
 */
@QLoadableDoc(
        value = "job_level",
        description = "Checks if a player has a certain level in a job. Requires JobsXL to be installed.",
        shortExample = "job_level: job=Miner; level=5",
        longExample = {
                "job_level:",
                "  job: Miner",
                "  level: 5"
        }
)
public class JobLevelCondition extends QBaseCondition {

    JobsXL jobsXL = QuestsXL.getInstance().getJobsXL();

    @QParamDoc(name = "job", description = "The name of the job that the player must have a certain level in.", required = true)
    String job;
    @QParamDoc(name = "level", description = "The level that the player must have in the specified job.", required = true)
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
    public void load(QConfig cfg) {
        super.load(cfg);
        job = cfg.getString("job");
        level = cfg.getInt("level");
    }

}
