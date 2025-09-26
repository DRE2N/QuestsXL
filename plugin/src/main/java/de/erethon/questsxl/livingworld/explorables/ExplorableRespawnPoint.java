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
import org.bukkit.Particle;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
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
            case NEAR:
                // For NEAR mode, check if player has explored it
                return qPlayer.getExplorer().hasExplored(this);
            case QUEST:
                if (respawnPoint.getUseQuest() != null) {
                    QQuest quest = plugin.getQuestManager().getByName(respawnPoint.getUseQuest());
                    return quest != null && qPlayer.hasQuest(quest);
                }
                return false;
            case ACTION:
                return qPlayer.getExplorer().hasExplored(this);
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
            // Create new display
            Location displayLoc = location().add(0.5, 1.2, 0.5);
            display = displayLoc.getWorld().spawn(displayLoc, BlockDisplay.class);
            display.setBlock(Material.BEACON.createBlockData());
            display.setVisibleByDefault(false);
            display.setBrightness(new Display.Brightness(15, 15)); // Full brightness
            display.setViewRange(32.0f); // Set view range

            // Set initial scale to be slightly larger
            Transformation initialTransform = new Transformation(
                new Vector3f(0, 0, 0), // Translation
                new AxisAngle4f(0, 0, 0, 1), // Left rotation
                new Vector3f(1.2f, 1.2f, 1.2f), // Scale (20% larger)
                new AxisAngle4f(0, 0, 0, 1) // Right rotation
            );
            display.setTransformation(initialTransform);

            playerDisplays.put(player.getUniqueId(), display);
        }

        // Show to player
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
                // Smooth rotation animation (360 degrees every 8 seconds)
                float rotationY = (time * 45.0f) % 360.0f; // 45 degrees per second

                // Smooth bobbing animation (sine wave, 0.3 blocks amplitude, 3 second period)
                float bobHeight = (float) Math.sin(time * Math.PI * 2.0 / 3.0) * 0.3f;

                // Gentle scale pulsing (between 1.1 and 1.3, 4 second period)
                float scalePulse = 1.2f + (float) Math.sin(time * Math.PI * 2.0 / 4.0) * 0.1f;

                // Create transformation with smooth animations
                Transformation transformation = new Transformation(
                    new Vector3f(0, bobHeight, 0), // Translation (bobbing)
                    new AxisAngle4f(0, 0, 0, 1), // Left rotation (none)
                    new Vector3f(scalePulse, scalePulse, scalePulse), // Pulsing scale
                    new AxisAngle4f((float) Math.toRadians(rotationY), 0, 1, 0) // Rotation around Y-axis
                );

                display.setTransformation(transformation);
                display.setInterpolationDuration(4); // 4 ticks (0.2 seconds) for smooth animation
                display.setInterpolationDelay(0);
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

        // Only update particles every 5 ticks to reduce performance impact
        if (currentTime % 250 != 0) return;

        for (Player player : loc.getWorld().getPlayers()) {
            if (player.getLocation().distanceSquared(loc) > 32 * 32) continue;

            QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
            if (qPlayer == null || !isVisibleTo(qPlayer)) continue;

            boolean isUnlocked = isUnlockedFor(qPlayer);
            Location centerLoc = loc.clone().add(0.5, 1.5, 0.5);

            if (isUnlocked) {
                // Fancy effects for UNLOCKED respawn points
                createUnlockedParticleEffects(player, centerLoc, time);
            } else {
                // Fancy effects for LOCKED respawn points
                createLockedParticleEffects(player, centerLoc, time);
            }
        }
    }

    /**
     * Creates particle effects for unlocked respawn points (green/gold theme)
     */
    private void createUnlockedParticleEffects(Player player, Location center, float time) {
        // Spiral of golden particles rising up
        for (int i = 0; i < 8; i++) {
            double angle = (time * 2.0 + i * Math.PI / 4.0) % (Math.PI * 2);
            double radius = 1.0 + Math.sin(time * 1.5) * 0.2;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + (time * 0.5 % 3.0) - 1.0 + i * 0.1;

            Location particleLoc = new Location(center.getWorld(), x, y, z);

            // Golden dust particles in spiral
            player.spawnParticle(org.bukkit.Particle.DUST_COLOR_TRANSITION,
                particleLoc, 1, 0, 0, 0, 0,
                new org.bukkit.Particle.DustTransition(
                    org.bukkit.Color.YELLOW, org.bukkit.Color.LIME, 1.0f));
        }

        // Ring of happy villager particles at base
        for (int i = 0; i < 12; i++) {
            double angle = i * Math.PI * 2 / 12.0 + time * 0.5;
            double x = center.getX() + Math.cos(angle) * 1.5;
            double z = center.getZ() + Math.sin(angle) * 1.5;
            Location ringLoc = new Location(center.getWorld(), x, center.getY() - 0.8, z);

            player.spawnParticle(org.bukkit.Particle.HAPPY_VILLAGER,
                ringLoc, 1, 0.1, 0.1, 0.1, 0);
        }

        // Occasional burst of sparkles
        if (Math.random() < 0.1) { // 10% chance per update
            player.spawnParticle(org.bukkit.Particle.END_ROD,
                center, 8, 0.8, 0.8, 0.8, 0.02);
        }

        // Gentle upward floating particles
        for (int i = 0; i < 3; i++) {
            Location floatLoc = center.clone().add(
                (Math.random() - 0.5) * 2.0,
                Math.random() * 2.0 - 0.5,
                (Math.random() - 0.5) * 2.0
            );
            player.spawnParticle(org.bukkit.Particle.COMPOSTER,
                floatLoc, 1, 0, 0.1, 0, 0.05);
        }
    }

    /**
     * Creates particle effects for locked respawn points (blue/purple theme)
     */
    private void createLockedParticleEffects(Player player, Location center, float time) {
        // Mystical blue spiral
        for (int i = 0; i < 6; i++) {
            double angle = (time * -1.5 + i * Math.PI / 3.0) % (Math.PI * 2);
            double radius = 0.8 + Math.cos(time * 2.0) * 0.3;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + Math.sin(time * 1.5 + i) * 0.5;

            Location particleLoc = new Location(center.getWorld(), x, y, z);

            // Blue to purple transition particles
            player.spawnParticle(org.bukkit.Particle.DUST_COLOR_TRANSITION,
                particleLoc, 1, 0, 0, 0, 0,
                new org.bukkit.Particle.DustTransition(
                    org.bukkit.Color.AQUA, org.bukkit.Color.PURPLE, 1.2f));
        }

        // Enchanting table particles in a wider circle
        for (int i = 0; i < 15; i++) {
            double angle = Math.random() * Math.PI * 2;
            double radius = 1.0 + Math.random() * 1.0;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            double y = center.getY() + Math.random() * 2.0 - 1.0;

            Location enchantLoc = new Location(center.getWorld(), x, y, z);

            player.spawnParticle(Particle.ENCHANT,
                enchantLoc, 1, 0, 0.3, 0, 0.8);
        }

        // Pulsing ring effect
        double pulseRadius = 1.2 + Math.sin(time * 3.0) * 0.4;
        for (int i = 0; i < 16; i++) {
            double angle = i * Math.PI * 2 / 16.0;
            double x = center.getX() + Math.cos(angle) * pulseRadius;
            double z = center.getZ() + Math.sin(angle) * pulseRadius;
            Location pulseLoc = new Location(center.getWorld(), x, center.getY() - 0.5, z);

            player.spawnParticle(org.bukkit.Particle.SOUL,
                pulseLoc, 1, 0.05, 0.05, 0.05, 0.01);
        }

        // Occasional magical burst
        if (Math.random() < 0.08) { // 8% chance per update
            player.spawnParticle(org.bukkit.Particle.WITCH,
                center, 12, 1.0, 1.0, 1.0, 0.1);
        }

        // Floating mystical orbs
        for (int i = 0; i < 2; i++) {
            Location orbLoc = center.clone().add(
                Math.sin(time + i * Math.PI) * 1.5,
                Math.cos(time * 0.8 + i) * 0.8,
                Math.cos(time + i * Math.PI) * 1.5
            );
            player.spawnParticle(org.bukkit.Particle.DRAGON_BREATH,
                orbLoc, 2, 0.1, 0.1, 0.1, 0.01);
        }
    }

    /**
     * Cleans up visual effects when the respawn point is removed
     */
    public void cleanup() {
        // Unregister from the VFX manager
        ExplorableRespawnPointVFXManager.getInstance().unregister(this);
    }

    /**
     * Cleans up VFX displays (called by VFX manager)
     */
    public void cleanupVFX() {
        // Remove all displays
        for (BlockDisplay display : playerDisplays.values()) {
            if (display != null && display.isValid()) {
                display.remove();
            }
        }
        playerDisplays.clear();
    }
}
