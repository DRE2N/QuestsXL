package de.erethon.questsxl.instancing;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InstancedBlockCollection {

    // All locations need to be in the same chunk section!

    String id;

    World world;
    Location pos1;
    Location pos2;

    Map<Location, Material> shown = new HashMap<>();
    Map<Location, Material> hidden = new HashMap<>();

    public InstancedBlockCollection() {

    }

    public InstancedBlockCollection(String id) {
        this.id = id;
    }

    public void show(Player player) {
        for (Map.Entry<Location, Material> entry : shown.entrySet()) {
            player.sendBlockChange(entry.getKey(), entry.getValue().createBlockData());
        }
    }

    public void hide(Player player) {
        for (Map.Entry<Location, Material> entry : hidden.entrySet()) {
            player.sendBlockChange(entry.getKey(), entry.getValue().createBlockData());
        }
    }

    public void reset(Player player) {
        for (Block entry : getBlocks(player.getWorld())) {
            player.sendBlockChange(entry.getLocation(), entry.getBlockData());
        }
    }


    public void saveShown() {
        for (Block block : getBlocks(pos1.getWorld())) {
            shown.put(block.getLocation(), block.getType());
        }
    }

    public void saveHidden() {
        for (Block block : getBlocks(pos1.getWorld())) {
            hidden.put(block.getLocation(), block.getType());
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
        world = pos1.getWorld();
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

    public String getId() {
        return id;
    }

    public World getWorld() {
        return world;
    }

    public void load(YamlConfiguration config) {
        world = Bukkit.getWorld(config.getString("world"));
        pos1 = config.getLocation("pos1");
        pos2 = config.getLocation("pos2");
        hidden = loadFromStringList(world, config.getStringList("hidden"));
        shown = loadFromStringList(world, config.getStringList("shown"));
    }

    public Map<Location, Material> loadFromStringList(World world, List<String> list) {
        Map<Location, Material> map = new HashMap<>();
        for (String entry : list) {
            String[] values = entry.split(";");
            Material material = Material.valueOf(values[0]);
            int x = Integer.parseInt(values[1]);
            int y = Integer.parseInt(values[2]);
            int z = Integer.parseInt(values[3]);
            map.put(new Location(world, x, y, z), material);
        }
        return map;
    }

    public YamlConfiguration save() {
        YamlConfiguration section = new YamlConfiguration();
        section.set("pos1", pos1);
        section.set("pos2", pos2);
        section.set("hidden", saveToStringList(hidden));
        section.set("shown", saveToStringList(shown));
        section.set("world", world.getName());
        return section;
    }

    public List<String> saveToStringList(Map<Location, Material> map) {
        List<String> list = new ArrayList<>();
        for (Map.Entry<Location, Material> entry : map.entrySet()) {
            Location loc = entry.getKey();
            String value = entry.getValue().name() + ";" + loc.getBlockX() + ";" + loc.getBlockY() + ";" + loc.getBlockZ();
            list.add(value);
        }
        return list;
    }
}
