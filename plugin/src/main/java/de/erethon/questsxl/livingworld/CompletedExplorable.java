package de.erethon.questsxl.livingworld;

import com.google.gson.JsonObject;
import de.erethon.questsxl.QuestsXL;

public record CompletedExplorable(ExplorationSet set, Explorable explorable, long timestamp) {

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("set", set.id());
        json.addProperty("explorable", explorable.id());
        json.addProperty("timestamp", timestamp);
        return json;
    }

    public static CompletedExplorable fromJson(JsonObject json) {
        Exploration exploration = QuestsXL.getInstance().getExploration();
        ExplorationSet set = exploration.getSet(json.get("set").getAsString());
        Explorable explorable = set.getExplorable(json.get("explorable").getAsString());
        long timestamp = json.get("timestamp").getAsLong();
        return new CompletedExplorable(set, explorable, timestamp);
    }
}
