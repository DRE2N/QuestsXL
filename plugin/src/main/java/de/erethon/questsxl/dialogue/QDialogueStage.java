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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Fyreum
 */
public class QDialogueStage implements QComponent {

    private QComponent parent;
    protected QDialogue dialogue;
    protected LinkedList<Map.Entry<QTranslatable, Integer>> messages;
    protected int maxMessageCount;
    protected Set<QCondition> conditions = new HashSet<>();
    protected Set<QAction> actions;
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
        this.conditions = stage.conditions != null ? new HashSet<>(stage.conditions) : new HashSet<>();
        this.actions = stage.actions != null ? new HashSet<>(stage.actions) : new HashSet<>();
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
            // Look for delay at the end - format: "message|delay"
            // Note: Pipe separator is used because semicolons are reserved for translation strings (en=...;de=...)
            String messageText = message;
            int delay = 0;

            String[] splitPipe = message.split("\\|");

            // Check if the last part is a number (delay)
            if (splitPipe.length > 1) {
                String lastPart = splitPipe[splitPipe.length - 1].trim();
                if (lastPart.matches("\\d+")) {
                    delay = NumberUtil.parseInt(lastPart);
                    messageText = String.join("|", java.util.Arrays.copyOf(splitPipe, splitPipe.length - 1));
                }
            }

            messages.add(new AbstractMap.SimpleEntry<>(QTranslatable.fromString(messageText), delay));
        }
        if (messages.isEmpty()) {
            throw new RuntimeException("The dialogue stage in " + cfg.getName() + " does not have any messages.");
        }
        conditions = new HashSet<>();
        if (cfg.contains("conditions")) {
            conditions.addAll((Collection<? extends QCondition>) QConfigLoader.load(this, "conditions", cfg, QRegistries.CONDITIONS));
            for (QCondition condition : conditions) {
                condition.setParent(this);
            }
        }
        actions = new HashSet<>();
        if  (cfg.contains("actions")) {
            actions.addAll((Collection<? extends QAction>) QConfigLoader.load(this, "actions", cfg, QRegistries.ACTIONS));
            for (QAction action : actions) {
                action.setParent(this);
            }
        }
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

    @Override
    public String id() {
        return "Dialog: " + dialogue.id() + " Stage " + index;
    }
}
