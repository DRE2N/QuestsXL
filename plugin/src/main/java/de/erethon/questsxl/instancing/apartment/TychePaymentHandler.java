package de.erethon.questsxl.instancing.apartment;

import de.erethon.tyche.EconomyService;
import de.erethon.tyche.TychePlugin;
import de.erethon.tyche.models.OwnerType;
import de.erethon.tyche.models.TransactionStatus;
import org.bukkit.entity.Player;

import java.util.concurrent.CompletableFuture;

public class TychePaymentHandler implements ApartmentService.PaymentHandler {

    @Override
    public CompletableFuture<Boolean> hasFunds(Player player, double amount) {
        EconomyService economyService = TychePlugin.getEconomyService();
        if  (economyService == null) {
            return CompletableFuture.completedFuture(false);
        }
        return economyService.getBalance(player.getUniqueId(), OwnerType.PLAYER, "Herone").thenApply(balance -> balance >= amount);
    }

    @Override
    public CompletableFuture<Boolean> withdraw(Player player, double amount) {
        EconomyService economyService = TychePlugin.getEconomyService();
        if  (economyService == null) {
            return CompletableFuture.completedFuture(false);
        }
        return economyService.withdraw(player.getUniqueId(), OwnerType.PLAYER, "Herone", (long) amount, "Apartment rent", player.getUniqueId()).thenApply(result -> result.status() == TransactionStatus.COMPLETED);
    }
}

