package de.erethon.questsxl.action;

import com.fastasyncworldedit.core.FaweAPI;
import com.sk89q.worldedit.EditSession;
import com.sk89q.worldedit.WorldEdit;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.function.operation.Operation;
import com.sk89q.worldedit.function.operation.Operations;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.session.ClipboardHolder;
import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.common.ObjectiveHolder;
import de.erethon.questsxl.common.QConfig;
import de.erethon.questsxl.common.QLineConfig;
import de.erethon.questsxl.common.QLoadableDoc;
import de.erethon.questsxl.common.QLocation;
import de.erethon.questsxl.common.QParamDoc;
import de.erethon.questsxl.livingworld.QEvent;
import de.erethon.questsxl.player.QPlayer;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.Arrays;

@QLoadableDoc(
        value = "paste_schematic",
        description = "Pastes a schematic at a location and undoes it after a certain amount of time, optionally.",
        shortExample = "- 'paste_schematic: x=~5; y=~0; z=0; schematic=example.schematic; time=60'",
        longExample = {
                "paste_schematic:",
                "  location:",
                "    x: 123",
                "    y: 64",
                "    z: 321",
                "  schematic: example.schematic",
                "  time: 60"
        }
)
public class PasteSchematicAction extends QBaseAction {

    @QParamDoc(name = "location", description = "The location to paste the schematic at. QLocation", required = true)
    private QLocation location;
    @QParamDoc(name = "schematic", description = "The name of the schematic file in the FAWE schematics folder", required = true)
    private File schematic;
    @QParamDoc(name = "time", description = "The time in ticks after which the schematic will be undone", def="60")
    private int time;

    @Override
    public void play(QPlayer player) {
        paste(player);
    }

    @Override
    public void play(QEvent event)  {
        paste(event); // Only paste it once, not per player, lol.
    }

    private void paste(ObjectiveHolder holder) {
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
        Location bukkitLocation = null;
        if (holder instanceof QEvent event) {
            bukkitLocation = location.get(event.getLocation());
        }
        if (holder instanceof QPlayer player) {
            bukkitLocation = location.get(player.getLocation());
        }
        com.sk89q.worldedit.world.World world = FaweAPI.getWorld(bukkitLocation.getWorld().getName());
        try (EditSession session = WorldEdit.getInstance().getEditSessionFactory().getEditSession(world, -1)) {
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
    public void load(QConfig cfg) {
        super.load(cfg);
        location = cfg.getQLocation("location");

        String schematicID = cfg.getString("schematic");
        MessageUtil.log("Loading schematic " + schematicID);
        for (File file : QuestsXL.SCHEMATICS.listFiles()) {
            if (file.getName().equals(schematicID)) {
                schematic = file;
            }
        }
        if (schematic == null) {
            throw new RuntimeException("The action " + id + " tried to load schematic that does not exist: " + schematicID);
        }
        time = cfg.getInt("time", 60);
    }
}
