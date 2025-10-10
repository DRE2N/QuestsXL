package de.erethon.questsxl.dialogue;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.error.FriendlyError;
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
    private boolean isDefault = false;

    // Store the next stage index temporarily during loading
    private int nextStageIndex = -1;

    public DialogueOption(QDialogueStage stage) {
        this.dialogueStage = stage;
    }

    public void show(Player player) {
        String prefix = isDefault ? "<gray> <gold>></gold> <dark_gray>[</dark_gray><i>" : "<gray> - <dark_gray>[</dark_gray><i>";
        Component component = MiniMessage.miniMessage().deserialize(prefix)
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

        QuestsXL.log("Player " + player.getName() + " selected dialogue option: " + displayText.getAsString() + " Actions: " + actions.size() + " Next stage: " + (nextStage != null ? nextStageIndex : "none"));
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

    @Override
    public String id() {
        return displayText.toString();
    }

    public void load(QConfig cfg) {
        displayText = QTranslatable.fromString(cfg.getString("text", "<missing>"));
        String hint = cfg.getString("hint", null);
        hoverHint = hint != null ? QTranslatable.fromString(hint) : null;
        actions = cfg.getActions(this, "actions");
        isDefault = cfg.getBoolean("default", false);
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

        // Parse default flag
        String defaultStr = parsed.get("default");
        if (defaultStr != null) {
            isDefault = Boolean.parseBoolean(defaultStr);
        }

        // Parse actions if present
        String actionsStr = parsed.get("actions");
        if (actionsStr != null && !actionsStr.isEmpty()) {
            actions = parseActions(actionsStr);
        }

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

    /**
     * Parse actions from a string.
     * For a single action: "actionType: param1=value1;param2=value2"
     * For multiple actions: "actionType1: config1;;actionType2: config2"
     * The double semicolon (;;) is used to separate multiple actions.
     * Example: "command: command=stop;op=true;;message: message=Server stopping!"
     */
    private Set<QAction> parseActions(String actionsString) {
        Set<QAction> parsedActions = new HashSet<>();

        // Split by double semicolons to separate individual actions
        String[] actionStrings = actionsString.split(";;");

        for (String actionStr : actionStrings) {
            actionStr = actionStr.trim();
            if (actionStr.isEmpty()) {
                continue;
            }

            // Parse the action type (before the colon)
            int colonIndex = actionStr.indexOf(':');
            if (colonIndex <= 0) {
                QuestsXL.get().addRuntimeError(new FriendlyError("Dialogue: " + dialogueStage.dialogue.getName(),
                        "Invalid action format in dialogue option",
                        "Missing colon in action definition: " + actionStr,
                        "Ensure each action is defined as 'actionType: config'"));
                continue;
            }

            String actionType = actionStr.substring(0, colonIndex).trim();
            String actionConfig = actionStr.substring(colonIndex + 1).trim();

            // Check if the action type is valid
            if (!QRegistries.ACTIONS.isValid(actionType)) {
                QuestsXL.get().addRuntimeError(new FriendlyError("Dialogue: " + dialogueStage.dialogue.getName(),
                        "Unknown action type in dialogue option",
                        "Action type '" + actionType + "' is not registered",
                        "Available actions: " + String.join(", ", QRegistries.ACTIONS.getEntries().keySet())));
                continue;
            }

            try {
                // Create and load the action
                QAction action = QRegistries.ACTIONS.get(actionType).getClass().getDeclaredConstructor().newInstance();
                action.setParent(this);

                // Create a QLineConfig from the action configuration string
                QLineConfig lineConfig = new QLineConfig(actionConfig);
                action.load(lineConfig);

                parsedActions.add(action);
            } catch (Exception e) {
                QuestsXL.get().addRuntimeError(new FriendlyError("Dialogue: " + dialogueStage.dialogue.getName(),
                        "Failed to load action in dialogue option",
                        "Action type: " + actionType + ", Error: " + e.getMessage(),
                        "Config: " + actionConfig));
                e.printStackTrace();
            }
        }

        return parsedActions;
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

    public boolean isDefault() {
        return isDefault;
    }

    public void execute(Player player) {
        onCallback(player);
    }
}
