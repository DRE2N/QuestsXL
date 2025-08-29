package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DialogueOption implements QComponent {

    private final QDialogueStage dialogueStage;

    private QTranslatable displayText;
    private QTranslatable hoverHint;
    private Set<QAction> actions = new HashSet<>();
    private QDialogueStage nextStage;

    // Store the next stage index temporarily during loading
    private int nextStageIndex = -1;

    public DialogueOption(QDialogueStage stage) {
        this.dialogueStage = stage;
    }

    public void show(Player player) {
        Component component = MiniMessage.miniMessage().deserialize("<gray> - <dark_gray>[</dark_gray><i>")
                .append(displayText.get())
                .append(MiniMessage.miniMessage().deserialize("<dark_gray>]"));
        if (hoverHint != null) {
            component = component.hoverEvent(hoverHint.get());
        }
        component = component.clickEvent(ClickEvent.callback(
                (clickEvent) -> {
                    onCallback(player);
                }
        ));
        QPlayer qplayer = QPlayer.get(player);
        qplayer.sendMarkedMessage(component);
    }

    private void onCallback(Player player) {
        QPlayer qplayer = QPlayer.get(player);
        ActiveDialogue activeDialogue = qplayer.getActiveDialogue();

        for (QAction action : actions) {
            action.play(qplayer);
        }

        // Handle stage transition
        if (nextStage != null && activeDialogue != null) {
            // Transition to the next stage within the current dialogue
            activeDialogue.transitionToStage(nextStageIndex);
        } else {
            // If no next stage specified, end the dialogue
            if (activeDialogue != null) {
                activeDialogue.finish();
            }
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
        displayText = QTranslatable.fromString(cfg.getString("text", "<missing>"));
        String hint = cfg.getString("hint", null);
        hoverHint = hint != null ? QTranslatable.fromString(hint) : null;
        actions = cfg.getActions(this, "actions");
        if (cfg.contains("next")) {
            int index = cfg.getInt("next");
            if (index < 0 || !dialogueStage.dialogue.getStages().containsKey(index)) {
                throw new RuntimeException("The next stage index is invalid: " + index);
            }
            nextStage = dialogueStage.dialogue.getStages().get(index);
        }
    }

    // Custom parser for dialogue options that preserves pipe separators
    public void loadFromString(String optionString) {
        // Parse the option string manually to handle pipe separators correctly
        Map<String, String> parsed = parseDialogueOption(optionString);

        displayText = QTranslatable.fromString(parsed.getOrDefault("text", "<missing>"));
        String hint = parsed.get("hint");
        hoverHint = hint != null ? QTranslatable.fromString(hint) : null;

        // Store the next stage index but don't validate it yet (will be done in post-load)
        String nextStr = parsed.get("next");
        if (nextStr != null) {
            try {
                nextStageIndex = Integer.parseInt(nextStr);
            } catch (NumberFormatException e) {
                throw new RuntimeException("Invalid next stage number: " + nextStr);
            }
        }
    }

    private Map<String, String> parseDialogueOption(String input) {
        Map<String, String> result = new HashMap<>();

        // Split by pipes, but be careful about translation strings that contain semicolons
        String[] parts = input.split("\\|");

        for (String part : parts) {
            String trimmed = part.trim();
            int equalsIndex = trimmed.indexOf('=');
            if (equalsIndex > 0) {
                String key = trimmed.substring(0, equalsIndex).trim();
                String value = trimmed.substring(equalsIndex + 1).trim();
                result.put(key, value);
            }
        }

        return result;
    }

    // Validate and link stage references after all stages have been loaded
    public void validateAndLinkStages() {
        if (nextStageIndex >= 0) {
            if (!dialogueStage.dialogue.getStages().containsKey(nextStageIndex)) {
                throw new RuntimeException("The next stage index is invalid: " + nextStageIndex);
            }
            nextStage = dialogueStage.dialogue.getStages().get(nextStageIndex);
        }
    }
}
