package de.erethon.questsxl.region;


import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.quest.QQuest;
import de.erethon.questsxl.quest.QuestManager;
import org.apache.commons.lang.math.IntRange;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class QRegion {

    QuestsXL plugin = QuestsXL.getInstance();
    QuestManager questManager = plugin.getQuestManager();
    public long lastAccessed;

    private String id;
    private Location pos1;
    private Location pos2;
    private QQuest linkedQuest;
    private final Set<RegionFlag> questFlags = new HashSet<>();
    private final Set<RegionFlag> publicFlags = new HashSet<>();

    public QRegion(String id) {
        this.id = id;
        publicFlags.add(RegionFlag.PROTECTED);
    }

    public boolean isInRegion(Location location) {
        double xp = location.getX();
        double yp = location.getY();
        double zp = location.getZ();

        double x1 = pos1.getX();
        double y1 = pos1.getY();
        double z1 = pos1.getZ();
        double x2 = pos2.getX();
        double y2 = pos2.getY();
        double z2 = pos2.getZ();
        return new IntRange(x1, x2).containsDouble(xp) && new IntRange(y1, y2).containsDouble(yp) && new IntRange(z1, z2).containsDouble(zp);
    }
    public Location getPos1() {
        return pos1;
    }

    public void setPos1(Location pos1) {
        this.pos1 = pos1;
    }

    public Location getPos2() {
        return pos2;
    }

    public void setPos2(Location pos2) {
        this.pos2 = pos2;
    }

    public QQuest getLinkedQuest() {
        return linkedQuest;
    }

    public void setLinkedQuest(QQuest linkedQuest) {
        this.linkedQuest = linkedQuest;
    }

    public String getId() {
        return id;
    }

    public boolean hasPublicFlag(RegionFlag flag) {
        return publicFlags.contains(flag);
    }

    public boolean hasQuestFlag(RegionFlag flag) {
        return questFlags.contains(flag);
    }

    public Set<RegionFlag> getQuestFlags() {
        return questFlags;
    }

    public Set<RegionFlag> getPublicFlags() {
        return publicFlags;
    }

    public String getNiceLocation() {
        return "&7Pos 1: &6" + pos1.getBlockX() + "&8/&6" + pos1.getBlockY() + "&8/&6" + pos1.getBlockZ() + " &8- &7Pos2: &6" + pos2.getBlockX() + "&8/&6" + pos2.getBlockY() + "&8/&6" + pos2.getBlockZ();
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) { return false; }
        if(!(obj instanceof QRegion)) { return false; }
        QRegion other = (QRegion) obj;
        return other.getPos1().equals(this.getPos1()) && other.getPos2().equals(this.getPos2()) && other.getLinkedQuest() == this.getLinkedQuest();
    }

    public void load(ConfigurationSection section) {
        pos1 = section.getLocation("pos1");
        pos2 = section.getLocation("pos2");
        if (section.contains("quest")) {
            linkedQuest = questManager.getByName(section.getString("quest"));
        }
        if (section.contains("publicFlags")) {
            for (String s : section.getStringList("publicFlags")) {
                RegionFlag flag = RegionFlag.valueOf(s.toUpperCase());
                publicFlags.add(flag);
            }
        }
        if (section.contains("questFlags")) {
            for (String s : section.getStringList("questFlags")) {
                RegionFlag flag = RegionFlag.valueOf(s.toUpperCase());
                questFlags.add(flag);
            }
        }
    }

    public ConfigurationSection save() {
        MemoryConfiguration config = new MemoryConfiguration();
        config.set("pos1", pos1);
        config.set("pos2", pos2);
        if (linkedQuest != null) {
            config.set("quest", linkedQuest.getName());
        }
        List<String> list = new ArrayList<>();
        for (RegionFlag s : publicFlags) {
            list.add(s.name());
        }
        config.set("publicFlags", list);
        List<String> list2 = new ArrayList<>();
        for (RegionFlag s : questFlags) {
            list2.add(s.name());
        }
        config.set("questFlags", list2);
        return config;
    }
}
