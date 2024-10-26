package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class SendMessageAction extends QBaseAction {

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
