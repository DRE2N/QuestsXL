package de.erethon.questsxl.dialogue;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.common.QConfigLoader;
import de.erethon.questsxl.common.QRegistries;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author Fyreum
 */
public class QDialogueStage {

    protected final LinkedList<Map.Entry<String, Integer>> messages;
    protected int maxMessageCount;
    protected final List<QCondition> conditions;
    protected final List<QAction> postActions;

    public QDialogueStage(@NotNull LinkedList<Map.Entry<String, Integer>> messages, @NotNull List<QCondition> conditions,
                          @NotNull List<QAction> postActions) {
        this.messages = messages;
        this.maxMessageCount = messages.size();
        this.conditions = conditions;
        this.postActions = postActions;
    }

    // copy constructor
    @Contract(pure = true)
    protected QDialogueStage(@NotNull QDialogueStage stage) {
        this(new LinkedList<>(stage.messages), new ArrayList<>(stage.conditions), new ArrayList<>(stage.postActions));
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

    public static QDialogueStage loadFromConfig(String id, ConfigurationSection section) {
        List<String> stringList = section.getStringList("messages");
        if (stringList.isEmpty()) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Nachrichten konnten nicht geladen werden.", "message list is empty", "Wahrscheinlich falsche Einrückung."));
            return null;
        }
        LinkedList<Map.Entry<String, Integer>> messages = new LinkedList<>();
        for (String string : stringList) {
            int lastSeparatorIndex = string.lastIndexOf(";");
            if (lastSeparatorIndex == -1) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Nachricht konnte nicht geladen werden.", "missing separator ';' to declare message delay", "Wahrscheinlich vergessen anzugeben."));
                continue;
            }
            String message = string.substring(0, lastSeparatorIndex);
            int messageDelay = NumberUtil.parseInt(string.substring(lastSeparatorIndex + 1), -1);
            if (messageDelay == -1) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Nachricht konnte nicht geladen werden.", "message delay has to be a number", "Wahrscheinlich vergessen anzugeben."));
                continue;
            }
            messages.add(new AbstractMap.SimpleEntry<>(message, messageDelay * 20)); // message delay in seconds
        }
        if (messages.isEmpty()) {
            QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Nachrichten konnten nicht geladen werden.", "not enough messages", "Siehe dir die vorherigen Fehlermeldungen an."));
            return null;
        }
        List<QCondition> conditions = new ArrayList<>();
        if (section.contains("conditions"))
            try {
                conditions.addAll((Collection<? extends QCondition>) QConfigLoader.load("conditions", section, QRegistries.CONDITIONS));
            } catch (Exception e) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "Bedingungen konnten nicht geladen werden.", e.getMessage(), "Wahrscheinlich falsche Einrückung."));
            }
        List<QAction> postActions = new ArrayList<>();
        if (section.contains("onFinish")) {
            try {
                postActions.addAll((Collection<? extends QAction>) QConfigLoader.load("onFinish", section, QRegistries.ACTIONS));
            } catch (Exception e) {
                QuestsXL.getInstance().getErrors().add(new FriendlyError(id, "End-Actions konnten nicht geladen werden.", e.getMessage(), "Wahrscheinlich falsche Einrückung."));
            }
        }
        return new QDialogueStage(messages, conditions, postActions);
    }
}
