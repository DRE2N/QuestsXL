package de.erethon.questsxl.component.objective;

import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

@QLoadableDoc(
        value = "chat",
        description = "This objective is completed when the player sends a message.",
        shortExample = "chat:",
        longExample = {
                "chat:",
                "  message: 'Hello!'",
                "  exactMatch: true"
        }
)
public class ChatObjective extends QBaseObjective<AsyncChatEvent> implements VariableProvider {

    @QParamDoc(description = "The message that the player has to send.")
    private String message;
    @QParamDoc(description = "Whether the message has to be an exact match.", def = "false")
    private boolean exactMatch = false;

    private String lastChatMessage = "";

    @Override
    protected QTranslatable getDefaultDisplayText(Player player) {
        if (message != null) {
            if (exactMatch) {
                return QTranslatable.fromString("Say \"" + message + "\"");
            } else {
                return QTranslatable.fromString("Say something containing \"" + message + "\"");
            }
        }
        return QTranslatable.fromString("Send a message in chat");
    }

    @Override
    public void check(ActiveObjective active, AsyncChatEvent e) {
        String chatMessage = PlainTextComponentSerializer.plainText().serialize(e.message());
        if (message != null && exactMatch && !chatMessage.equals(message)) return;
        if (message != null && !exactMatch && !chatMessage.contains(message)) return;
        if (shouldCancelEvent) e.setCancelled(true);
        // Chat is async, so we need to run the rest on the main thread
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!conditions(e.getPlayer())) return;
            lastChatMessage = chatMessage;
            checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
        });
    }

    /** Exposes %chat_message% to child actions (onComplete). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        return Map.of("chat_message", new QVariable(lastChatMessage));
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        message = cfg.getString("message");
        exactMatch = cfg.getBoolean("exactMatch", false);
    }

    @Override
    public Class<AsyncChatEvent> getEventType() {
        return AsyncChatEvent.class;
    }
}
