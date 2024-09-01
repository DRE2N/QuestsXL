package de.erethon.questsxl.common;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.AddEventParticipation;
import de.erethon.questsxl.action.*;
import de.erethon.questsxl.condition.ActiveQuestCondition;
import de.erethon.questsxl.condition.*;
import de.erethon.questsxl.objective.*;

public class QRegistries {

    private static final QuestsXL qxl = QuestsXL.getInstance();

    public static final QRegistry<QAction> ACTIONS = new QRegistry<>();
    public static final QRegistry<QCondition> CONDITIONS = new QRegistry<>();
    public static final QRegistry<QObjective> OBJECTIVES = new QRegistry<>();

    public static void init() {
        MessageUtil.log("Initializing QRegistries...");
        initActions();
        MessageUtil.log("Initialised " + ACTIONS.size() + " actions.");
        initConditions();
        MessageUtil.log("Initialised " + CONDITIONS.size() + " conditions.");
        initObjectives();
        MessageUtil.log("Initialised " + OBJECTIVES.size() + " objectives.");
        MessageUtil.log("Successfully initialised all QRegistries.");
    }

    private static void initActions() {
        ACTIONS.register("event_participation", AddEventParticipation::new);
        ACTIONS.register("delay", DelayAction::new);
        ACTIONS.register("dialogue", DialogueAction::new);
        ACTIONS.register("display_marker", DisplayLocationMarkerAction::new);
        ACTIONS.register("dummy", DummyAction::new);
        ACTIONS.register("give_item", GiveItemAction::new);
        ACTIONS.register("hide_ibc", HideIBCAction::new);
        ACTIONS.register("objective_display", ObjectiveDisplayTextAction::new);
        ACTIONS.register("permission", PermissionAction::new);
        ACTIONS.register("play_animation", PlayAnimationAction::new);
        ACTIONS.register("play_cutscene", PlayCutsceneAction::new);
        ACTIONS.register("quest", QuestAction::new);
        ACTIONS.register("repeat", RepeatAction::new);
        ACTIONS.register("reset_ibc", ResetIBCAction::new);
        ACTIONS.register("run_as", RunAsAction::new);
        ACTIONS.register("command", RunCommandAction::new);
        ACTIONS.register("score", ScoreAction::new);
        ACTIONS.register("message", SendMessageAction::new);
        ACTIONS.register("title", SendTitleAction::new);
        ACTIONS.register("show_ibc", ShowIBCAction::new);
        ACTIONS.register("stage", StageAction::new);
        ACTIONS.register("start_event", StartEventAction::new);
        ACTIONS.register("talk", TalkAction::new);
        ACTIONS.register("teleport", TeleportPlayerAction::new);
        if (qxl.isWEEnabled()) {
            MessageUtil.log("Found WorldEdit, enabling WorldEdit actions.");
            ACTIONS.register("paste_schematic", PasteSchematicAction::new);
        }
        if (qxl.isAetherEnabled()) {
            MessageUtil.log("Found Aether, enabling Aether actions.");
            ACTIONS.register("spawn_mob", SpawnMobAction::new);
            ACTIONS.register("mob_follow", MobFollowPlayerAction::new);
            ACTIONS.register("spawner", SpawnerAction::new);
        }
        if (qxl.isAetherEnabled()) {
            MessageUtil.log("Found JobsXL, enabling JobsXL actions.");
            ACTIONS.register("job_exp", JobExpAction::new);
        }
    }

    private static void initConditions() {
        CONDITIONS.register("active_quest", ActiveQuestCondition::new);
        CONDITIONS.register("completed_quest", CompletedQuestCondition::new);
        CONDITIONS.register("event_state", EventStateCondition::new);
        CONDITIONS.register("global_score", GlobalScoreCondition::new);
        CONDITIONS.register("inventory_contains", InventoryCondition::new);
        CONDITIONS.register("inverted", InvertedCondition::new);
        CONDITIONS.register("item_in_hand", ItemInHandCondition::new);
        CONDITIONS.register("level", LevelCondition::new);
        CONDITIONS.register("location", LocationCondition::new);
        CONDITIONS.register("looking_at", LookingAtCondition::new);
        CONDITIONS.register("permission", PermissionCondition::new);
        CONDITIONS.register("player_score", PlayerScoreCondition::new);
        CONDITIONS.register("region", RegionCondition::new);
        CONDITIONS.register("time", TimeCondition::new);
        if (qxl.isAergiaEnabled()) {
            MessageUtil.log("Found Aergia, enabling Aergia conditions.");
            CONDITIONS.register("group_size", GroupSizeCondition::new);
        }
        if (qxl.isJXLEnabled()) {
            MessageUtil.log("Found JobsXL, enabling JobsXL conditions.");
            CONDITIONS.register("job_level", JobLevelCondition::new);
        }
    }

    private static void initObjectives() {
        OBJECTIVES.register("block_interact", BlockInteractObjective::new);
        OBJECTIVES.register("death", DeathObjective::new);
        OBJECTIVES.register("enter_region", EnterRegionObjective::new);
        OBJECTIVES.register("entity_interact", EntityInteractObjective::new);
        OBJECTIVES.register("experience", ExperienceObjective::new);
        OBJECTIVES.register("feed_mob", FeedMobObjective::new);
        OBJECTIVES.register("impossible", ImpossibleObjective::new);
        OBJECTIVES.register("instant", InstantObjective::new);
        OBJECTIVES.register("kill_player", KillPlayerObjective::new);
        OBJECTIVES.register("leave_region", LeaveRegionObjective::new);
        OBJECTIVES.register("location", LocationObjective::new);
        OBJECTIVES.register("mythic_mob", MythicMobObjective::new);
        OBJECTIVES.register("server_command", ServerCommandObjective::new);
        OBJECTIVES.register("take_damage", TakeDamageObjective::new);
        OBJECTIVES.register("wait", WaitObjective::new);
        if (qxl.isAetherEnabled()) {
            MessageUtil.log("Found Aether, enabling Aether objectives.");
            OBJECTIVES.register("escort_mob", EscortNPCObjective::new);
            OBJECTIVES.register("kill_mob", KillMobObjective::new);
        }
        if (qxl.isJXLEnabled()) {
            MessageUtil.log("Found JobsXL, enabling JobsXL objectives.");
            OBJECTIVES.register("craft", CraftObjective::new);
        }
        if (qxl.isHephaestusEnabled()) {
            MessageUtil.log("Found Hephaestus, enabling Hephaestus objectives.");
            OBJECTIVES.register("consume_item", ConsumeItemObjective::new);
            OBJECTIVES.register("drop_item", DropItemObjective::new);
            OBJECTIVES.register("item_pickup", ItemPickupObjective::new);
            OBJECTIVES.register("place_item", PlaceItemInContainerObjective::new);
            OBJECTIVES.register("use_item", UseItemObjective::new);
        }
    }
}
