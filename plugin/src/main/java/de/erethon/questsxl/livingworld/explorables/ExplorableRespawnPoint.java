package de.erethon.questsxl.livingworld.explorables;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.QTranslatable;
import de.erethon.questsxl.livingworld.Explorable;
import de.erethon.questsxl.livingworld.ExplorationSet;
import de.erethon.questsxl.player.QPlayer;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.respawn.RespawnPoint;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An explorable respawn point that integrates with the exploration system.
 * Only respawn points with NEAR unlock mode are part of the exploration system.
 */
public class ExplorableRespawnPoint implements Explorable {

    private final QuestsXL plugin = QuestsXL.get();
    private final RespawnPoint respawnPoint;
    private ExplorationSet set;

    // Visual effects
    private final Map<UUID, BlockDisplay> playerDisplays = new HashMap<>();

    public ExplorableRespawnPoint(RespawnPoint respawnPoint) {
        this.respawnPoint = respawnPoint;
        // Register with the centralized VFX manager
        ExplorableRespawnPointVFXManager.getInstance().register(this);
    }

    @Override
    public String id() {
        return respawnPoint.getId();
    }

    @Override
    public QTranslatable displayName() {
        return respawnPoint.getDisplayName() != null ? respawnPoint.getDisplayName() : QTranslatable.fromString(respawnPoint.getId());
    }

    @Override
    public Location location() {
        return respawnPoint.getLocation().clone();
    }

    @Override
    public boolean countsForProgress() {
        return true;
    }

    public RespawnPoint getRespawnPoint() {
        return respawnPoint;
    }

    public ExplorationSet getSet() {
        return set;
    }

    public void setSet(ExplorationSet set) {
        this.set = set;
    }

    /**
     * Checks if this respawn point should be visible to a player based on its unlock mode
     */
    public boolean isVisibleTo(QPlayer qPlayer) {
        switch (respawnPoint.getUnlockMode()) {
            case NEAR:
                return true; // Always visible for NEAR mode
            case QUEST:
                if (respawnPoint.getUseQuest() != null) {
                    QQuest quest = plugin.getQuestManager().getByName(respawnPoint.getUseQuest());
                    return quest != null && qPlayer.hasQuest(quest);
                }
                return false;
            case ACTION:
                return qPlayer.getExplorer().hasExplored(this); // Only visible if unlocked
            default:
                return true;
        }
    }

    /**
     * Checks if this respawn point is unlocked for a player
     */
    public boolean isUnlockedFor(QPlayer qPlayer) {
        switch (respawnPoint.getUnlockMode()) {
            case NEAR, ACTION:
                if (set != null) {
                    return qPlayer.getExplorer().hasExplored(this);
                } else {
                    return qPlayer.getExplorer().isStandaloneRespawnPointUnlocked(respawnPoint.getId());
                }
            case QUEST:
                if (respawnPoint.getUseQuest() != null) {
                    QQuest quest = plugin.getQuestManager().getByName(respawnPoint.getUseQuest());
                    return quest != null && qPlayer.hasQuest(quest);
                }
                return false;
            default:
                return false;
        }
    }

    /**
     * Starts the visual effects for this respawn point
     * Now handled by the centralized VFX manager
     */
    private void startVisualEffects() {
        // No longer needed - VFX manager handles this
    }

    /**
     * Updates visual effects for nearby players
     * Called by the centralized VFX manager
     */
    public void updateVFX() {
        Location loc = location();
        if (loc == null || loc.getWorld() == null) return;

        Chunk chunk = loc.getChunk();
        if (!chunk.isLoaded()) {
            // Hide all displays if chunk is not loaded
            hideAllDisplays();
            return;
        }

        // Check for players within visual range (32 blocks)
        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) <= 32 * 32) {
                QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
                if (qPlayer != null && isVisibleTo(qPlayer)) {
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
    private void hideAllDisplays() {
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
     * Updates the animations for all active displays with smooth interpolation
     */
    private void updateDisplayAnimations() {
        long currentTime = System.currentTimeMillis();
        float time = (currentTime - ExplorableRespawnPointVFXManager.getInstance().getStartTime()) / 1000.0f;

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
        float time = (currentTime - ExplorableRespawnPointVFXManager.getInstance().getStartTime()) / 1000.0f;

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
                    center, 2, 0.3, 0.3, 0.3, 0.05); // Reduced count from 5 to 2
        }
    }

    /**
     * Creates particle effects for locked respawn points (red/orange/smoke theme)
     */
    private void createLockedParticleEffects(Player player, Location center, float time) {
        for (int i = 0; i < 2; i++) {
            double angle = (time * -0.8 + i * Math.PI) % (Math.PI * 2);
            double radius = 1.0 + Math.sin(time * 1.2) * 0.2;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + Math.sin(time * 0.8 + i) * 0.4;

            Location particleLoc = new Location(center.getWorld(), x, y, z);

            player.spawnParticle(org.bukkit.Particle.DUST,
                    particleLoc, 1, 0, 0, 0, 0,
                    new org.bukkit.Particle.DustOptions(org.bukkit.Color.fromRGB(200, 50, 50), 1.0f));
        }

        if (Math.random() < 0.1) {
            Location smokeLoc = center.clone().add(
                    (Math.random() - 0.5) * 0.8,
                    Math.random() * 0.5,
                    (Math.random() - 0.5) * 0.8
            );
            player.spawnParticle(org.bukkit.Particle.LARGE_SMOKE,
                    smokeLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        if (Math.random() < 0.02) {
            player.spawnParticle(org.bukkit.Particle.FLAME,
                    center, 3, 0.4, 0.4, 0.4, 0.02);
        }
    }
}
