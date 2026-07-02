package de.erethon.hermes;

import de.erethon.questsxl.QuestsXL;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.util.Locale;
import java.util.function.Supplier;

public enum HermesContentType {
    QUESTS("quests", "Quests", true, () -> QuestsXL.QUESTS),
    EVENTS("events", "Events", true, () -> QuestsXL.EVENTS),
    INTERACTIONS("interactions", "Interactions", true, () -> QuestsXL.INTERACTIONS),
    MACROS("macros", "Macros", true, () -> QuestsXL.MACROS),
    DIALOGUES("dialogues", "Dialogues", true, () -> QuestsXL.DIALOGUES),
    ANIMATIONS("animations", "Animations", true, () -> QuestsXL.ANIMATIONS),
    BLOCKS("blocks", "Block Collections", true, () -> QuestsXL.IBCS),
    REGIONS("regions", "Regions", false, () -> QuestsXL.REGIONS),
    RESPAWNS("respawns", "Respawn Points", false, () -> QuestsXL.RESPAWNS),
    EXPLORABLES("explorables", "Explorables", false, () -> QuestsXL.EXPLORABLES),
    EXPLORATION_SETS("explorationSets", "Exploration Sets", false, () -> QuestsXL.EXPLORATION_SETS),
    GLOBAL_OBJECTIVES("globalObjectives", "Global Objectives", false, () -> QuestsXL.GLOBAL_OBJ),
    PERIODIC_QUESTS("periodicQuests", "Periodic Quests", false, () -> QuestsXL.PERIODIC_QUESTS),
    AETHER_MOBS("aetherMobs", "Mobs", true, () -> pluginDataFolder("Aether", "creatures")),
    HEPHAESTUS_ITEMS("hephaestusItems", "Items", true, () -> pluginDataFolder("Hephaestus", "items")),
    HEPHAESTUS_UPGRADES("hephaestusUpgrades", "Item Upgrades", true, () -> pluginDataFolder("Hephaestus", "upgrades")),
    HEPHAESTUS_JOBS("hephaestusJobs", "Jobs", true, () -> pluginDataFolder("Hephaestus", "")),
    HEPHAESTUS_VANILLA_RECIPES("hephaestusVanillaRecipes", "Vanilla Recipes", true, () -> pluginDataFolder("Hephaestus", "")),
    HEPHAESTUS_JOB_RECIPES("hephaestusJobRecipes", "Job Recipes", true, () -> pluginDataFolder("Hephaestus", "recipes")),
    HEPHAESTUS_SHOPS("hephaestusShops", "Shops", true, () -> pluginDataFolder("Hephaestus", "shops")),
    HECATE_CLASSES("hecateClasses", "Classes", true, () -> pluginDataFolder("Hecate", "classes")),
    HECATE_TRAITLINES("hecateTraitlines", "Traitlines", true, () -> pluginDataFolder("Hecate", "traitlines")),
    SPELLBOOK_SPELLS("spellbookSpells", "Spells", true, () -> pluginDataFolder("Spellbook", "spells")),
    SPELLBOOK_TRAITS("spellbookTraits", "Traits", true, () -> pluginDataFolder("Spellbook", "traits")),
    SPELLBOOK_EFFECTS("spellbookEffects", "Spell Effects", true, () -> pluginDataFolder("Spellbook", "effects")),
    FACTIONS_BUILDINGS("factionsBuildings", "Buildings", true, () -> pluginDataFolder("Factions", "buildings")),
    FACTIONS_BUILDING_TAGS("factionsBuildingTags", "Building Tags", false, () -> pluginDataFolder("Factions", "buildingTags.yml")),
    DAEDALUS_MODELS("daedalusModels", "Models", true, () -> pluginDataFolder("Daedalus", "models")),
    TRANSLATIONS("translations", "Translations", false, () -> new File("plugins"));

    private final String id;
    private final String label;
    private final boolean directory;
    private final Supplier<File> rootSupplier;

