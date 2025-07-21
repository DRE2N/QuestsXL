package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;

public class DialogueOption implements QComponent {

    private final QDialogueStage dialogueStage;

    private String displayText;
    private String hoverHint;
    private Set<QAction> actions = new HashSet<>();
    private QDialogueStage nextStage;

    public DialogueOption(QDialogueStage stage) {
        this.dialogueStage = stage;
    }

    public void show(Player player) {
        Component component = MiniMessage.miniMessage().deserialize("<gray> - <dark_gray>[</dark_gray><i>" + displayText + "</i><dark_gray>]");
        if (hoverHint != null) {
            component = component.hoverEvent(MiniMessage.miniMessage().deserialize(hoverHint));
        }
        component = component.clickEvent(ClickEvent.callback(
                (clickEvent) -> {
                    onCallback(player);
                }
        ));
        player.sendMessage(component);
    }

    private void onCallback(Player player) {
        QPlayer qplayer = QPlayer.get(player);
        for (QAction action : actions) {
            action.play(qplayer);
        }
        if (nextStage != null) {
            nextStage.start(qplayer);
        }
    }

    @Override
    public QComponent getParent() {
        return dialogueStage;
    }

    @Override
    public void setParent(QComponent parent) {

    }

    public void load(QConfig cfg) {
        displayText = cfg.getString("text", "<missing>");
        hoverHint = cfg.getString("hint", null);
        actions = cfg.getActions(this, "actions");
        if (cfg.contains("next")) {
            int index = cfg.getInt("next");
            if (index < 0 || !dialogueStage.dialogue.getStages().containsKey(0)) {
                throw new RuntimeException("The next stage index is invalid: " + index);
            }
            nextStage = dialogueStage.dialogue.getStages().get(index);
        }


    }
}
