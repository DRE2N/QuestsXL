package de.erethon.questsxl.common;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.dungeonsxl.api.DungeonsAPI;
import de.erethon.dungeonsxl.api.dungeon.Game;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;

/**
 * @author Malfrador
 * Utility class for relative & DXL-Dungeon locations
 */
public class QLocation {

    private final QuestsXL plugin = QuestsXL.getInstance();
    private final DungeonsAPI dungeonsAPI = plugin.getDungeonsAPI();

    private String worldID;
    private String dungeonID;
    private double x;
    private double y;
    private double z;
    private boolean isRelative = false;

    public QLocation(World world, double x, double y, double z) {
        this.worldID = world.getName();
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public QLocation(String worldID, double x, double y, double z) {
        this.worldID = worldID;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public QLocation(World world, double x, double y, double z, boolean isRelative) {
        this.worldID = world.getName();
        this.x = x;
        this.y = y;
        this.z = z;
        this.isRelative = isRelative;
    }

    public QLocation(String worldID, double x, double y, double z, boolean isRelative) {
        this.worldID = worldID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.isRelative = isRelative;
    }

    public QLocation(boolean dungeon, String dungeonID, double x, double y, double z) {
        this.dungeonID = dungeonID;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public QLocation(boolean dungeon, String dungeonID, double x, double y, double z, boolean isRelative) {
        this.dungeonID = dungeonID;
        this.x = x;
        this.y = y;
        this.z = z;
        this.isRelative = isRelative;
    }

    public QLocation(String[] args, int arrayStart) {
        if (args[arrayStart].contains("~")) {
            isRelative = true;
            x = Double.parseDouble(args[arrayStart].replace("~", ""));
            y = Double.parseDouble(args[arrayStart + 1].replace("~", ""));
            z = Double.parseDouble(args[arrayStart + 2].replace("~", ""));
            return;
        }
        if (args[arrayStart].contains("dungeon:")) {
            dungeonID = args[arrayStart].replace("dungeon:", "");
            x = Double.parseDouble(args[arrayStart + 1]);
            y = Double.parseDouble(args[arrayStart + 2]);
            z = Double.parseDouble(args[arrayStart + 3]);
            return;
        }
        worldID = args[arrayStart];
        x = Double.parseDouble(args[arrayStart + 1]);
        y = Double.parseDouble(args[arrayStart + 2]);
        z = Double.parseDouble(args[arrayStart + 3]);
    }

    public QLocation(ConfigurationSection section) {
        if (section.contains("world")) {
            worldID = section.getString("world");
        }
        if (section.contains("dungeon")) {
            dungeonID = section.getString("dungeon");
        }
        if (section.getString("x").contains("~")) {
            isRelative = true;
            x = Double.parseDouble(section.getString("x").replace("~", ""));
            y = Double.parseDouble(section.getString("y").replace("~", ""));
            z = Double.parseDouble(section.getString("z").replace("~", ""));
            return;
        }
        x = section.getDouble("x");
        y = section.getDouble("y");
        z = section.getDouble("z");
    }

    /**
     * @param location the Bukkit location
     * @return A Bukkit location, relative to the given location if the QLocation is relative
     */
    public Location get(Location location) {
        if (isRelative) {
            return location.clone().add(x, y, z);
        }
        if (Bukkit.getWorld(worldID) == null) {
            MessageUtil.log("World " + worldID + " does not exist!");
            return null;
        }
        return new Location(Bukkit.getWorld(worldID), x, y, z);
    }

    /**
     * @param location the Bukkit location
     * @return true if the given location is equal to the QLocation. If the QLocation is a dungeon location, it will return true if the given location is in the same dungeon.
     */
    public boolean equals(Location location) {
        if (location.getWorld().getName().equals(worldID) && location.getX() == x && location.getY() == y && location.getZ() == z) {
            return true;
        }
        Game game = dungeonsAPI.getGame(location.getWorld());
        if (location.getX() == x && location.getY() == y && location.getZ() == z && game != null && game.getDungeon().getName().equals(dungeonID)) {
            return true;
        }
        return false;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public String getWorldID() {
        return worldID;
    }

    public String getDungeonID() {
        return dungeonID;
    }

    public boolean isRelative() {
        return isRelative;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }

    public void setWorldID(String worldID) {
        this.worldID = worldID;
    }

    public void setDungeonID(String dungeonID) {
        this.dungeonID = dungeonID;
    }

    public void setRelative(boolean relative) {
        isRelative = relative;
    }
}
