package de.erethon.questsxl.error;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;

public class FriendlyError {

    String title;
    String location;
    String exception;
    String hint;
    String stacktrace;

    public FriendlyError(String location, String title, String exception, String hint) {
        this.title = title;
        this.location = location;
        this.exception = exception;
        this.hint = hint;
        MessageUtil.log("[QXL] " + getMessage());
    }

    public String getMessage() {
        if (stacktrace != null && QuestsXL.getInstance().isShowStacktraces()) {
            return "&8- &c" + location + "&8: &e<hover:show_text:'<red>" + exception + "\n<green>" + hint + "\n<dark_gray>" + stacktrace + "'>" + title + "</hover>";
        }
        return "&8- &c" + location + "&8: &e<hover:show_text:'<red>" + exception + "\n<green>" + hint + "'>" + title + "</hover>";
    }

    public FriendlyError addStacktrace(StackTraceElement[] trace) {
        String message = "<gray>";
        int line = 0;
        for (StackTraceElement element : trace) {
            if (line > 8) {
                break;
            }
            message = message + element.toString() + "\n<gray>";
            line++;
        }
        stacktrace = message;
        return this;
    }
}
