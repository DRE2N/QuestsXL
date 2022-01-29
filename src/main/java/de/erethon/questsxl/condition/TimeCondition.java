package de.erethon.questsxl.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class TimeCondition extends QBaseCondition {

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH,mm");
    int minHour;
    int minMinute;
    int maxHour;
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
    public void load(ConfigurationSection section) {
        super.load(section);
        minHour = section.getInt("minHour");
        minMinute = section.getInt("minMinute");
        maxHour = section.getInt("maxHour");
        maxMinute = section.getInt("maxMinute");
        String timeZone = section.getString("timeZone", "ECT");
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
    }

    @Override
    public void load(String[] c) {
        minHour = NumberUtil.parseInt(c[0]);
        minMinute = NumberUtil.parseInt(c[1]);
        maxHour = NumberUtil.parseInt(c[2]);
        maxMinute = NumberUtil.parseInt(c[3]);
        String timeZone = c[4] != null ? c[4] : "ECT";
        dateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
    }
}
