package de.erethon.questsxl.livingworld;

import com.google.gson.JsonObject;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.livingworld.explorables.ExplorableRespawnPoint;
import de.erethon.questsxl.livingworld.explorables.PointOfInterest;
import de.erethon.questsxl.respawn.RespawnPoint;
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

    private final QuestsXL plugin = QuestsXL.get();
    private final Exploration exploration = plugin.getExploration();

    private QPlayer qPlayer;
    private Map<ExplorationSet, Set<CompletedExplorable>> completedExplorables = new HashMap<>();
    private Set<String> unlockedStandaloneRespawnPoints = new HashSet<>(); // For standalone respawn points
    private String lastRespawnPointId; // Store the ID of the last respawn point used
    private String nearestRespawnPointId; // Store the ID of the nearest respawn point
    private ExplorationSet currentClosestSet;
    private ContentGuide contentGuide;

    public PlayerExplorer(QPlayer qPlayer) {
        this.qPlayer = qPlayer;
        contentGuide = new ContentGuide(qPlayer, this);
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

        // Send unlock message based on explorable type
        if (explorable instanceof ExplorableRespawnPoint respawnPoint) {
            qPlayer.sendMessage(Component.translatable("qxl.explorable.respawn.unlocked",
                respawnPoint.displayName().get()));
        } else if (explorable instanceof PointOfInterest poi) {
            qPlayer.sendMessage(Component.translatable("qxl.explorable.poi.discovered",
                poi.displayName().get()));
        } else {
            qPlayer.sendMessage(Component.translatable("qxl.explorable.discovered",
                explorable.displayName().get()));
        }

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

    public boolean hasCompletedSet(ExplorationSet set) {
        return completedExplorables.containsKey(set);
    }

    public boolean hasExplored(Explorable explorable) {
        for (Set<CompletedExplorable> explorableSet : completedExplorables.values()) {
            if (explorableSet.stream().anyMatch(e -> e.explorable().equals(explorable))) {
                return true;
            }
        }
        return false;
    }

    public Set<Explorable> getExploredInSet(ExplorationSet set) {
        Set<CompletedExplorable> explorableSet = completedExplorables.get(set);
        if (explorableSet == null) {
            return new HashSet<>();
        }
        Set<Explorable> explored = new HashSet<>();
        for (CompletedExplorable completed : explorableSet) {
            explored.add(completed.explorable());
        }
        return explored;
    }

    /**
     * Updates the closest set to the player's location.
     * Caution: This method is expensive and should not be called too often.
     */
    public void updateClosestSet() {
        currentClosestSet = exploration.getClosestSet(qPlayer.getPlayer().getLocation());
    }

    public ExplorationSet getCurrentClosestSet() {
        return currentClosestSet;
    }

    public ContentGuide getContentGuide() {
        return contentGuide;
    }

    /**
     * Checks if the player is near any unexplored explorables and automatically discovers them.
     * This method should be called when the player moves to check for proximity-based discoveries.
     */
    public void checkProximityDiscoveries() {
        // Check respawn points with NEAR unlock mode
        for (ExplorableRespawnPoint respawnPoint : plugin.getExploration().getExplorableRespawnPoints()) {
            if (respawnPoint.getRespawnPoint().getUnlockMode() != de.erethon.questsxl.respawn.RespawnPointUnlockMode.NEAR) {
                continue;
            }

            if (!respawnPoint.isVisibleTo(qPlayer)) {
                continue;
            }

            // Check if respawn point is already unlocked
            boolean alreadyUnlocked;
            ExplorationSet set = findSetForExplorable(respawnPoint);
            if (set != null) {
                alreadyUnlocked = hasExplored(respawnPoint);
            } else {
                alreadyUnlocked = isStandaloneRespawnPointUnlocked(respawnPoint.getRespawnPoint().getId());
            }

            if (alreadyUnlocked) {
                continue;
            }

            double distance = qPlayer.getPlayer().getLocation().distance(respawnPoint.location());
            if (distance <= 3.0) {
                if (set != null) {
                    // Respawn point is part of an exploration set
                    if (completeExplorable(set, respawnPoint, System.currentTimeMillis())) {
                        qPlayer.getPlayer().playSound(respawnPoint.location(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.66f, 1.5f);
                    }
                } else {
                    // Standalone respawn point - unlock it directly
                    if (unlockStandaloneRespawnPoint(respawnPoint.getRespawnPoint().getId())) {
                        qPlayer.getPlayer().playSound(respawnPoint.location(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.66f, 1.5f);
                    }
                }
            }
        }

        // Check points of interest
        for (ExplorationSet set : plugin.getExploration().getSets()) {
            for (Explorable explorable : set.entries()) {
                if (explorable instanceof de.erethon.questsxl.livingworld.explorables.PointOfInterest poi) {
                    if (!hasExplored(poi)) {
                        poi.attemptDiscovery(qPlayer);
                    }
                }
            }
        }
    }

    /**
     * Finds the exploration set that contains the given explorable.
     */
    private ExplorationSet findSetForExplorable(Explorable explorable) {
        for (ExplorationSet set : plugin.getExploration().getSets()) {
            if (set.entries().contains(explorable)) {
                return set;
            }
        }
        return null;
    }

    /**
     * Unlocks a standalone respawn point (not part of any exploration set).
     * @param respawnPointId The ID of the respawn point to unlock
     * @return true if the respawn point was unlocked, false if it was already unlocked
     */
    public boolean unlockStandaloneRespawnPoint(String respawnPointId) {
        if (unlockedStandaloneRespawnPoints.contains(respawnPointId)) {
            return false;
        }
        unlockedStandaloneRespawnPoints.add(respawnPointId);

        // Send unlock message
        RespawnPoint respawnPoint = plugin.getRespawnPointManager().getRespawnPoint(respawnPointId);
        if (respawnPoint != null) {
            qPlayer.sendMessage(Component.translatable("qxl.explorable.respawn.unlocked",
                respawnPoint.getDisplayName().get()));
        }

        // Save to database to ensure persistence across server restarts
        qPlayer.saveToDatabase();

        return true;
    }

    /**
     * Locks a standalone respawn point (removes it from unlocked list).
     * @param respawnPointId The ID of the respawn point to lock
     * @return true if the respawn point was locked, false if it wasn't unlocked
     */
    public boolean lockStandaloneRespawnPoint(String respawnPointId) {
        return unlockedStandaloneRespawnPoints.remove(respawnPointId);
    }

    /**
     * Checks if a standalone respawn point is unlocked.
     * @param respawnPointId The ID of the respawn point to check
     * @return true if the respawn point is unlocked, false otherwise
     */
    public boolean isStandaloneRespawnPointUnlocked(String respawnPointId) {
        return unlockedStandaloneRespawnPoints.contains(respawnPointId);
    }

    /**
     * Checks if a respawn point is unlocked (works for both exploration-based and standalone).
     * @param respawnPoint The respawn point to check
     * @return true if the respawn point is unlocked, false otherwise
     */
    public boolean isRespawnPointUnlocked(RespawnPoint respawnPoint) {
        // First check if it's an explorable respawn point (part of exploration system)
        ExplorableRespawnPoint explorable = plugin.getRespawnPointManager().getExplorableRespawnPoint(respawnPoint.getId());
        if (explorable != null) {
            // Check if it's been explored as part of an exploration set
            if (hasExplored(explorable)) {
                return true;
            }
            // If it's explorable but not explored via sets, check if it's unlocked as standalone
            // This handles NEAR mode respawn points that aren't part of exploration sets
            return isStandaloneRespawnPointUnlocked(respawnPoint.getId());
        }

        // Otherwise check if it's a standalone respawn point
        return isStandaloneRespawnPointUnlocked(respawnPoint.getId());
    }

    /**
     * Sets the last respawn point used by the player.
     * @param respawnPointId The ID of the respawn point
     */
    public void setLastRespawnPoint(String respawnPointId) {
        this.lastRespawnPointId = respawnPointId;
    }

    /**
     * Gets the last respawn point used by the player.
     * @return The RespawnPoint object, or null if none set or point doesn't exist
     */
    public RespawnPoint getLastRespawnPoint() {
        if (lastRespawnPointId == null) {
            return null;
        }
        return plugin.getRespawnPointManager().getRespawnPoint(lastRespawnPointId);
    }

    /**
     * Sets the nearest respawn point to the player.
     * @param respawnPointId The ID of the respawn point
     */
    public void setNearestRespawnPoint(String respawnPointId) {
        this.nearestRespawnPointId = respawnPointId;
    }

    /**
     * Gets the nearest respawn point to the player.
     * @return The RespawnPoint object, or null if none set or point doesn't exist
     */
    public RespawnPoint getNearestRespawnPoint() {
        if (nearestRespawnPointId == null) {
            return null;
        }
        return plugin.getRespawnPointManager().getRespawnPoint(nearestRespawnPointId);
    }

    /**
     * Updates the nearest respawn point based on the player's current location.
     * Should be called periodically to keep the nearest point up to date.
     */
    public void updateNearestRespawnPoint() {
        RespawnPoint nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (RespawnPoint respawnPoint : plugin.getRespawnPointManager().getRespawnPoints()) {
            if (!isRespawnPointUnlocked(respawnPoint)) {
                continue;
            }

            double distance = qPlayer.getPlayer().getLocation().distance(respawnPoint.getLocation());
            if (distance < nearestDistance) {
                nearest = respawnPoint;
                nearestDistance = distance;
            }
        }

        if (nearest != null) {
            setNearestRespawnPoint(nearest.getId());
        }
    }

    /*
    JSON Serialization
    */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();

        // Save completed explorables
        completedExplorables.forEach((set, list) -> {
            JsonObject setJson = new JsonObject();
            list.forEach(e -> {
                setJson.add(e.explorable().id(), e.toJson());
            });
            json.add(set.id(), setJson);
        });

        // Save standalone respawn points
        if (!unlockedStandaloneRespawnPoints.isEmpty()) {
            JsonObject standaloneJson = new JsonObject();
            for (String respawnPointId : unlockedStandaloneRespawnPoints) {
                standaloneJson.addProperty(respawnPointId, System.currentTimeMillis());
            }
            json.add("standaloneRespawnPoints", standaloneJson);
        }

        // Save last and nearest respawn point IDs
        if (lastRespawnPointId != null) {
            json.addProperty("lastRespawnPoint", lastRespawnPointId);
        }
        if (nearestRespawnPointId != null) {
            json.addProperty("nearestRespawnPoint", nearestRespawnPointId);
        }

        return json;
    }

    public static PlayerExplorer fromJson(QPlayer qPlayer, JsonObject json) {
        PlayerExplorer explorer = new PlayerExplorer(qPlayer);

        // Load completed explorables
        json.entrySet().forEach(e -> {
            String key = e.getKey();

            // Skip special keys
            if (key.equals("standaloneRespawnPoints") || key.equals("lastRespawnPoint") || key.equals("nearestRespawnPoint")) {
                return;
            }

            ExplorationSet set = explorer.exploration.getSet(key);
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

        // Load standalone respawn points
        if (json.has("standaloneRespawnPoints")) {
            JsonObject standaloneJson = json.getAsJsonObject("standaloneRespawnPoints");
            standaloneJson.entrySet().forEach(entry -> {
                explorer.unlockedStandaloneRespawnPoints.add(entry.getKey());
            });
        }

        // Load last and nearest respawn point IDs
        if (json.has("lastRespawnPoint")) {
            explorer.lastRespawnPointId = json.get("lastRespawnPoint").getAsString();
        }
        if (json.has("nearestRespawnPoint")) {
            explorer.nearestRespawnPointId = json.get("nearestRespawnPoint").getAsString();
        }

        return explorer;
    }

    private void loadExplorable(ExplorationSet set, Explorable explorable, long timestamp) {
        Set<CompletedExplorable> explorableSet = completedExplorables.computeIfAbsent(set, k -> new HashSet<>());
        CompletedExplorable completed = new CompletedExplorable(set, explorable, timestamp);
        explorableSet.add(completed);
    }
}
