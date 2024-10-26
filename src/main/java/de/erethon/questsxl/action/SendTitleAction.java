package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;

public class SendTitleAction extends QBaseAction {

    String msg;
    String subtitle;
    int fadeIn;
    int stay;
    int fadeOut;

    @Override
    public void play(QPlayer player) {
        if (!conditions(player)) return;
        player.getPlayer().showTitle(Title.title(MessageUtil.parse(msg), MessageUtil.parse(subtitle)));
        onFinish(player);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        msg = cfg.getString("title", "");
        msg = cfg.getString("subtitle", "");
        fadeIn = cfg.getInt("fadeIn", 1);
        stay = cfg.getInt("stay", 1);
        fadeOut = cfg.getInt("fadeOut", 1);
    }
}