    HermesContentType(String id, String label, boolean directory, Supplier<File> rootSupplier) {
        this.id = id;
        this.label = label;
        this.directory = directory;
        this.rootSupplier = rootSupplier;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public boolean directory() {
        return directory;
    }

    public boolean visibleInWebEditor() {
        return switch (this) {
            case ANIMATIONS, BLOCKS, REGIONS, RESPAWNS, EXPLORATION_SETS, FACTIONS_BUILDING_TAGS -> false;
            default -> true;
        };
    }

    public File root() {
        return rootSupplier.get();
    }

    public String gitRootName() {
        return switch (this) {
            case QUESTS, EVENTS, INTERACTIONS, MACROS, DIALOGUES, ANIMATIONS, BLOCKS, REGIONS, RESPAWNS,
                    EXPLORABLES, EXPLORATION_SETS, GLOBAL_OBJECTIVES, PERIODIC_QUESTS -> "QuestsXL";
            case AETHER_MOBS -> "Aether";
            case HEPHAESTUS_ITEMS, HEPHAESTUS_UPGRADES, HEPHAESTUS_JOBS, HEPHAESTUS_VANILLA_RECIPES, HEPHAESTUS_JOB_RECIPES, HEPHAESTUS_SHOPS -> "Hephaestus";
            case HECATE_CLASSES, HECATE_TRAITLINES -> "Hecate";
            case SPELLBOOK_SPELLS, SPELLBOOK_TRAITS, SPELLBOOK_EFFECTS -> "Spellbook";
            case FACTIONS_BUILDINGS, FACTIONS_BUILDING_TAGS -> "Factions";
            case DAEDALUS_MODELS -> "Daedalus";
            case TRANSLATIONS -> "";
        };
    }

    public String gitRelativePath(String itemPath) {
        String normalized = itemPath == null ? "" : itemPath.replace('\\', '/').trim();
        return switch (this) {
            case QUESTS -> "quests/" + normalized;
            case EVENTS -> "events/" + normalized;
            case INTERACTIONS -> "interactions/" + normalized;
            case MACROS -> "macros/" + normalized;
            case DIALOGUES -> "dialogues/" + normalized;
            case GLOBAL_OBJECTIVES -> "globalObjectives.yml";
            case PERIODIC_QUESTS -> "periodicQuests.yml";
            case AETHER_MOBS -> "creatures/" + normalized;
            case HEPHAESTUS_ITEMS -> "items/" + normalized;
            case HEPHAESTUS_UPGRADES -> "upgrades/" + normalized;
            case HEPHAESTUS_JOBS -> "jobs.yml";
            case HEPHAESTUS_VANILLA_RECIPES -> "vanilla_recipes.yml";
            case HEPHAESTUS_JOB_RECIPES -> {
                int hash = normalized.indexOf('#');
                yield "recipes/" + (hash >= 0 ? normalized.substring(0, hash) : normalized);
            }
            case HEPHAESTUS_SHOPS -> "shops/" + normalized;
            case HECATE_CLASSES -> "classes/" + normalized;
            case HECATE_TRAITLINES -> "traitlines/" + normalized;
            case SPELLBOOK_SPELLS -> "spells/" + normalized;
            case SPELLBOOK_TRAITS -> "traits/" + normalized;
            case SPELLBOOK_EFFECTS -> "effects/" + normalized;
            case FACTIONS_BUILDINGS -> "buildings/" + normalized;
            case FACTIONS_BUILDING_TAGS -> "buildingTags.yml";
            case DAEDALUS_MODELS -> "models/" + normalized;
            case TRANSLATIONS -> normalized;
            case ANIMATIONS -> "animations/" + normalized;
            case BLOCKS -> "blocks/" + normalized;
            case REGIONS -> "regions.yml";
            case RESPAWNS -> "respawnPoints.yml";
            case EXPLORABLES -> "explorables.yml";
            case EXPLORATION_SETS -> "explorationSets.yml";
        };
    }

    public String gitPath(String itemPath) {
        String root = gitRootName();
        String relative = gitRelativePath(itemPath);
        return root.isBlank() ? relative : root + "/" + relative;
    }

    public static HermesContentType byId(String id) {
        for (HermesContentType type : values()) {
            if (type.id.equalsIgnoreCase(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown content type: " + id);
    }

    public String defaultFileName() {
        return id.toLowerCase(Locale.ROOT) + ".yml";
    }

    private static File pluginDataFolder(String pluginName, String child) {
        Plugin plugin = Bukkit.getPluginManager().getPlugin(pluginName);
        File root = plugin == null ? new File("plugins", pluginName) : plugin.getDataFolder();
        return new File(root, child);
    }
}
