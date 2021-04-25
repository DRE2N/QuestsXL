package de.erethon.questsxl.instancing;

import com.comphenix.protocol.wrappers.MultiBlockChangeInfo;
import com.comphenix.protocol.wrappers.WrappedBlockData;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.tools.packetwrapper.WrapperPlayServerMultiBlockChange;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.w3c.dom.stylesheets.LinkStyle;

import java.util.*;

public class InstancedBlockCollection {

    // All locations need to be in the same chunk section!

    String id;

    World world;
    Location pos1;
    Location pos2;

    Map<Location, Material> shown = new HashMap<>();
    Map<Location, Material> hidden = new HashMap<>();

    public void show(Player player) {
        WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange();
        ArrayList<WrappedBlockData> data = new ArrayList<>();
        ArrayList<Short> locations = new ArrayList<>();
        for (Map.Entry<Location, Material> entry : shown.entrySet()) {
            data.add(WrappedBlockData.createData(entry.getValue()));
            locations.add(packet.getShortLoc(entry.getKey()));
        }
        packet.setChunk(pos1);
        packet.setRecords(data, locations);
        packet.sendPacket(player);
    }

    public void showSlowly(Player player, int delay) {
        Map<Location, Material> tempMap = new HashMap<>(shown);
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
        WrapperPlayServerMultiBlockChange packet = new WrapperPlayServerMultiBlockChange();
        ArrayList<WrappedBlockData> data = new ArrayList<>();
        ArrayList<Short> locations = new ArrayList<>();
        for (Map.Entry<Location, Material> entry : hidden.entrySet()) {
            data.add(WrappedBlockData.createData(entry.getValue()));
            locations.add(packet.getShortLoc(entry.getKey()));
        }
        packet.setChunk(pos1);

        packet.setRecords(data, locations);
        packet.sendPacket(player);
    }

    public void hideSlowly(Player player, int delay) {
        Map<Location, Material> tempMap = new HashMap<>(hidden);
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

    public void load(ConfigurationSection section) {
        id = section.getName();
        world = Bukkit.getWorld(section.getString("world"));
        hidden = loadFromStringList(world, section.getStringList("hidden"));
        shown = loadFromStringList(world, section.getStringList("shown"));
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

    public ConfigurationSection save() {
        MemoryConfiguration section = new MemoryConfiguration();
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
