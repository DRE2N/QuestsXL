package de.erethon.questsxl.action;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class DummyAction extends QBaseAction {
    @Override
    public void play(Player player) {
    }

    @Override
    public boolean conditions(Player player) {
        return true;
    }

    @Override
    public void onFinish(Player player) {
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
    public void load(String[] msg) {
    }

    @Override
    public void load(ConfigurationSection section) {
    }
}
