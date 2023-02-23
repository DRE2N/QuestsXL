package de.erethon.questsxl.action;

import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.configuration.ConfigurationSection;

public class DummyAction extends QBaseAction {
    @Override
    public void play(QPlayer player) {
    }

    @Override
    public void play(QEvent event) {
    }

    @Override
    public boolean conditions(QPlayer player) {
        return true;
    }

    @Override
    public void onFinish(QPlayer player) {
    }

    @Override
    public void delayedEnd(int seconds) {
    }

    @Override
    public void cancel() {
    }

    @Override
    public String getID() {
        return "DUMMY";
    }

    @Override
    public void load(QLineConfig cfg) {
    }

    @Override
    public void load(ConfigurationSection section) {
    }
}
