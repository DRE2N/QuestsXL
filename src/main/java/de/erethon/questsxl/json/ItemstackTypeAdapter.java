package de.erethon.questsxl.json;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import de.erethon.questsxl.QuestsXL;
import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

import java.lang.reflect.Type;
import java.util.Map;

public class ItemstackTypeAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    @Override
    public ItemStack deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
        Gson gson = QuestsXL.getInstance().getGson();
        Map<String, Object> map = gson.fromJson(jsonElement, new TypeToken<Map<String, Object>>(){}.getType());
        return ItemStack.deserialize(map);
    }

    @Override
    public JsonElement serialize(ItemStack itemStack, Type type, JsonSerializationContext context) {
        Gson gson = QuestsXL.getInstance().getGson();
        return new JsonPrimitive(gson.toJson(itemStack.serialize()));
    }
}
