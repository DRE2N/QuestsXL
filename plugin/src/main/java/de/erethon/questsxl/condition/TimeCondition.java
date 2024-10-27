package de.erethon.questsxl.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

@QLoadableDoc(
        value = "time",
        description = "Checks if the current time is between min and max time.",
        shortExample = "'time: minHour=18; minMinute=0; maxHour=20; maxMinute=0' #18:00 - 20:00",
        longExample = {
                "time:",
                "  minHour: 18",
                "  minMinute: 0",
                "  maxHour: 20",
                "  maxMinute: 0",
                "  timeZone: ECT"
        }
)
public class TimeCondition extends QBaseCondition {

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH,mm");

    @QParamDoc(name = "minHour", description = "The minimum hour.")
    int minHour;
    @QParamDoc(name = "minMinute", description = "The minimum minute.")
    int minMinute;
    @QParamDoc(name = "maxHour", description = "The maximum hour.")
    int maxHour;
    @QParamDoc(name = "maxMinute", description = "The maximum minute.")
    int maxMinute;

    @Override
    public boolean check(QPlayer player) {
        String[] formatted = dateFormat.format(new Date()).split(",");
        int hour = NumberUtil.parseInt(formatted[0]);
        int minute = NumberUtil.parseInt(formatted[1]);
        if (hour > maxHour || hour < minHour) {
            return fail(player);
        }
        if (hour == minHour && minute < minMinute) {
            return fail(player);
        }
        if (hour == maxHour && minute > maxMinute) {
            return fail(player);
        }
        return success(player);
    }

    @Override
    public boolean check(QEvent event) {
        String[] formatted = dateFormat.format(new Date()).split(",");
        int hour = NumberUtil.parseInt(formatted[0]);
        int minute = NumberUtil.parseInt(formatted[1]);
        if (hour > maxHour || hour < minHour) {
            return fail(event);
        }
        if (hour == minHour && minute < minMinute) {
            return fail(event);
        }
        if (hour == maxHour && minute > maxMinute) {
            return fail(event);
        }
        return success(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        minHour = cfg.getInt("minHour");
        minMinute = cfg.getInt("minMinute");
        maxHour = cfg.getInt("maxHour");
        maxMinute = cfg.getInt("maxMinute");
        String timeZone = cfg.getString("timeZone", "ECT");
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
    }

}
