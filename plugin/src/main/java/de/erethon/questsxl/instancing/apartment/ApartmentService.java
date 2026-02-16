package de.erethon.questsxl.instancing.apartment;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.instancing.InstanceDao;
import de.erethon.questsxl.instancing.InstanceManager;
import de.erethon.questsxl.instancing.InstanceTemplate;
import de.erethon.questsxl.instancing.InstancedArea;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Sign;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

/**
 * Service that bridges apartment rent signs to the instancing system.
 */
public class ApartmentService implements Listener {

    public static class RentalInfo {
        public final String templateId;
        public final Instant expiresAt;
        public RentalInfo(String templateId, Instant expiresAt) { this.templateId = templateId; this.expiresAt = expiresAt; }
    }

    public interface PaymentHandler {
        CompletableFuture<Boolean> hasFunds(Player player, double amount);
        CompletableFuture<Boolean> withdraw(Player player, double amount);
    }

    private final QuestsXL plugin;
    private final InstanceManager instanceManager;
    private final InstanceDao instanceDao;
    private final NamespacedKey templateKey;
    private final NamespacedKey priceKey;
    private final NamespacedKey durationKey;

    private BiPredicate<Player, String> rentGuard = (player, templateId) -> true;
    private PaymentHandler paymentHandler;

    private double rentCost;
    private long rentDurationMinutes;

    private final Map<UUID, Long> lastAttempt = new ConcurrentHashMap<>();
    private final Map<String, Map<String, Set<ChunkPos>>> templateChunks = new ConcurrentHashMap<>(); // templateId -> world -> chunks
    private final Map<String, Map<ChunkPos, Set<String>>> worldChunkIndex = new ConcurrentHashMap<>(); // world -> chunk -> templateIds
    private final Map<UUID, String> currentApartment = new ConcurrentHashMap<>(); // player UUID -> current apartment template

    public record ChunkPos(int x, int z) { }

    public ApartmentService(QuestsXL plugin, InstanceManager instanceManager) {
        this.plugin = plugin;
        this.instanceManager = instanceManager;
        this.instanceDao = instanceManager.getInstanceDao();
        this.templateKey = new NamespacedKey(plugin, "apartment_template");
        this.priceKey = new NamespacedKey(plugin, "apartment_price");
        this.durationKey = new NamespacedKey(plugin, "apartment_duration");
        loadConfig();
        loadTemplateChunks();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    private void loadConfig() {
        FileConfiguration cfg = plugin.getConfig();
        cfg.addDefault("apartments.rentCost", 1);
        cfg.addDefault("apartments.rentDurationMinutes", 1440L);
        cfg.options().copyDefaults(true);
        plugin.saveConfig();

        rentCost = cfg.getDouble("apartments.rentCost", 1);
        rentDurationMinutes = cfg.getLong("apartments.rentDurationMinutes", 1440L);
    }

    private void loadTemplateChunks() {
        if (instanceDao == null) return;
        var rows = instanceDao.getAllTemplateChunks();
        QuestsXL.log("[ApartmentService] Loading template chunks, found " + (rows == null ? 0 : rows.size()) + " rows");

        templateChunks.clear();
        worldChunkIndex.clear();
        if (rows == null || rows.isEmpty()) {
            QuestsXL.log("[ApartmentService] No template chunks found in database");
            return;
        }

        for (var row : rows) {
            templateChunks.computeIfAbsent(row.templateId, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(row.worldName, k -> ConcurrentHashMap.newKeySet())
                    .add(new ChunkPos(row.chunkX, row.chunkZ));
            worldChunkIndex
                    .computeIfAbsent(row.worldName, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(new ChunkPos(row.chunkX, row.chunkZ), k -> ConcurrentHashMap.newKeySet())
                    .add(row.templateId);
            QuestsXL.log("[ApartmentService] Indexed template " + row.templateId + " at chunk (" + row.chunkX + ", " + row.chunkZ + ") in world " + row.worldName);
        }
        QuestsXL.log("[ApartmentService] Loaded " + templateChunks.size() + " templates into chunk index");
    }

    @EventHandler(ignoreCancelled = true)
    private void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;
        if (event.getFrom().getChunk().equals(event.getTo().getChunk())) return;
        Player player = event.getPlayer();
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            return;
        }

        String worldName = player.getWorld().getName();
        int cx = player.getLocation().getBlockX() >> 4;
        int cz = player.getLocation().getBlockZ() >> 4;

        // Check if player should leave current apartment
        String currentTemplate = currentApartment.get(player.getUniqueId());
        if (currentTemplate != null && instanceManager.isInInstance(qPlayer)) {
            boolean stillNearby = isNearApartment(currentTemplate, worldName, cx, cz);
            if (!stillNearby) {
                QuestsXL.log("[ApartmentService] Player " + player.getName() + " moved away from apartment " + currentTemplate + ", unloading");
                instanceManager.leaveInstance(qPlayer);
                currentApartment.remove(player.getUniqueId());
                return; // Don't try to enter another apartment immediately
            }
        }

        // Don't try to enter if already in an instance
        if (instanceManager.isInInstance(qPlayer)) {
            return;
        }

        var worldIndex = worldChunkIndex.get(worldName);
        if (worldIndex == null || worldIndex.isEmpty()) {
            return;
        }

        Set<String> nearbyTemplates = new HashSet<>();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                ChunkPos pos = new ChunkPos(cx + dx, cz + dz);
                var templates = worldIndex.get(pos);
                if (templates != null) {
                    nearbyTemplates.addAll(templates);
                }
            }
        }

