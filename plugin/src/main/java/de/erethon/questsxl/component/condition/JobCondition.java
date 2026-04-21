package de.erethon.questsxl.component.condition;

import de.erethon.hecate.Hecate;
import de.erethon.hecate.data.HCharacter;
import de.erethon.hephaestus.jobs.JobDatabaseManager;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.entity.Player;

import java.util.Map;
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
public class JobCondition extends QBaseCondition implements VariableProvider {

    private JobDatabaseManager jobDB = QuestsXL.get().getJobDatabaseManager();
    private Hecate hecate = Hecate.getInstance();

    @QParamDoc(name = "job", description = "The ID of the job to check for.", required = true)
    private String jobID;

    private String lastJobId = "";

    @Override
    public boolean checkInternal(Quester quester) {
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
            if (currentJobID.isPresent()) {
                lastJobId = currentJobID.get();
                if (lastJobId.equalsIgnoreCase(jobID)) {
                    return success(quester);
                }
                return fail(quester);
            }
        }
        return fail(quester);
    }

    /** Exposes %job_id% (the player's actual current job) to child actions. */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("job_id", new QVariable(lastJobId));
    }
}
