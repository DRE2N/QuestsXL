package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;

public class RemoveMobsAction extends QBaseAction {

    private String mob;
    private int radius;
    private boolean doDamage;

    @Override
    public void play(QPlayer player) {
        super.play(player);
    }

    @Override
    public void play(QEvent event) {
        super.play(event);
    }

    @Override
    public void load(QConfig cfg) {
        super.load(cfg);
        mob = cfg.getString("id");
        radius = cfg.getInt("radius", 32);
        doDamage = cfg.getBoolean("doDamage", false);
    }
}
