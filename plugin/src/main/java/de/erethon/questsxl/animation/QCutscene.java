package de.erethon.questsxl.animation;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.QAction;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.entity.CraftArmorStand;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QCutscene {

    private transient final QuestsXL plugin = QuestsXL.get();

    private final String id;

    private final List<Location> locations = new ArrayList<>();
    private List<String> messages = new ArrayList<>();
    private Map<Player, Integer> progress = new HashMap<>();
    private final ItemStack headItem = new ItemStack(Material.CARVED_PUMPKIN);
    double stepSize;

    public QCutscene(String id) {
        this.id = id;
    }

    public void play(Player player, QAction action) {
        progress.put(player, 0);
        MessageUtil.broadcastMessage("LocsPlay: " + locations.size());
        Location loc = player.getLocation();
        GameMode previousGamemode = player.getGameMode();
        ItemStack previousHead = player.getInventory().getHelmet();
        ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND, CreatureSpawnEvent.SpawnReason.CUSTOM);
        net.minecraft.world.entity.decoration.ArmorStand nmsStand= ((CraftArmorStand) stand).getHandle();
        nmsStand.noPhysics = true;
        stand.setInvisible(true);
        player.setGameMode(GameMode.SPECTATOR);
        //player.getInventory().setItem(EquipmentSlot.HEAD, headItem);
        player.setSpectatorTarget(stand);

        BukkitRunnable mover = new BukkitRunnable() {
            int tick = 1;
            @Override
            public void run() {
                float speedMod = 0.5F;
                float lookMod = 1.0F;
                Location current = stand.getLocation();
                Location target = locations.get(progress.get(player));
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
                    progress.put(player, progress.get(player) + 1);
                    tick = 1;
                    if (progress.get(player) >= locations.size()) {
                        player.setGameMode(previousGamemode);
                        //player.getInventory().setItem(EquipmentSlot.HEAD, previousHead);
                        stand.remove();
                        action.onFinish(plugin.getPlayerCache().getByPlayer(player));
                        progress.put(player, 0);
                        cancel();
                        return;
                    }
                    stepSize = (diffYaw / locations.get(progress.get(player)).distance(current)) * lookMod;
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
                tick++;

            }
        };
        mover.runTaskTimer(plugin, 5,1);
    }

    public void setLocs(List<Location> locs) {
        locations.addAll(locs);
        MessageUtil.broadcastMessage("Locs: " + locations.size());
    }
    public void setMessages(List<String> strings) {
        this.messages = strings;
    }

    public List<Location> getLocations() {
        return locations;
    }

    public List<String> getMessages() {
        return messages;
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

    public String getId() {
        return id;
    }

    public void load(YamlConfiguration configuration) {
        for (String s : configuration.getStringList("points")) {
            String[] entry = s.split("#");
            Location location = getLocFromString(entry[0]);
            locations.add(location);
            messages.add(entry[0]);
        }
    }

    private Location getLocFromString(String s) {
        String[] v = s.split(";");
        World world = Bukkit.getWorld(v[0]);
        double x = Double.parseDouble(v[1]);
        double y = Double.parseDouble(v[2]);
        double z = Double.parseDouble(v[3]);
        float yaw = Float.parseFloat(v[4]);
        float pitch = Float.parseFloat(v[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }

    private String locationToString(Location location) {
        String world = location.getWorld().getName();
        double x = location.getX();
        double y = location.getY();
        double z = location.getZ();
        float yaw = location.getYaw();
        float pitch = location.getPitch();
        return world + ";" + x + ";" + y + ";" + z + ";" + yaw + ";" + pitch;
    }

    public YamlConfiguration save() {
        YamlConfiguration configuration = new YamlConfiguration();
        int num = 0;
        List<String> strings = new ArrayList<>();
        for (Location location : locations) {
            String msg = null;
            try {
                msg = messages.get(num);
            } catch (IndexOutOfBoundsException ignored) { }
            if (msg != null) {
                strings.add(locationToString(location) + "#" + messages.get(num));
            } else {
                strings.add(locationToString(location)+ "#");
            }
            num++;
        }
        configuration.set("points", strings);
        return configuration;
    }

}
