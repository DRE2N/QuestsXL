package de.erethon.questsxl.condition;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hephaestus.jobs.JobDatabaseManager;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;

import java.util.Optional;
import java.util.UUID;

@QLoadableDoc(
        value = "job",
        description = "Checks if the player has a specific job.",
        shortExample = "job: job=armorsmith",
        longExample = {
                "job:",
                "  job: armorsmith",
        }
)
public class JobCondition extends QBaseCondition {

    private JobDatabaseManager jobDB = QuestsXL.get().getJobDatabaseManager();
    private Hecate hecate = Hecate.getInstance();

    @QParamDoc(name = "job", description = "The ID of the job to check for.", required = true)
    private String jobID;

    @Override
    public boolean check(Quester quester) {
        if (jobDB == null || hecate == null) {
            QuestsXL.get().getLogger().warning("JobCondition used but Hephaestus is not installed.");
            return fail(quester);
        }
        if (quester instanceof QPlayer player) {
            Player bPlayer = player.getPlayer();
            HCharacter charData = hecate.getDatabaseManager().getCurrentCharacter(bPlayer);
            if (charData == null) {
                return fail(quester);
            }
            UUID uuid = charData.getCharacterID();
            Optional<String> currentJobID = jobDB.getCharacterJobId(uuid).join();
            if  (currentJobID.isPresent()) {
                String currentJob = currentJobID.get();
                if (currentJob.equalsIgnoreCase(jobID)) {
                    success(quester);
                }
                return fail(quester);
            }
        }
        return fail(quester);
    }


}