        if (nearbyTemplates.isEmpty()) {
            return;
        }

        QuestsXL.log("[ApartmentService] Player " + player.getName() + " near apartment chunks " + cx + "," + cz + ", checking templates: " + nearbyTemplates);
        for (String templateId : nearbyTemplates) {
            // Only check templates that are marked as rentable
            var template = instanceManager.getTemplate(templateId);
            if (template == null || !template.isRentable()) {
                continue;
            }
            throttledEnter(player, qPlayer, templateId, false);
        }
    }

    private boolean isNearApartment(String templateId, String worldName, int playerChunkX, int playerChunkZ) {
        var byWorld = templateChunks.get(templateId);
        if (byWorld == null) return false;
        var chunkSet = byWorld.get(worldName);
        if (chunkSet == null) return false;

        for (ChunkPos pos : chunkSet) {
            if (Math.abs(pos.x() - playerChunkX) <= 1 && Math.abs(pos.z() - playerChunkZ) <= 1) {
                return true;
            }
        }
        return false;
    }


    public NamespacedKey getTemplateKey() {
        return templateKey;
    }

    public void setRentGuard(BiPredicate<Player, String> rentGuard) {
        this.rentGuard = Objects.requireNonNullElse(rentGuard, (player, templateId) -> true);
    }

    public void setPaymentHandler(PaymentHandler handler) {
        this.paymentHandler = handler;
    }

    public double getRentCost() {
        return rentCost;
    }

    public long getRentDurationMinutes() {
        return rentDurationMinutes;
    }

    public boolean isRentSign(Sign sign) {
        return getTemplateId(sign) != null;
    }

    public void tagSign(Sign sign, String templateId) {
        PersistentDataContainer container = sign.getPersistentDataContainer();
        container.set(templateKey, PersistentDataType.STRING, templateId);
        sign.update();
    }

    public void clearTag(Sign sign) {
        sign.getPersistentDataContainer().remove(templateKey);
        sign.update();
    }

    public String getTemplateId(Sign sign) {
        return sign.getPersistentDataContainer().get(templateKey, PersistentDataType.STRING);
    }

    public void setSignRentInfo(Sign sign, String templateId, double price, long durationMinutes) {
        PersistentDataContainer container = sign.getPersistentDataContainer();
        container.set(templateKey, PersistentDataType.STRING, templateId);
        container.set(priceKey, PersistentDataType.DOUBLE, price);
        container.set(durationKey, PersistentDataType.LONG, durationMinutes);
        sign.update();
    }

    public double getSignPrice(Sign sign) {
        Double price = sign.getPersistentDataContainer().get(priceKey, PersistentDataType.DOUBLE);
        return price != null ? price : rentCost;
    }

    public long getSignDuration(Sign sign) {
        Long duration = sign.getPersistentDataContainer().get(durationKey, PersistentDataType.LONG);
        return duration != null ? duration : rentDurationMinutes;
    }

    public CompletableFuture<InstancedArea> enterApartment(Player player, String templateId, Sign sign) {
        QPlayer qPlayer = plugin.getDatabaseManager().getCurrentPlayer(player);
        if (qPlayer == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Player data not loaded.");
            return CompletableFuture.completedFuture(null);
        }

        InstanceTemplate template = instanceManager.getTemplate(templateId);
        if (template == null) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Template '" + templateId + "' not found");
            return CompletableFuture.completedFuture(null);
        }

        if (!rentGuard.test(player, templateId)) {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "You cannot rent this right now.");
            return CompletableFuture.completedFuture(null);
        }

        double price = sign != null ? getSignPrice(sign) : rentCost;
        long duration = sign != null ? getSignDuration(sign) : rentDurationMinutes;

        return ensureRental(player, qPlayer, templateId, price, duration).thenApply(allowed -> {
            if (!allowed) {
                return null;
            }
            return instanceManager.enterInstance(qPlayer, templateId);
        });
    }

    private void throttledEnter(Player player, QPlayer qPlayer, String templateId, boolean announce) {
        long now = System.currentTimeMillis();
        long last = lastAttempt.getOrDefault(player.getUniqueId(), 0L);
        if (now - last < 2000) {
            QuestsXL.log("[ApartmentService] Throttled apartment entry for " + player.getName() + " (template: " + templateId + ")");
            return;
        }
        lastAttempt.put(player.getUniqueId(), now);

        QuestsXL.log("[ApartmentService] Checking rental for " + player.getName() + " (template: " + templateId + ")");
        ensureRental(player, qPlayer, templateId).thenAccept(allowed -> {
            QuestsXL.log("[ApartmentService] Rental check result for " + player.getName() + " (template: " + templateId + "): " + allowed);
            if (!allowed) {
                return;
            }
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (!instanceManager.isInInstance(qPlayer)) {
                    QuestsXL.log("[ApartmentService] Entering apartment instance for " + player.getName() + " (template: " + templateId + ")");
                    instanceManager.enterInstance(qPlayer, templateId);
                    currentApartment.put(player.getUniqueId(), templateId);
                    if (announce) {
                        MessageUtil.sendMessage(player, "&aEntering your apartment instance for '&e" + templateId + "&a'.");
                    }
                }
            });
        });
    }

    private CompletableFuture<Boolean> ensureRental(Player player, QPlayer qPlayer, String templateId) {
        return ensureRental(player, qPlayer, templateId, rentCost, rentDurationMinutes);
    }

    private CompletableFuture<Boolean> ensureRental(Player player, QPlayer qPlayer, String templateId, double price, long durationMinutes) {
        if (instanceDao == null) {
            QuestsXL.log("[ApartmentService] instanceDao is null");
            return CompletableFuture.completedFuture(false);
        }

        UUID characterId = plugin.getDatabaseManager().getCurrentCharacterId(player);
        if (characterId == null) {
            QuestsXL.log("[ApartmentService] characterId is null for " + player.getName());
            return CompletableFuture.completedFuture(false);
        }

        QuestsXL.log("[ApartmentService] Querying rental expiry for character " + characterId + " template " + templateId);
        return CompletableFuture.supplyAsync(() -> instanceDao.getRentalExpiry(characterId, templateId)).thenCompose(expiry -> {
            Instant now = Instant.now();
            QuestsXL.log("[ApartmentService] Rental expiry for " + player.getName() + " template " + templateId + ": " + expiry + " (now: " + now + ")");
            if (expiry != null && expiry.isAfter(now)) {
                return CompletableFuture.completedFuture(true);
            }
            QuestsXL.log("[ApartmentService] Rental expired or not found, attempting to charge and extend");
            return chargeAndExtend(player, characterId, templateId, now.plus(Duration.ofMinutes(durationMinutes)), price);
        });
    }

    private CompletableFuture<Boolean> chargeAndExtend(Player player, UUID characterId, String templateId, Instant newExpiry, double price) {
        if (paymentHandler == null) {
            return persistRental(characterId, templateId, newExpiry);
        }

        return paymentHandler.hasFunds(player, price).thenCompose(has -> {
            if (!has) {
                MessageUtil.sendMessage(player, QuestsXL.ERROR + "You don't have enough money for this rent.");
                return CompletableFuture.completedFuture(false);
            }
            return paymentHandler.withdraw(player, price).thenCompose(success -> {
                if (!success) {
                    MessageUtil.sendMessage(player, QuestsXL.ERROR + "Payment failed.");
                    return CompletableFuture.completedFuture(false);
                }
                return persistRental(characterId, templateId, newExpiry);
            });
        });
    }

    private CompletableFuture<Boolean> persistRental(UUID characterId, String templateId, Instant expiry) {
        return CompletableFuture.supplyAsync(() -> {
            instanceDao.upsertRental(characterId, templateId, expiry);
            return true;
        });
    }
}
