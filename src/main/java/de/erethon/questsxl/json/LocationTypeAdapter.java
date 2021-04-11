package de.erethon.questsxl.json;

import com.google.gson.*;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.Location;
import org.bukkit.World;

import java.lang.reflect.Type;
import java.util.UUID;

public class LocationTypeAdapter implements JsonSerializer<Location>, JsonDeserializer<Location> {

    @Override
    public JsonElement serialize(Location src, Type typeOfSrc, JsonSerializationContext context) {
        String location = src.getWorld().getUID() + ";" + src.getX() + ";" + src.getY() + ";" + src.getZ() + ";" + src.getYaw() + ";" + src.getPitch();
        return new JsonPrimitive(location);
    }

    @Override
    public Location deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        String source = json.getAsString();
        String[] data = source.split(";");
        World world = QuestsXL.getInstance().getServer().getWorld(UUID.fromString(data[0]));
        double x = Double.parseDouble(data[1]);
        double y = Double.parseDouble(data[2]);
        double z = Double.parseDouble(data[3]);
        float yaw = Float.parseFloat(data[4]);
        float pitch = Float.parseFloat(data[5]);
        return new Location(world, x, y, z, yaw, pitch);
    }

}