package de.erethon.questsxl.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.title.Title;
import org.bukkit.configuration.ConfigurationSection;

@QLoadableDoc(
        value = "title",
        description = "Sends a title to the player or all event participants, optionally with specified behaviour.",
        shortExample = "title: title=Hello, world!",
        longExample = {
                "title:",
                "  title: Hello, world!",
                "  subtitle: This is a subtitle",
                "  fadeIn: 1",
                "  stay: 5",
                "  fadeOut: 1"
        }
)
public class SendTitleAction extends QBaseAction {

    @QParamDoc(name = "title", description = "The title to send", required = true)
    String msg;
    @QParamDoc(name = "subtitle", description = "The subtitle to send", def = " ")
    String subtitle;
    @QParamDoc(name = "fadeIn", description = "The time in ticks the title takes to fade in", def = "10")
    int fadeIn;
    @QParamDoc(name = "stay", description = "The time in ticks the title stays on screen", def = "20")
    int stay;
    @QParamDoc(name = "fadeOut", description = "The time in ticks the title takes to fade out", def = "10")
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
        fadeIn = cfg.getInt("fadeIn", 10);
        stay = cfg.getInt("stay", 20);
        fadeOut = cfg.getInt("fadeOut", 10);
    }
}
