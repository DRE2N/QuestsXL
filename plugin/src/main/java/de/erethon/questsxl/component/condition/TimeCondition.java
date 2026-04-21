package de.erethon.questsxl.component.condition;

import de.erethon.bedrock.misc.NumberUtil;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.script.QVariable;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.common.script.VariableProvider;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

@QLoadableDoc(
        value = "time",
        description = "Checks if the current time is between min and max time.",
        shortExample = "time: minHour=18; minMinute=0; maxHour=20; maxMinute=0 #18:00 - 20:00",
        longExample = {
                "time:",
                "  minHour: 18",
                "  minMinute: 0",
                "  maxHour: 20",
                "  maxMinute: 0",
                "  timeZone: ECT"
        }
)
public class TimeCondition extends QBaseCondition implements VariableProvider {

    SimpleDateFormat dateFormat = new SimpleDateFormat("HH,mm");

    @QParamDoc(name = "minHour", description = "The minimum hour.")
    int minHour;
    @QParamDoc(name = "minMinute", description = "The minimum minute.")
    int minMinute;
    @QParamDoc(name = "maxHour", description = "The maximum hour.")
    int maxHour;
    @QParamDoc(name = "maxMinute", description = "The maximum minute.")
    int maxMinute;

    private int lastHour = 0;
    private int lastMinute = 0;

    @Override
    public boolean checkInternal(Quester quester) {
        String[] formatted = dateFormat.format(new Date()).split(",");
        lastHour = NumberUtil.parseInt(formatted[0]);
        lastMinute = NumberUtil.parseInt(formatted[1]);
        if (lastHour > maxHour || lastHour < minHour) {
            return fail(quester);
        }
        if (lastHour == minHour && lastMinute < minMinute) {
            return fail(quester);
        }
        if (lastHour == maxHour && lastMinute > maxMinute) {
            return fail(quester);
        }
        return success(quester);
    }

    /** Exposes %time_hour% and %time_minute% to child actions (onSuccess / onFail). */
    @Override
    public Map<String, QVariable> provideVariables(Quester quester) {
        Map<String, QVariable> vars = new HashMap<>();
        vars.put("time_hour", new QVariable(lastHour));
        vars.put("time_minute", new QVariable(lastMinute));
        return vars;
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
