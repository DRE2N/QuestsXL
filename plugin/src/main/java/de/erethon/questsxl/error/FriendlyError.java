package de.erethon.questsxl.error;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

/**
 * A friendly error message that can be displayed to the console and optionally to a player
 * Error messages are stored and can be retrieved by an admin or quest scripter.
 */
public class FriendlyError {

    String title;
    String location;
    String exception;
    String hint;
    String stacktrace;
    QPlayer relatedPlayer;

    public FriendlyError(String location, String title, String exception, String hint) {
        this.title = title;
        this.location = location;
        this.exception = exception;
        this.hint = hint;
        QuestsXL.log("[QXL] " + getMessage());
    }

    public String getMessage() {
        if (stacktrace != null && QuestsXL.get().isShowStacktraces()) {
            return "&8- &c" + location + "&8: &e<hover:show_text:'<red>" + exception + "\n<green>" + hint + "\n<dark_gray>" + stacktrace + "'>" + title + "</hover>";
        }
        return "&8- &c" + location + "&8: &e<hover:show_text:'<red>" + exception + "\n<green>" + hint + "'>" + title + "</hover>";
    }

    /**
     * Optionally, add a stacktrace to the error message
     * The stacktrace will be displayed when hovering over the error message
     * @param trace The stacktrace to add
     */
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

    /**
     * Optionally, add a player to the error message
     * The player will be notified with a friendly error message
     * @param player The player to notify
     */
    public FriendlyError addPlayer(QPlayer player) {
        relatedPlayer = player;
        // Add the time, so player screenshots can be matched with errors in the console, because players are dumb sometimes
        Date currentTime = Date.from(Instant.now());
        String formatted = new SimpleDateFormat("HH:mm:ss - dd.MM").format(currentTime);
        MessageUtil.sendMessage(player.getPlayer(), Component.translatable("qxl.error", Component.text(exception), Component.text(formatted)));
        return this;
    }
}
