package de.erethon.questsxl.respawn;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.livingworld.Explorable;
import de.erethon.questsxl.livingworld.ExplorationSet;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

enum UseMode {
    NEAREST,
    LAST,
}

public class RespawnPoint implements Explorable {

    QuestsXL plugin = QuestsXL.get();

    String id;
    Location location;
    QTranslatable displayName;
    QTranslatable description;
    int cooldown;
    long lastUsed;
    RespawnPointUnlockMode respawnPointUnlockMode;
    UseMode useMode;
    String useQuest;
    ExplorationSet set;

    // VFX system - managed centrally now
    private final Map<UUID, BlockDisplay> playerDisplays = new HashMap<>();
    private final long vfxStartTime = System.currentTimeMillis();

    public RespawnPoint(String id) {
        this.id = id;
    }

    public RespawnPoint(String id, Location location) {
        this.id = id;
        Location l = location.clone();
        l.setPitch(0);
        l.setYaw(0);
        l.add(0, 1.5f, 0);
        this.location = l;
    }

    /**
     * Updates visual effects for nearby players - called by centralized VFX system
     */
    public void updateVFX() {
        Location loc = location();
        if (loc == null || loc.getWorld() == null) return;

        // Check for players within visual range (32 blocks)
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) <= 32 * 32) {
                QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
                if (qPlayer != null && qPlayer.isDataLoaded() && isVisibleTo(qPlayer)) {
                    showDisplayToPlayer(player, qPlayer);
                } else {
                    hideDisplayFromPlayer(player);
                }
            } else {
                hideDisplayFromPlayer(player);
            }
        }

        // Update animations for active displays
        updateDisplayAnimations();

        // Add fancy particle effects
        updateParticleEffects();
    }

    /**
     * Shows the beacon display to a specific player with fancy effects
     */
    private void showDisplayToPlayer(Player player, QPlayer qPlayer) {
        BlockDisplay display = playerDisplays.get(player.getUniqueId());

        if (display == null || !display.isValid()) {
            Location displayLoc = location().add(0.0, 1.5, 0.0);
            display = displayLoc.getWorld().spawn(displayLoc, BlockDisplay.class);

            boolean isUnlocked = isUnlockedFor(qPlayer);
            if (isUnlocked) {
                display.setBlock(Material.BEACON.createBlockData());
            } else {
                display.setBlock(Material.CRYING_OBSIDIAN.createBlockData());
            }

            display.setVisibleByDefault(false);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(32.0f);
            display.setPersistent(false);

            Transformation initialTransform = new Transformation(
                    new Vector3f(-0.5f, -0.5f, -0.5f),
                    new AxisAngle4f(0, 0, 0, 1),
                    new Vector3f(0.8f, 0.8f, 0.8f),
                    new AxisAngle4f(0, 0, 0, 1)
            );
            display.setTransformation(initialTransform);

            playerDisplays.put(player.getUniqueId(), display);
        } else {
            boolean isUnlocked = isUnlockedFor(qPlayer);
            Material currentMaterial = display.getBlock().getMaterial();
            Material expectedMaterial = isUnlocked ? Material.BEACON : Material.CRYING_OBSIDIAN;

            if (currentMaterial != expectedMaterial) {
                display.setBlock(expectedMaterial.createBlockData());
            }
        }

        player.showEntity(plugin, display);
    }

    /**
     * Hides the display from a specific player
     */
    private void hideDisplayFromPlayer(Player player) {
        BlockDisplay display = playerDisplays.get(player.getUniqueId());
        if (display != null && display.isValid()) {
            player.hideEntity(plugin, display);
        }
    }

    /**
     * Hides all displays from all players (used when chunk unloads)
     */
    public void hideAllDisplays() {
        for (Map.Entry<UUID, BlockDisplay> entry : playerDisplays.entrySet()) {
            BlockDisplay display = entry.getValue();
            if (display != null && display.isValid()) {
                // Hide from all online players
                for (Player onlinePlayer : plugin.getServer().getOnlinePlayers()) {
                    onlinePlayer.hideEntity(plugin, display);
                }
            }
        }
    }

    /**
     * Completely removes and cleans up all displays
     */
    public void cleanupAllDisplays() {
        for (Map.Entry<UUID, BlockDisplay> entry : playerDisplays.entrySet()) {
            BlockDisplay display = entry.getValue();
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        playerDisplays.clear();
    }

    /**
     * Updates the animations for all active displays with smooth interpolation
     */
    private void updateDisplayAnimations() {
        long currentTime = System.currentTimeMillis();
        float time = (currentTime - vfxStartTime) / 1000.0f;

        for (Map.Entry<UUID, BlockDisplay> entry : playerDisplays.entrySet()) {
            BlockDisplay display = entry.getValue();
            if (display != null && display.isValid()) {
                float angle = (time * 30.0f) % 360.0f;
                float bobHeight = (float) Math.sin(time * Math.PI * 2.0 / 4.0) * 0.2f;
                Location baseLocation = location().add(0.0, 1.5, 0.0);

                Location newLocation = baseLocation.clone().add(0, bobHeight, 0);
                newLocation.setYaw(angle);
                display.setTeleportDuration(6);
                display.teleport(newLocation);
            }
        }
    }

    /**
     * Creates fancy particle effects around the respawn point
     */
    private void updateParticleEffects() {
        Location loc = location();
        if (loc == null || loc.getWorld() == null) return;

        long currentTime = System.currentTimeMillis();
        float time = (currentTime - vfxStartTime) / 1000.0f;

        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) > 32 * 32) continue;

            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
            if (qPlayer == null || !isVisibleTo(qPlayer)) continue;

            boolean isUnlocked = isUnlockedFor(qPlayer);
            Location centerLoc = loc.clone().add(0.0, 1.8, 0.0);

            if (isUnlocked) {
                createUnlockedParticleEffects(player, centerLoc, time);
            } else {
                createLockedParticleEffects(player, centerLoc, time);
            }
        }
    }

    /**
     * Creates particle effects for unlocked respawn points (green/gold/white theme)
     */
    private void createUnlockedParticleEffects(Player player, Location center, float time) {
        for (int i = 0; i < 2; i++) {
            double angle = (time * 1.5 + i * Math.PI) % (Math.PI * 2);
            double radius = 1.2;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + (time * 0.8 % 2.5) - 1.0 + i * 0.3;

            Location particleLoc = new Location(center.getWorld(), x, y, z);

            player.spawnParticle(org.bukkit.Particle.DUST,
                    particleLoc, 1, 0, 0, 0, 0,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.YELLOW, 1.0f));
        }

        if (Math.random() < 0.03) {
            player.spawnParticle(org.bukkit.Particle.TOTEM_OF_UNDYING,
                    center, 2, 0.3, 0.3, 0.3, 0.05);
        }
    }

    /**
     * Creates particle effects for locked respawn points (dark/purple theme)
     */
    private void createLockedParticleEffects(Player player, Location center, float time) {
        for (int i = 0; i < 2; i++) {
            double angle = (time * 1.0 + i * Math.PI * 0.7) % (Math.PI * 2);
            double radius = 1.0;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + Math.sin(time * 2 + i * Math.PI) * 0.3;

            Location particleLoc = new Location(center.getWorld(), x, y, z);

            player.spawnParticle(org.bukkit.Particle.DUST,
                    particleLoc, 1, 0, 0, 0, 0,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.PURPLE, 1.0f));
        }

        if (Math.random() < 0.02) {
            player.spawnParticle(org.bukkit.Particle.SOUL,
                    center, 1, 0.2, 0.2, 0.2, 0.02);
        }
    }

    // Explorable interface implementation
    @Override
    public String id() {
        return id;
    }

    @Override
    public QTranslatable displayName() {
        return displayName != null ? displayName : QTranslatable.fromString(id);
    }

    @Override
    public Location location() {
        return location != null ? location.clone() : null;
    }

    @Override
    public QTranslatable description() {
        return description;
    }

    @Override
    public boolean countsForProgress() {
        return respawnPointUnlockMode == RespawnPointUnlockMode.NEAR || respawnPointUnlockMode == RespawnPointUnlockMode.ACTION;
    }

    /**
     * Checks if this respawn point should be visible to a player based on its unlock mode
     */
    public boolean isVisibleTo(QPlayer qPlayer) {
        return switch (respawnPointUnlockMode) {
            case NEAR, ACTION -> true; // Always visible for proximity-based unlock modes
            case QUEST -> {
                if (useQuest != null) {
                    QQuest quest = plugin.getQuestManager().getByName(useQuest);
                    yield quest != null && qPlayer.hasQuest(quest);
                }
                yield false;
            }
            default -> true;
        };
    }

    /**
     * Checks if this respawn point is unlocked for a player
     */
    public boolean isUnlockedFor(QPlayer qPlayer) {
        return switch (respawnPointUnlockMode) {
            case NEAR, ACTION -> qPlayer.getExplorer().hasExplored(this);
            case QUEST -> {
                if (useQuest != null) {
                    QQuest quest = plugin.getQuestManager().getByName(useQuest);
                    yield quest != null && qPlayer.hasQuest(quest);
                }
                yield false;
            }
            default -> false;
        };
    }

    public void setLastUsed() {
        lastUsed = System.currentTimeMillis();
    }

    public boolean canRespawn(QPlayer qPlayer) {
        long now = System.currentTimeMillis();
        if (cooldown != 0 && lastUsed + cooldown > now) {
            return false;
        }
        if (respawnPointUnlockMode == RespawnPointUnlockMode.QUEST && useQuest != null) {
            QQuest quest = plugin.getQuestManager().getByName(useQuest);
            if (quest == null) {
                return false;
            }
            return qPlayer.hasQuest(quest);
        }
        return true;
    }

    public String getId() {
        return id;
    }

    public Location getLocation() {
        return location;
    }

    public QTranslatable getDisplayName() {
        return displayName;
    }

    public UseMode getUseMode() {
        return useMode;
    }

    public String getUseQuest() {
        return useQuest;
    }

    public int getCooldown() {
        return cooldown;
    }

    public ExplorationSet getSet() {
        return set;
    }

    public RespawnPointUnlockMode getUnlockMode() {
        return respawnPointUnlockMode;
    }

    public void setDisplayName(QTranslatable displayName) {
        this.displayName = displayName;
    }

    public void setDescription(QTranslatable description) {
        this.description = description;
    }

    public QTranslatable getDescription() {
        return description;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public void setUseMode(UseMode useMode) {
        this.useMode = useMode;
    }

    public void setUseQuest(String useQuest) {
        this.useQuest = useQuest;
    }

    public void setCooldown(int cooldown) {
        this.cooldown = cooldown;
    }

    public void setSet(ExplorationSet set) {
        this.set = set;
    }


    public void setUnlockMode(RespawnPointUnlockMode respawnPointUnlockMode) {
        this.respawnPointUnlockMode = respawnPointUnlockMode;
    }

    public ConfigurationSection save() {
        YamlConfiguration configuration = new YamlConfiguration();
        configuration.set("location", location);
        configuration.set("displayName", displayName.toString());
        configuration.set("description", description != null ? description.toString() : null);
        configuration.set("cooldown", cooldown);
        configuration.set("unlockMode", respawnPointUnlockMode.name());
        if (useMode == null) {
            useMode = UseMode.NEAREST;
        }
        configuration.set("useMode", useMode.name());
        configuration.set("quest", useQuest);
        if (set != null) {
            configuration.set("set", set.id());
        }
        return configuration;
    }

    public void load(ConfigurationSection section) {
        location = section.getLocation("location");
        displayName = QTranslatable.fromString(section.getString("displayName"));
        description = QTranslatable.fromString(section.getString("description", null));
        cooldown = section.getInt("cooldown", 0);
        respawnPointUnlockMode = RespawnPointUnlockMode.valueOf(section.getString("unlockMode", "NEAR"));
        useMode = UseMode.valueOf(section.getString("useMode", "NEAREST"));
        useQuest = section.getString("quest", null);
        String setId = section.getString("set", null);
        if (setId != null) {
            set = plugin.getExploration().getSet(setId);
        }
    }
}
