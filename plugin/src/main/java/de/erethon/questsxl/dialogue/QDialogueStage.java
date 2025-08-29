package de.erethon.questsxl.dialogue;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QComponent;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Fyreum
 */
public class QDialogueStage implements QComponent {

    private QComponent parent;
    protected QDialogue dialogue;
    protected LinkedList<Map.Entry<QTranslatable, Integer>> messages;
    protected int maxMessageCount;
    protected List<QCondition> conditions;
    protected List<QAction> actions;
    protected List<DialogueOption> dialogueOptions;
    protected boolean autoNext;
    protected int index;

    public QDialogueStage(QDialogue dialogue, ConfigurationSection section, int index) {
        this.dialogue = dialogue;
        this.load(section);
    }

    public QDialogueStage(QDialogueStage stage) {
        this.dialogue = stage.dialogue;
        this.messages = new LinkedList<>(stage.messages);
        this.maxMessageCount = stage.maxMessageCount;
        this.conditions = stage.conditions != null ? new ArrayList<>(stage.conditions) : new ArrayList<>();
        this.actions = stage.actions != null ? new ArrayList<>(stage.actions) : new ArrayList<>();
        this.dialogueOptions = stage.dialogueOptions != null ? new ArrayList<>(stage.dialogueOptions) : new ArrayList<>();
        this.autoNext = stage.autoNext;
        this.index = stage.index;
    }

    public boolean canStart(@NotNull QPlayer player) {
        for (QCondition condition : conditions) {
            if (!condition.check(player)) {
                player.send(condition.getDisplayText());
                return false;
            }
        }
        return true;
    }

    public QActiveDialogueStage start(@NotNull QPlayer player) {
        return new QActiveDialogueStage(player, this);
    }

    @Override
    public QComponent getParent() {
        return parent;
    }

    @Override
    public void setParent(QComponent parent) {
        this.parent = parent;
    }

    public void load(ConfigurationSection cfg) {
        if (cfg.getString("id") == null) {
            throw new RuntimeException("The dialogue stage in " + cfg.getName() + " does not have an id.");
        }
        messages = new LinkedList<>();
        List<String> messageList = cfg.getStringList("messages");
        maxMessageCount = messageList.size();
        for (String message : messageList) {
            // Look for delay at the end - format: "message|delay" or "message;delay"
            String messageText = message;
            int delay = 0;

            String[] splitSemicolon = message.split(";");
            String[] splitPipe = message.split("\\|");

            // Check if the last part is a number (delay)
            String lastPartSemicolon = splitSemicolon.length > 1 ? splitSemicolon[splitSemicolon.length - 1].trim() : null;
            String lastPartPipe = splitPipe.length > 1 ? splitPipe[splitPipe.length - 1].trim() : null;

            if (lastPartSemicolon != null && lastPartSemicolon.matches("\\d+")) {
                delay = NumberUtil.parseInt(lastPartSemicolon);
                messageText = String.join(";", java.util.Arrays.copyOf(splitSemicolon, splitSemicolon.length - 1));
            }
            else if (lastPartPipe != null && lastPartPipe.matches("\\d+")) {
                delay = NumberUtil.parseInt(lastPartPipe);
                messageText = String.join("|", java.util.Arrays.copyOf(splitPipe, splitPipe.length - 1));
            }

            messages.add(new AbstractMap.SimpleEntry<>(QTranslatable.fromString(messageText), delay));
        }
        if (messages.isEmpty()) {
            throw new RuntimeException("The dialogue stage in " + cfg.getName() + " does not have any messages.");
        }
        conditions = (List<QCondition>) QConfigLoader.load(this, "conditions", cfg, QRegistries.CONDITIONS);
        actions = (List<QAction>) QConfigLoader.load(this, "actions", cfg, QRegistries.ACTIONS);
        autoNext = cfg.getBoolean("autoNext", true);
        List<String> options = cfg.getStringList("options");
        dialogueOptions = new ArrayList<>();
        for (String option : options) {
            DialogueOption dialogueOption = new DialogueOption(this);
            try {
                dialogueOption.loadFromString(option);
                dialogueOptions.add(dialogueOption);
            } catch (Exception e) {
                QuestsXL.get().getErrors().add(new FriendlyError(cfg.getName(), "Failed to load dialogue option", e.getMessage(), "Path: " + cfg.getCurrentPath() + "." + option).addStacktrace(e.getStackTrace()));
            }
        }
    }

    // Validate and link stage references for all dialogue options after all stages have been loaded
    public void validateAndLinkOptions() {
        for (DialogueOption option : dialogueOptions) {
            option.validateAndLinkStages();
        }
    }
}
