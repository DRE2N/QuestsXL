package de.erethon.questsxl.scoreboard;

import de.erethon.aergia.player.EPlayer;
import de.erethon.aergia.scoreboard.ScoreboardLines;
import de.erethon.aergia.util.DynamicString;
import de.erethon.aergia.util.ScoreboardComponent;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Fyreum
 * pls fix
 */
public class QuestScoreboardLines implements ScoreboardLines {

    final QuestsXL plugin = QuestsXL.getInstance();

    @NotNull
    @Override
    public List<ScoreboardComponent> getLines(@NotNull EPlayer ePlayer) {
        QPlayer player = plugin.getPlayerCache().getByPlayer(ePlayer.getPlayer());
        List<ScoreboardComponent> lines = new ArrayList<>();

        return List.of();
    }

    @Override
    public int getPriority() {
        return 5;
    }
}
