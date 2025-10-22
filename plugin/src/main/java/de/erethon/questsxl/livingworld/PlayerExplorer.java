package de.erethon.questsxl.livingworld;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.player.QPlayer;
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
    private String lastRespawnPointId; // Store the ID of the last respawn point used
    private String nearestRespawnPointId; // Store the ID of the nearest respawn point
    private ExplorationSet currentClosestSet;
    private ContentGuide contentGuide;

    // Track standalone explorables completion
    private final Set<CompletedExplorable> completedStandaloneExplorables = new HashSet<>();

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
        if (explorableSet.stream().anyMatch(e -> e.explorable().id().equals(explorable.id()))) {
            return false;
        }
        CompletedExplorable completed = new CompletedExplorable(set, explorable, timestamp);
        explorableSet.add(completed);

        qPlayer.sendMessage(MiniMessage.miniMessage().deserialize("<dark_purple><strikethrough>          </strikethrough> <dark_gray>[<#edcc4b>❄<dark_gray>]<#edcc4b> <dark_purple><strikethrough>          </strikethrough>"));
        qPlayer.sendMessage(Component.text(" "));
        // Send unlock message based on explorable type
        if (explorable instanceof RespawnPoint respawnPoint) {
            qPlayer.sendMessage(Component.translatable("qxl.explorable.respawn.unlocked",
                respawnPoint.displayName().get()));
        } else if (explorable instanceof PointOfInterest poi) {
            qPlayer.sendMessage(Component.translatable("qxl.explorable.poi.discovered",
                poi.displayName().get()));
        } else {
            qPlayer.sendMessage(Component.translatable("qxl.explorable.discovered",
                explorable.displayName().get()));
        }
        qPlayer.sendMessage(explorable.description().get());

        set.checkCompletion(qPlayer);
        qPlayer.saveToDatabase(); // Always save to database to ensure persistence
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
            // Use ID-based comparison for consistency
            boolean done = list.stream().anyMatch(e -> e.explorable().id().equals(explorable.id()));
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
        // Check in exploration sets
        for (Set<CompletedExplorable> explorableSet : completedExplorables.values()) {
            if (explorableSet.stream().anyMatch(e -> e.explorable().id().equals(explorable.id()))) {
                return true;
            }
        }

        // Check in standalone explorables
        if (hasStandaloneExplorable(explorable)) {
            return true;
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
        // Check all explorables (including respawn points that implement Explorable directly)
        for (Explorable explorable : plugin.getExploration().getAllExplorables()) {
            // Check if this is a respawn point with NEAR unlock mode
            if (explorable.location().getWorld() != qPlayer.getPlayer().getLocation().getWorld()) {
                continue;
            }
            if (explorable instanceof de.erethon.questsxl.respawn.RespawnPoint respawnPoint) {
                if (respawnPoint.getUnlockMode() != de.erethon.questsxl.respawn.RespawnPointUnlockMode.NEAR) {
                    continue;
                }

                if (!respawnPoint.isVisibleTo(qPlayer)) {
                    continue;
                }

                boolean alreadyUnlocked = respawnPoint.isUnlockedFor(qPlayer);

                if (alreadyUnlocked) {
                    continue;
                }

                double distance = qPlayer.getPlayer().getLocation().distance(respawnPoint.location());
                if (distance <= 3.0) {
                    ExplorationSet set = plugin.getExploration().getSetContaining(respawnPoint);
                    if (set != null) {
                        // Respawn point is part of an exploration set - mark as explored within the set
                        if (completeExplorable(set, respawnPoint, System.currentTimeMillis())) {
                            qPlayer.getPlayer().playSound(respawnPoint.location(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.66f, 1.5f);
                        }
                    } else {
                        // Standalone respawn point - mark as explored directly
                        if (completeStandaloneExplorable(respawnPoint, System.currentTimeMillis())) {
                            qPlayer.getPlayer().playSound(respawnPoint.location(), org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.66f, 1.5f);
                        }
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
        RespawnPoint respawnPoint = plugin.getRespawnPointManager().getRespawnPoint(respawnPointId);
        if (respawnPoint == null) {
            return false;
        }

        // Check if it's already unlocked
        if (hasStandaloneExplorable(respawnPoint)) {
            return false;
        }

        // Add to the completedStandaloneExplorables collection
        completedStandaloneExplorables.add(new CompletedExplorable(null, respawnPoint, System.currentTimeMillis()));

        qPlayer.sendMessage(Component.translatable("qxl.explorable.respawn.unlocked",
            respawnPoint.getDisplayName().get()));

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
        boolean wasUnlocked = completedStandaloneExplorables.removeIf(completed ->
            completed.explorable().id().equals(respawnPointId));

        // Save to database if something was actually removed
        if (wasUnlocked) {
            qPlayer.saveToDatabase();
        }

        return wasUnlocked;
    }

    /**
     * Checks if a standalone respawn point is unlocked.
     * @param respawnPointId The ID of the respawn point to check
     * @return true if the respawn point is unlocked, false otherwise
     */
    public boolean isStandaloneRespawnPointUnlocked(String respawnPointId) {
        return completedStandaloneExplorables.stream()
            .anyMatch(completed -> completed.explorable().id().equals(respawnPointId));
    }

    /**
     * Checks if a respawn point is unlocked (works for both exploration-based and standalone).
     * @param respawnPoint The respawn point to check
     * @return true if the respawn point is unlocked, false otherwise
     */
    public boolean isRespawnPointUnlocked(RespawnPoint respawnPoint) {
        return hasExplored(respawnPoint);
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

        // Save standalone explorables
        if (!completedStandaloneExplorables.isEmpty()) {
            JsonObject standaloneExplorableJson = new JsonObject();
            for (CompletedExplorable completed : completedStandaloneExplorables) {
                standaloneExplorableJson.add(completed.explorable().id(), completed.toJson());
            }
            json.add("standaloneExplorables", standaloneExplorableJson);
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
            if (key.equals("standaloneExplorables") || key.equals("standaloneRespawnPoints") ||
                key.equals("lastRespawnPoint") || key.equals("nearestRespawnPoint")) {
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

                JsonObject completedJson = entry.getValue().getAsJsonObject();
                long timestamp = completedJson.get("timestamp").getAsLong();

                explorer.loadExplorable(set, explorable, timestamp);
            });
        });

        // Load standalone explorables - handle both old and new key names
        JsonObject standaloneExplorableJson = null;
        if (json.has("standaloneExplorables")) {
            standaloneExplorableJson = json.getAsJsonObject("standaloneExplorables");
        } else if (json.has("standaloneRespawnPoints")) {
            // Handle legacy key name
            standaloneExplorableJson = json.getAsJsonObject("standaloneRespawnPoints");
        }

        if (standaloneExplorableJson != null) {
            standaloneExplorableJson.entrySet().forEach(entry -> {
                String explorableId = entry.getKey();
                JsonObject completedJson = entry.getValue().getAsJsonObject();
                long timestamp = completedJson.get("timestamp").getAsLong();

                // First try to find it as a standalone explorable
                Explorable explorable = explorer.plugin.getExploration().getStandaloneExplorable(explorableId);

                // If not found as standalone explorable, try to find it as a respawn point
                if (explorable == null) {
                    RespawnPoint respawnPoint = explorer.plugin.getRespawnPointManager().getRespawnPoint(explorableId);
                    if (respawnPoint != null) {
                        explorable = respawnPoint;
                    }
                }

                if (explorable != null) {
                    explorer.completedStandaloneExplorables.add(new CompletedExplorable(null, explorable, timestamp));
                }
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

    /**
     * Marks a standalone explorable as completed by the player.
     * @param explorable The explorable that was completed
     * @param timestamp The time when it was completed
     * @return true if the explorable was marked as completed, false if it was already completed
     */
    public boolean completeStandaloneExplorable(Explorable explorable, long timestamp) {
        // Check if already explored
        if (hasStandaloneExplorable(explorable)) {
            return false;
        }

        // Add to standalone collection
        completedStandaloneExplorables.add(new CompletedExplorable(null, explorable, timestamp));

        // Send unlock message
        qPlayer.sendMessage(MiniMessage.miniMessage().deserialize("<dark_purple><strikethrough>          </strikethrough> <dark_gray>[<#edcc4b>❄<dark_gray>]<#edcc4b> <dark_purple><strikethrough>          </strikethrough>"));
        qPlayer.sendMessage(Component.text(" "));

        if (explorable instanceof de.erethon.questsxl.respawn.RespawnPoint) {
            qPlayer.sendMessage(Component.translatable("qxl.explorable.respawn.unlocked",
                explorable.displayName().get()));
        } else {
            qPlayer.sendMessage(Component.translatable("qxl.explorable.discovered",
                explorable.displayName().get()));
        }

        if (explorable.description() != null) {
            qPlayer.sendMessage(explorable.description().get());
        }

        // Save to database to ensure persistence
        qPlayer.saveToDatabase();

        return true;
    }

    /**
     * Checks if a standalone explorable has been completed
     */
    public boolean hasStandaloneExplorable(Explorable explorable) {
        return completedStandaloneExplorables.stream()
            .anyMatch(e -> e.explorable().id().equals(explorable.id()));
    }

    /**
     * Gets the completed standalone explorables set
     */
    public Set<CompletedExplorable> getCompletedStandaloneExplorables() {
        return completedStandaloneExplorables;
    }
}
