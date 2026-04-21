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

    private static final int HOVER_STACKTRACE_LINES = 9;
    private static final int CONSOLE_STACKTRACE_LINES = 3;

    String title;
    String location;
    String exception;
    String hint;
    String stacktrace;
    String consoleStackPreview;
    QPlayer relatedPlayer;

    public FriendlyError(String location, String title) {
        this(location, title, "", "");
    }


    public FriendlyError(String location, String title, String exception, String hint) {
        this.title = title;
        this.location = location;
        this.exception = exception;
        this.hint = hint;
        logToConsole();
    }

    private void logToConsole() {
        String path = location == null || location.isBlank() ? "unknown" : location;
        String error = exception == null || exception.isBlank() ? "-" : exception;
        String help = hint == null || hint.isBlank() ? "-" : hint;

        QuestsXL.log("[QXL] Error: " + title);
        QuestsXL.log("[QXL]   Path: " + path);
        QuestsXL.log("[QXL]   Error: " + error);
        QuestsXL.log("[QXL]   Hint: " + help);

        if (consoleStackPreview != null && !consoleStackPreview.isBlank() && QuestsXL.get().isShowStacktraces()) {
            QuestsXL.log("[QXL]   Stacktrace (first " + CONSOLE_STACKTRACE_LINES + " lines):");
            for (String line : consoleStackPreview.split("\\n")) {
                if (!line.isBlank()) {
                    QuestsXL.log("[QXL]     at " + line);
                }
            }
        }
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
        StringBuilder hoverMessage = new StringBuilder("<gray>");
        StringBuilder consoleMessage = new StringBuilder();
        int maxLines = Math.min(trace.length, Math.max(HOVER_STACKTRACE_LINES, CONSOLE_STACKTRACE_LINES));

        for (int i = 0; i < maxLines; i++) {
            if (i < HOVER_STACKTRACE_LINES) {
                hoverMessage.append(trace[i]).append("\n<gray>");
            }
            if (i < CONSOLE_STACKTRACE_LINES) {
                if (!consoleMessage.isEmpty()) {
                    consoleMessage.append("\n");
                }
                consoleMessage.append(trace[i]);
            }
        }

        stacktrace = hoverMessage.toString();
        consoleStackPreview = consoleMessage.toString();
        logToConsole();
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
