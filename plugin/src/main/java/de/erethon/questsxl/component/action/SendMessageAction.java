package de.erethon.questsxl.component.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QTranslatable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

import java.util.Map;

@QLoadableDoc(
        value = "message",
        description = "Sends a message to the player or all event participants.",
        shortExample = "message: message=Hello, world!",
        longExample = {
                "message:",
                "  message: Hello, world!"
        }
)
public class SendMessageAction extends QBaseAction {

    @QParamDoc(name = "message", description = "The message to send — supports %variables%", required = true)
    private String rawMessage;

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        if (quester instanceof QEvent event) {
            Map<String, QVariable> snapshot = ExecutionContext.snapshotVariables();
            for (QPlayer player : event.getPlayersInRange()) {
                boolean pushed = ExecutionContext.push(player, this);
                try {
                    if (pushed && !snapshot.isEmpty()) {
                        ExecutionContext.publishVariables(snapshot);
                    }
                    ExecutionContext ctx = ExecutionContext.current();
                    String resolved = ctx != null ? ctx.resolveString(rawMessage) : rawMessage;
                    QTranslatable message = QTranslatable.fromString(resolved);
                    MessageUtil.sendMessage(player.getPlayer(), message.get());
                } finally {
                    if (pushed) {
                        ExecutionContext.pop();
                    }
                }
            }
            onFinish(quester);
            return;
        }
        ExecutionContext ctx = ExecutionContext.current();
        String resolved = ctx != null ? ctx.resolveString(rawMessage) : rawMessage;
        QTranslatable message = QTranslatable.fromString(resolved);
        execute(quester, (QPlayer player) -> MessageUtil.sendMessage(player.getPlayer(), message.get()));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        rawMessage = cfg.getString("message");
    }
}
