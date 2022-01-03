package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SendMessage extends QBaseAction {

    private String message;

    @Override
    public void play(Player player) {
        if (!conditions(player)) return;
        MessageUtil.sendMessage(player, message);
        onFinish(player);
    }

    @Override
    public void load(String[] msg) {
        message = msg[0];
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        message = section.getString("message");
    }
}
