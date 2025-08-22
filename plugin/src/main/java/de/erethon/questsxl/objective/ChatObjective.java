package de.erethon.questsxl.objective;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.Event;

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
public class ChatObjective extends QBaseObjective<AsyncChatEvent> {

    @QParamDoc(description = "The message that the player has to send.")
    private String message;
    @QParamDoc(description = "Whether the message has to be an exact match.", def = "false")
    private boolean exactMatch = false;

    @Override
    public void check(ActiveObjective active, AsyncChatEvent e) {
        if (!conditions(e.getPlayer())) return;
        if (message != null && exactMatch && !PlainTextComponentSerializer.plainText().serialize(e.message()).equals(message)) return;
        if (message != null && !exactMatch && !PlainTextComponentSerializer.plainText().serialize(e.message()).contains(message)) return;
        if (shouldCancelEvent) e.setCancelled(true);
        checkCompletion(active, this, plugin.getDatabaseManager().getCurrentPlayer(e.getPlayer()));
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
