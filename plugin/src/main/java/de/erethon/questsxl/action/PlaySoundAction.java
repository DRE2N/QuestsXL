package de.erethon.questsxl.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Location;
import org.bukkit.Material;

@QLoadableDoc(
        value = "play_sound",
        description = "Plays a sound to the player.",
        shortExample = "play_sound: sound=entity.player.levelup",
        longExample = {
                "play_sound:",
                "  sound: entity.player.levelup",
                "  volume: 1.0",
                "  pitch: 1.0",
        }
)
public class PlaySoundAction extends QBaseAction {

    @QParamDoc(name = "sound", description = "The sound to play", required = true)
    private String sound;
    @QParamDoc(name = "volume", description = "The volume of the sound", def = "1.0")
    private double volume = 1.0;
    @QParamDoc(name = "pitch", description = "The pitch of the sound", def = "1.0")
    private double pitch = 1.0;

    public void play(Quester quester) {
        if (!conditions(quester)) return;
        execute(quester, (QPlayer player) -> {
            player.getPlayer().playSound(player.getPlayer(), sound, (float) volume, (float) pitch);
        });
        onFinish(quester);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        sound = cfg.getString("sound", null);
        volume = cfg.getDouble("volume", 1.0);
        pitch = cfg.getDouble("pitch", 1.0);
        if (sound == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(id(), "Sound is missing"));
        }
    }
}