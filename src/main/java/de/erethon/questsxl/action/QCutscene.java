package de.erethon.questsxl.action;

import com.destroystokyo.paper.entity.Pathfinder;
import com.destroystokyo.paper.entity.ai.PaperMobGoals;
import de.erethon.commons.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.players.QPlayer;
import net.minecraft.server.v1_16_R3.EntityArmorStand;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftArmorStand;
import org.bukkit.craftbukkit.v1_16_R3.entity.CraftEntity;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.*;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QCutscene extends QBaseAction{

    private transient final QuestsXL plugin = QuestsXL.getInstance();
    private List<Location> locations = new ArrayList<>();
    private List<String> messages = new ArrayList<>();
    private int progress = 0;
    private ItemStack headItem = new ItemStack(Material.CARVED_PUMPKIN);
    double stepSize;

    public void play(Player player) {
        if (!conditions(player)) return;

        Location loc = player.getLocation();
        GameMode previousGamemode = player.getGameMode();
        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.CUSTOM);
        EntityArmorStand nmsStand= ((CraftArmorStand) stand).getHandle();

        nmsStand.noclip = true;
        stand.setInvisible(true);
        player.setGameMode(GameMode.SPECTATOR);
        player.getInventory().setItem(EquipmentSlot.HEAD, headItem);
        player.setSpectatorTarget(stand);


        BukkitRunnable mover = new BukkitRunnable() {
            int tick = 1;
            @Override
            public void run() {
                float speedMod = 0.5F;
                float lookMod = 1.0F;
                Location current = stand.getLocation();
                Location target = locations.get(progress);
                Vector vCurrent = current.toVector();
                Vector vTarget = target.toVector();
                Vector direction = vTarget.subtract(vCurrent);
                double currentYaw = clampYaw(current.getYaw());
                double targetYaw = clampYaw(target.getYaw());
                double currentPitch = current.getPitch();
                double targetPitch = target.getPitch();
                double diffYaw = Math.abs(targetYaw - currentYaw);
                double diffPitch = Math.abs(targetPitch - currentPitch);
                if (current.distance(target) < 1) {
                    progress++;
                    tick = 1;
                    if (progress >= locations.size()) {
                        player.setGameMode(previousGamemode);
                        player.getInventory().setItem(EquipmentSlot.HEAD, null);
                        player.getInventory().setItem(EquipmentSlot.HAND, null);
                        stand.remove();
                        onFinish(player);
                        cancel();
                        return;
                    }
                    stepSize = (diffYaw / locations.get(progress).distance(current)) * lookMod;
                    return;
                }
                //player.sendActionBar(MessageUtil.parse(messages.get(progress)));
                stand.setVelocity(direction.multiply(tick).normalize().multiply(speedMod));
                float yaw = 0.00F;
                float pitch = 0.00F;
                if (shortestAngleDistance((float) currentYaw, (float) targetYaw) >= 0) {
                    if (Math.abs(currentYaw - targetYaw) <= stepSize) {
                        yaw = (float) (currentYaw + Math.abs(currentYaw - targetYaw));
                    } else {
                        yaw = (float) (currentYaw + stepSize);
                    }
                } else {
                    if (Math.abs(currentYaw - targetYaw) <= stepSize) {
                        yaw = (float) (currentYaw - Math.abs(currentYaw - targetYaw));
                    } else {
                        yaw = (float) (currentYaw - stepSize);
                    }
                }
                if (targetPitch >= currentPitch) {
                    if ((Math.abs(targetPitch - currentPitch) <= stepSize)) {
                        pitch = (float) (currentPitch + (Math.abs(targetPitch - currentPitch)));
                    } else {
                        pitch = (float) (currentPitch + stepSize);
                    }
                } else {
                    if ((Math.abs(targetPitch - currentPitch) <= stepSize)) {
                        pitch = (float) (currentPitch - (Math.abs(targetPitch - currentPitch)));
                    } else {
                        pitch = (float) (currentPitch - stepSize);
                    }
                }
                stand.setRotation(clampYaw(yaw), pitch);
                player.sendActionBar("Current: " + currentPitch + "/" + targetPitch + " Stepsize: " + stepSize);
                tick++;

            }
        };
        mover.runTaskTimer(plugin, 5,1);
    }

    public void setLocs(List<Location> locs) {
        this.locations = locs;
    }

    private float clampYaw(float yaw) {
        while(yaw < -180.0F) {
            yaw += 360.0F;
        }

        while(yaw >= 180.0F) {
            yaw -= 360.0F;
        }

        return yaw;
    }

    private float shortestAngleDistance(float fromAngle, float toAngle) {
        float delta = fromAngle - toAngle;
        return (delta > 180) ? 360 - delta : (delta < -180) ? -360 - delta : -delta;
    }

    @Override
    public Material getIcon() {
        return Material.PAINTING;
    }
}
