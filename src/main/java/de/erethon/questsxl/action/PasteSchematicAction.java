package de.erethon.questsxl.action;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.Transform;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Vector;

public class PasteSchematicAction extends QBaseAction {

    File schematic;
    Location location;
    int time;

    @Override
    public void play(Player player) {
        Clipboard clipboard = null;
        try {
            clipboard = FaweAPI.load(schematic);
        } catch (Exception e) {
            MessageUtil.log("Error loading schematic " + schematic.getName());
            e.printStackTrace();
        }
        if (clipboard == null || location == null) {
            return;
        }
        MessageUtil.broadcastMessage("Clipboard dimensions: " + clipboard.getDimensions().toString());
        com.sk89q.worldedit.world.World world = FaweAPI.getWorld(location.getWorld().getName());
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1)) {
            MessageUtil.broadcastMessage("World: " + session.getWorld().getName());
            MessageUtil.broadcastMessage("Location: " + location.getX() + ", " + location.getY() + ", " + location.getZ());
            Operation operation = new ClipboardHolder(clipboard)
                    .createPaste(session)
                    .to(BlockVector3.at(location.getX(), location.getY(), location.getZ()))
                    .ignoreAirBlocks(false)
                    .build();
            Operations.complete(operation);
            BukkitRunnable undoRunnable = new BukkitRunnable() {
                @Override
                public void run() {
                    EditSession undo = null; //FaweAPI.getEditSessionBuilder(world).build();
                    session.undo(undo);
                    session.flushQueue();
                    undo.flushQueue();
                }
            };
            undoRunnable.runTaskLater(QuestsXL.getInstance(), time);
        }
    }

    @Override
    public void load(String[] msg) {
        MessageUtil.log("Loading from array: " + Arrays.toString(msg));
        World world = Bukkit.getWorld(msg[1]);
        double x = Double.parseDouble(msg[2]);
        double y = Double.parseDouble(msg[3]);
        double z = Double.parseDouble(msg[4]);
        if (world == null) {
            throw new RuntimeException("The action " + Arrays.toString(msg) + " contains a location in an invalid world.");
        }
        location = new Location(world, x, y, z);

        String schematicID = msg[0];
        MessageUtil.log("Loading schematic " + msg[0]);
        for (File file : QuestsXL.SCHEMATICS.listFiles()) {
            if (file.getName().equals(schematicID)) {
                schematic = file;
            }
        }
        if (schematic == null) {
            throw new RuntimeException("The action " + id + " tried to load schematic that does not exist: " + schematicID);
        }
        time = Integer.parseInt(msg[5]);
    }

    @Override
    public void load(ConfigurationSection section) {
        super.load(section);
        MessageUtil.log("Loading from section");
        String world = section.getString("world");
        double x = section.getDouble("x");
        double y = section.getDouble("y");
        double z = section.getDouble("z");
        float yaw = (float) section.getDouble("yaw", 0.00);
        float pitch = (float) section.getDouble("pitch", 0.00);
        if (world == null) {
            throw new RuntimeException("The action " + id + " contains a location in an invalid world.");
        }
        location = new Location(Bukkit.getWorld(world), x, y, z, yaw, pitch);

        String schematicID = section.getString("schematic");
        MessageUtil.log("Loading schematic " + schematicID);
        for (File file : QuestsXL.SCHEMATICS.listFiles()) {
            if (file.getName().equals(schematicID)) {
                schematic = file;
            }
        }
        if (schematic == null) {
            throw new RuntimeException("The action " + id + " tried to load schematic that does not exist: " + schematicID);
        }
        time = section.getInt("time", 60);
    }
}
