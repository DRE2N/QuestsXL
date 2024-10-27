package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

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

    @QParamDoc(name = "message", description = "The message to send", required = true)
    private String message;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        MessageUtil.sendMessage(player.getPlayer(), message);
        onFinish(player);
    }

    @Override
    public void play(QEvent player) {
        if (!conditions(player)) return;
        for (QPlayer qPlayer : player.getPlayersInRange()) {
            MessageUtil.sendMessage(qPlayer.getPlayer(), message);
        }
        onFinish(player);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        message = cfg.getString("message", "MESSAGE_NOT_FOUND");
    }
}
