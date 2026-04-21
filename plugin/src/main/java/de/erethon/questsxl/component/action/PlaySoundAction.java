package de.erethon.questsxl.component.action;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.script.ExecutionContext;
import de.erethon.questsxl.common.script.QConfig;
import de.erethon.questsxl.common.doc.QLoadableDoc;
import de.erethon.questsxl.common.doc.QParamDoc;
import de.erethon.questsxl.common.Quester;
import de.erethon.questsxl.error.FriendlyError;
import de.erethon.questsxl.player.QPlayer;

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
    @QParamDoc(name = "volume", description = "The volume of the sound — supports %variables%", def = "1.0")
    private String rawVolume = "1.0";
    @QParamDoc(name = "pitch", description = "The pitch of the sound — supports %variables%", def = "1.0")
    private String rawPitch = "1.0";

    public void playInternal(Quester quester) {
        if (!conditions(quester)) return;
        ExecutionContext ctx = ExecutionContext.current();
        float volume = (float) (ctx != null ? ctx.resolveDouble(rawVolume) : parseDouble(rawVolume));
        float pitch = (float) (ctx != null ? ctx.resolveDouble(rawPitch) : parseDouble(rawPitch));
        execute(quester, (QPlayer player) -> {
            player.getPlayer().playSound(player.getPlayer(), sound, volume, pitch);
        });
        onFinish(quester);
    }

    private static double parseDouble(String s) {
        try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 1.0; }
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        sound = cfg.getString("sound", null);
        rawVolume = cfg.getString("volume", "1.0");
        rawPitch = cfg.getString("pitch", "1.0");
        if (sound == null) {
            QuestsXL.get().addRuntimeError(new FriendlyError(id(), "Sound is missing"));
        }
    }
}