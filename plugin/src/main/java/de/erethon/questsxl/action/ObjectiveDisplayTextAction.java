package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.Completable;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "objective_display",
        description = "Sets the display text of an objective for a player. Shown in the sidebar (right side of the screen).",
        shortExample = "objective_display: id=example_quest; text=Hello!",
        longExample = {
                "objective_display:",
                "  id: example_quest",
                "  text: Hello again!"
        }
)
public class ObjectiveDisplayTextAction extends QBaseAction {

    @QParamDoc(name = "id", description = "The ID of the quest or event to set the objective for. Defaults to the top parent if not specified.")
    private Completable completable;
    @QParamDoc(name = "text", description = "The text to display, empty to clear", def = " ")
    private String text;


    @Override
    public void play(Quester quester) {
        if (!conditions(quester)) return;
        if (completable == null && findTopParent() instanceof Completable c) {
            completable = c;
        }
        execute(quester, (QPlayer player) -> {
            if (completable instanceof QQuest quest) {
                player.getActiveQuest(quest).setObjectiveDisplayText(text);
            } else if (completable instanceof QEvent event) {
                event.setObjectiveDisplayText(text);
            }
        });
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        text = cfg.getString("text", "");
        completable = QuestsXL.getInstance().getQuestManager().getByName(cfg.getString("id"));
        if (completable == null) {
            completable = QuestsXL.getInstance().getEventManager().getByID(cfg.getString("id"));
        }
    }
}
