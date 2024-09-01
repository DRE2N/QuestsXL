package de.erethon.questsxl.listener;

import de.erethon.aether.events.CreatureDeathEvent;
import de.erethon.aether.events.CreatureInteractEvent;
import de.erethon.aether.events.InstancedCreatureDeathEvent;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.dialogue.ActiveDialogue;
import de.erethon.questsxl.dialogue.QDialogue;
import de.erethon.questsxl.dialogue.QDialogueManager;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.event.EventHandler;

public class AetherListener extends AbstractListener {

    QuestsXL plugin = QuestsXL.getInstance();
    QDialogueManager dialogueManager = plugin.getDialogueManager();

    @EventHandler
    public void onCreatureDeath(CreatureDeathEvent event) {
        checkObjectives(event.getKiller(), event);
    }

    @EventHandler
    public void onInstancedCreatureDeath(InstancedCreatureDeathEvent event) {
        checkObjectives(event.getKiller(), event);
    }

    @EventHandler
    public void onInteractCreature(CreatureInteractEvent event) {
        String dialogueId = dialogueManager.getNPCRegistry().get(event.getID());
        if (dialogueId == null) {
            return;
        }
        QPlayer player = cache.getByPlayer(event.getPlayer());
        ActiveDialogue activeDialogue = player.getActiveDialogue();
        if (activeDialogue != null) {
            if (!activeDialogue.getDialogue().getName().equals(dialogueId)) {
                return;
            }
            activeDialogue.continueDialogue();
            return;
        }
        QDialogue dialogue = dialogueManager.get(dialogueId);
        if (dialogue.canStart(player)) {
            dialogue.start(player);
        }
    }

}
