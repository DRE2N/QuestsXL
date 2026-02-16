package de.erethon.questsxl.instancing.apartment;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.instancing.InstanceManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.SignChangeEvent;

/**
 * Listens for apartment rent sign creation and interaction.
 */
public class ApartmentSignListener implements Listener {

    private final QuestsXL plugin;
    private final ApartmentService apartmentService;
    private final InstanceManager instanceManager;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ApartmentSignListener(QuestsXL plugin, ApartmentService apartmentService, InstanceManager instanceManager) {
        this.plugin = plugin;
        this.apartmentService = apartmentService;
        this.instanceManager = instanceManager;
    }

    @EventHandler(ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (!event.getPlayer().hasPermission("qxl.admin.apartment")) {
            return;
        }

        String firstLine = event.getLine(0);
        if (firstLine == null || !firstLine.equalsIgnoreCase("[apartment]")) {
            return;
        }

        String templateId = event.getLine(1);
        if (templateId == null || templateId.isBlank()) {
            MessageUtil.sendMessage(event.getPlayer(), QuestsXL.ERROR + "Usage: Line 1 = [apartment], Line 2 = <template>, Line 3 (optional) = <price>, Line 4 (optional) = <duration_minutes>");
            return;
        }

        if (instanceManager.getTemplate(templateId) == null) {
            MessageUtil.sendMessage(event.getPlayer(), QuestsXL.ERROR + "Template '" + templateId + "' not found");
            return;
        }

        // Parse optional price and duration
        double price = apartmentService.getRentCost();
        long durationMinutes = apartmentService.getRentDurationMinutes();

        String line3 = event.getLine(2);
        if (line3 != null && !line3.isBlank()) {
            try {
                price = Double.parseDouble(line3);
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(event.getPlayer(), QuestsXL.ERROR + "Invalid price: " + line3);
                return;
            }
        }

        String line4 = event.getLine(3);
        if (line4 != null && !line4.isBlank()) {
            try {
                durationMinutes = Long.parseLong(line4);
            } catch (NumberFormatException e) {
                MessageUtil.sendMessage(event.getPlayer(), QuestsXL.ERROR + "Invalid duration: " + line4);
                return;
            }
        }

        Block block = event.getBlock();
        if (!(block.getState() instanceof Sign sign)) {
            return;
        }

        apartmentService.setSignRentInfo(sign, templateId, price, durationMinutes);

        event.line(0, mm.deserialize("<green>[Apartment]"));
        event.line(1, mm.deserialize("<yellow>" + templateId));
        event.line(2, mm.deserialize(" "));
        event.line(3, mm.deserialize("<gold>" + price));

        MessageUtil.sendMessage(event.getPlayer(), "&aCreated apartment sign for '&e" + templateId + "&a' ($" + price + " for " + durationMinutes + " minutes)");
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        Block clicked = event.getClickedBlock();
        if (clicked == null) {
            return;
        }

        if (!(clicked.getState() instanceof Sign sign)) {
            return;
        }

        String templateId = apartmentService.getTemplateId(sign);
        if (templateId == null) {
            return;
        }

        event.setCancelled(true); // Prevent default edit/open

        Player player = event.getPlayer();
        apartmentService.enterApartment(player, templateId, sign).thenAccept(instance -> {
            if (instance != null) {
                MessageUtil.sendMessage(player, "&aEntering your apartment instance for '&e" + templateId + "&a'.");
            }
        }).exceptionally(ex -> {
            MessageUtil.sendMessage(player, QuestsXL.ERROR + "Could not enter apartment: " + ex.getMessage());
            return null;
        });
    }
}
