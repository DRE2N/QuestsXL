package de.erethon.questsxl.livingworld;

import com.google.gson.JsonObject;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of a player's progress in the exploration of the world.
 */
public class PlayerExplorer {

    private final QuestsXL plugin = QuestsXL.getInstance();
    private final Exploration exploration = plugin.getExploration();

    private QPlayer qPlayer;
    private Map<ExplorationSet, Set<CompletedExplorable>> completedExplorables = new HashMap<>();
    private ExplorationSet currentClosestSet;

    public PlayerExplorer(QPlayer qPlayer) {
        this.qPlayer = qPlayer;
    }

    /**
    * Marks an explorable as completed by the player.
    * @param set The exploration set the explorable belongs to
    * @param explorable The explorable that was completed
    * @return true if the explorable was marked as completed, false if it was already completed
     */
    public boolean completeExplorable(ExplorationSet set, Explorable explorable, long timestamp) {
        Set<CompletedExplorable> explorableSet = completedExplorables.computeIfAbsent(set, k -> new HashSet<>());
        if (explorableSet.stream().anyMatch(e -> e.explorable().equals(explorable))) {
            return false;
        }
        CompletedExplorable completed = new CompletedExplorable(set, explorable, timestamp);
        explorableSet.add(completed);
        qPlayer.sendMessage(Component.translatable("qxl.explorable.discovered", explorable.displayName().get()));
        set.checkCompletion(qPlayer); // Check if the set is now fully completed
        return true;
    }

    public @Nullable ExplorationSet getSet(String id) {
        for (ExplorationSet set : completedExplorables.keySet()) {
            if (set.id().equals(id)) {
                return set;
            }
        }
        return null;
    }

    /**
     * Displays the progress of the player in the exploration set in
     * a nice format in the player's chat.
     * @param set The exploration set to display progress for
     */
    public void displayProgressForSet(ExplorationSet set) {
        Set<CompletedExplorable> list = completedExplorables.get(set);
        if (list == null) {
            return;
        }
        MiniMessage mm = MiniMessage.miniMessage();
        Component title = mm.deserialize("<strikethrough>     </strikethrough> <gradient:blue:purple> " + set.displayName().get() + " </gradient> <strikethrough>     </strikethrough>");
        Component description = mm.deserialize("<gray><i> " + set.description().get() + " </gray>");
        qPlayer.sendMessage(title);
        qPlayer.sendMessage(description);
        int total = set.entries().size();
        int completed = (int) list.stream().filter(e -> set.entries().contains(e.explorable())).count();
        Component header = mm.deserialize("<gradient:blue:purple> " + completed + "/" + total + " </gradient>");
        qPlayer.sendMessage(header);
        for (Explorable explorable : set.entries()) {
            boolean done = list.stream().anyMatch(e -> e.explorable().equals(explorable));
            if (done) {
                qPlayer.sendMessage(mm.deserialize("<dark_gray> - <explorable>", Placeholder.component("explorable", explorable.displayName().get())));
            }
        }
    }

    public void displayWorldProgress() {

    }

    public Map<ExplorationSet, Set<CompletedExplorable>> getCompletedExplorables() {
        return completedExplorables;
    }

    /**
     * Updates the closest set to the player's location.
     * Caution: This method is expensive and should not be called too often.
     */
    public void updateClosestSet() {
        currentClosestSet = exploration.getClosestSet(qPlayer.getPlayer().getLocation());
    }


    /*
    JSON Serialization
    */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        completedExplorables.forEach((set, list) -> {
            JsonObject setJson = new JsonObject();
            list.forEach(e -> {
                setJson.add(e.explorable().id(), e.toJson());
            });
            json.add(set.id(), setJson);
        });
        return json;
    }

    public static PlayerExplorer fromJson(QPlayer qPlayer, JsonObject json) {
        PlayerExplorer explorer = new PlayerExplorer(qPlayer);
        json.entrySet().forEach(e -> {
            ExplorationSet set = explorer.getSet(e.getKey());
            if (set == null) {
                return;
            }
            JsonObject setJson = e.getValue().getAsJsonObject();
            setJson.entrySet().forEach(entry -> {
                Explorable explorable = set.getExplorable(entry.getKey());
                if (explorable == null) {
                    return;
                }
                long timestamp = entry.getValue().getAsLong();
                explorer.loadExplorable(set, explorable, timestamp);
            });
        });
        return explorer;
    }

    private void loadExplorable(ExplorationSet set, Explorable explorable, long timestamp) {
        Set<CompletedExplorable> explorableSet = completedExplorables.computeIfAbsent(set, k -> new HashSet<>());
        CompletedExplorable completed = new CompletedExplorable(set, explorable, timestamp);
        explorableSet.add(completed);
    }
}
