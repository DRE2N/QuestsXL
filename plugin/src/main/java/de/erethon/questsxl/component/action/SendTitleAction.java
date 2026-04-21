package de.erethon.questsxl.component.action;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.title.Title;

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

    @QParamDoc(name = "title", description = "The title to send — supports %variables%", required = true)
    String rawTitle;
    @QParamDoc(name = "subtitle", description = "The subtitle to send — supports %variables%", def = " ")
    String rawSubtitle;
    @QParamDoc(name = "fadeIn", description = "The time in ticks the title takes to fade in", def = "10")
    int fadeIn;
    @QParamDoc(name = "stay", description = "The time in ticks the title stays on screen", def = "20")
    int stay;
    @QParamDoc(name = "fadeOut", description = "The time in ticks the title takes to fade out", def = "10")
    int fadeOut;

    @Override
    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        ExecutionContext ctx = ExecutionContext.current();
        String title = ctx != null ? ctx.resolveString(rawTitle) : rawTitle;
        String subtitle = ctx != null ? ctx.resolveString(rawSubtitle) : rawSubtitle;
        execute(quester, (QPlayer player) -> player.getPlayer().showTitle(Title.title(MessageUtil.parse(title), MessageUtil.parse(subtitle))));
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        rawTitle = cfg.getString("title", "");
        rawSubtitle = cfg.getString("subtitle", "");
        fadeIn = cfg.getInt("fadeIn", 10);
        stay = cfg.getInt("stay", 20);
        fadeOut = cfg.getInt("fadeOut", 10);
    }
}
