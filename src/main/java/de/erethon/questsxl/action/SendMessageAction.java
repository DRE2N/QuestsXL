package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QLineConfig;
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
    public void load(QLineConfig cfg) {
        message = cfg.getString("message", cfg.toString());
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        message = section.getString("message", "MESSAGE_NOT_FOUND");
    }
}
