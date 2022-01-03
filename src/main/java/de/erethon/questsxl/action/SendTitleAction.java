package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class SendTitleAction extends QBaseAction {

    String msg;
    String subtitle;
    int fadeIn;
    int stay;
    int fadeOut;

    @Override
    public void play(Player player) {
        if (!conditions(player)) return;
        player.showTitle(Title.title(MessageUtil.parse(msg), MessageUtil.parse(subtitle)));
        onFinish(player);
    }

    @Override
    public void load(String[] msg) {
        this.msg = msg[0];
        subtitle = msg[1];
        fadeOut = Integer.parseInt(msg[2]);
        stay = Integer.parseInt(msg[3]);
        fadeOut = Integer.parseInt(msg[4]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        msg = section.getString("title", "");
        msg = section.getString("subtitle", "");
        fadeIn = section.getInt("fadeIn", 1);
        stay = section.getInt("stay", 1);
        fadeOut = section.getInt("fadeOut", 1);
    }
}
