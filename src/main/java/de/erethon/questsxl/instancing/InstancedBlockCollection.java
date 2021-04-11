package de.erethon.questsxl.instancing;

import de.erethon.questsxl.QuestsXL;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class InstancedBlockCollection {

    Location pos1;
    Location pos2;

    Map<Location, Material> blocks = new HashMap<>();

    public void show(Player player) {
        for (Map.Entry<Location, Material> entry : blocks.entrySet()) {
            player.sendBlockChange(entry.getKey(), entry.getValue().createBlockData());
        }
    }

    public void showSlowly(Player player, int delay) {
        Map<Location, Material> tempMap = new HashMap<>(blocks);
        BukkitRunnable asyncShowTask = new BukkitRunnable() {
            @Override
            public void run() {
                List<Location> keysAsArray = new ArrayList<>(tempMap.keySet());
                Random r = new Random();
                Location location = keysAsArray.get(r.nextInt(keysAsArray.size()));
                player.sendBlockChange(location, tempMap.get(location).createBlockData());
                tempMap.remove(location);
            }
        };
        asyncShowTask.runTaskTimerAsynchronously(QuestsXL.getInstance(), 0, delay);
    }

    public void hide(Player player) {
        for (Location entry : blocks.keySet()) {
            player.sendBlockChange(entry, Material.AIR.createBlockData());
        }
    }

    public void hideSlowly(Player player, int delay) {
        Map<Location, Material> tempMap = new HashMap<>(blocks);
        BukkitRunnable asyncShowTask = new BukkitRunnable() {
            @Override
            public void run() {
                List<Location> keysAsArray = new ArrayList<>(tempMap.keySet());
                Random r = new Random();
                Location location = keysAsArray.get(r.nextInt(keysAsArray.size()));
                player.sendBlockChange(location, Material.AIR.createBlockData());
                tempMap.remove(location);
            }
        };
        asyncShowTask.runTaskTimerAsynchronously(QuestsXL.getInstance(), 0, delay);
    }


    public void save() {
        for (Block block : getBlocks(pos1.getWorld())) {
            blocks.put(block.getLocation(), block.getType());
            block.setType(Material.AIR);
        }
    }

    public Location getPos1() {
        return pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    private Set<Block> getBlocks(World world) {
        Set<Block> blockList = new HashSet<>();
        Set<Location> result = new HashSet<>();
        double minX = Math.min(pos1.getX(), pos2.getX());
        double minY = Math.min(pos1.getY(), pos2.getY());
        double minZ = Math.min(pos1.getZ(), pos2.getZ());
        double maxX = Math.max(pos1.getX(), pos2.getX());
        double maxY = Math.max(pos1.getY(), pos2.getY());
        double maxZ = Math.max(pos1.getZ(), pos2.getZ());
        for (double x = minX; x <= maxX; x+=1) {
            for (double y = minY; y <= maxY; y+=1) {
                for (double z = minZ; z <= maxZ; z+=1) {
                    result.add(new Location(world, x, y, z));
                }
            }
        }
        for (Location location : result) {
            blockList.add(world.getBlockAt(location));
        }

        return blockList;
    }
}
