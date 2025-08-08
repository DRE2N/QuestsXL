package de.erethon.questsxl.common;

import de.erethon.bedrock.chat.MessageUtil;
import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.AddEventParticipationAction;
import de.erethon.questsxl.action.*;
import de.erethon.questsxl.condition.ActiveQuestCondition;
import de.erethon.questsxl.condition.*;
import de.erethon.questsxl.objective.*;

public class QRegistries {

    private static final QuestsXL qxl = QuestsXL.get();

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
        ACTIONS.register("event_participation", AddEventParticipationAction::new);
        ACTIONS.register("delay", DelayAction::new);
        ACTIONS.register("display_marker", DisplayLocationMarkerAction::new);
        ACTIONS.register("dummy", DummyAction::new);
        ACTIONS.register("give_item", GiveItemAction::new);
        ACTIONS.register("hide_ibc", HideIBCAction::new);
        ACTIONS.register("modify_attribute", ModifyAttributeAction::new);
        ACTIONS.register("objective_display", ObjectiveDisplayTextAction::new);
        ACTIONS.register("permission", PermissionAction::new);
        ACTIONS.register("play_animation", PlayAnimationAction::new);
        ACTIONS.register("play_dialogue", DialogueAction::new);
        ACTIONS.register("play_cutscene", PlayCutsceneAction::new);
        ACTIONS.register("repeat", RepeatAction::new);
        ACTIONS.register("reset_ibc", ResetIBCAction::new);
        ACTIONS.register("run_as", RunAsAction::new);
        ACTIONS.register("command", RunCommandAction::new);
        ACTIONS.register("score", ScoreAction::new);
        ACTIONS.register("message", SendMessageAction::new);
        ACTIONS.register("title", SendTitleAction::new);
        ACTIONS.register("set_tracked_event", SetTrackedEventAction::new);
        ACTIONS.register("set_tracked_quest", SetTrackedQuestAction::new);
        ACTIONS.register("show_ibc", ShowIBCAction::new);
        ACTIONS.register("stage", StageAction::new);
        ACTIONS.register("start_event", StartEventAction::new);
        ACTIONS.register("start_quest", QuestAction::new);
        ACTIONS.register("talk", TalkAction::new);
        ACTIONS.register("teleport", TeleportPlayerAction::new);
        ACTIONS.register("velocity", VelocityAction::new);
        if (qxl.isWEEnabled()) {
            MessageUtil.log("Found WorldEdit, enabling WorldEdit actions.");
            ACTIONS.register("paste_schematic", PasteSchematicAction::new);
        }
    }

    private static void initConditions() {
        CONDITIONS.register("active_quest", ActiveQuestCondition::new);
        CONDITIONS.register("attribute", AttributeCondition::new);
        CONDITIONS.register("completed_quest", CompletedQuestCondition::new);
        CONDITIONS.register("event_state", EventStateCondition::new);
        CONDITIONS.register("fire", FireCondition::new);
        CONDITIONS.register("freezing", FreezingCondition::new);
        CONDITIONS.register("global_score", GlobalScoreCondition::new);
        CONDITIONS.register("health", HealthCondition::new);
        CONDITIONS.register("idle", IdleCondition::new);
        CONDITIONS.register("inventory_contains", InventoryCondition::new);
        CONDITIONS.register("inverted", InvertedCondition::new);
        CONDITIONS.register("item_in_hand", ItemInHandCondition::new);
        CONDITIONS.register("level", LevelCondition::new);
        CONDITIONS.register("location", LocationCondition::new);
        CONDITIONS.register("looking_at", LookingAtCondition::new);
        CONDITIONS.register("mounted", MountedCondition::new);
        CONDITIONS.register("passengers", PassengersCondition::new);
        CONDITIONS.register("permission", PermissionCondition::new);
        CONDITIONS.register("player_score", PlayerScoreCondition::new);
        CONDITIONS.register("players_in_range", PlayersInRangeCondition::new);
        CONDITIONS.register("rain", RainCondition::new);
        CONDITIONS.register("region", RegionCondition::new);
        CONDITIONS.register("sneaking", SneakingCondition::new);
        CONDITIONS.register("sprinting", SprintingCondition::new);
        CONDITIONS.register("time", TimeCondition::new);
        CONDITIONS.register("velocity", VelocityCondition::new);
        CONDITIONS.register("wet", WetCondition::new);
        if (qxl.isAergiaEnabled()) {
            MessageUtil.log("Found Aergia, enabling Aergia conditions.");
            CONDITIONS.register("group_size", GroupSizeCondition::new);
        }
    }

    private static void initObjectives() {
        OBJECTIVES.register("block_interact", BlockInteractObjective::new);
        OBJECTIVES.register("breed", BreedObjective::new);
        OBJECTIVES.register("chat", ChatObjective::new);
        OBJECTIVES.register("death", DeathObjective::new);
        OBJECTIVES.register("enter_region", EnterRegionObjective::new);
        OBJECTIVES.register("entity_interact", EntityInteractObjective::new);
        OBJECTIVES.register("experience", ExperienceObjective::new);
        OBJECTIVES.register("feed_mob", FeedMobObjective::new);
        OBJECTIVES.register("impossible", ImpossibleObjective::new);
        OBJECTIVES.register("instant", InstantObjective::new);
        OBJECTIVES.register("kill_player", KillPlayerObjective::new);
        OBJECTIVES.register("jump", JumpObjective::new);
        OBJECTIVES.register("leave_region", LeaveRegionObjective::new);
        OBJECTIVES.register("location", LocationObjective::new);
        OBJECTIVES.register("login", LoginObjective::new);
        OBJECTIVES.register("server_command", ServerCommandObjective::new);
        OBJECTIVES.register("sign_edit", SignEditObjective::new);
        OBJECTIVES.register("sleep", SleepObjective::new);
        OBJECTIVES.register("sneak", SneakObjective::new);
        OBJECTIVES.register("take_damage", TakeDamageObjective::new);
        OBJECTIVES.register("wait", WaitObjective::new);
        if (qxl.isHephaestusEnabled()) {
            MessageUtil.log("Found Hephaestus, enabling Hephaestus objectives.");
            OBJECTIVES.register("consume_item", ConsumeItemObjective::new);
            OBJECTIVES.register("drop_item", DropItemObjective::new);
            OBJECTIVES.register("pickup_item", ItemPickupObjective::new);
            OBJECTIVES.register("place_item", ItemPlaceInContainerObjective::new);
            OBJECTIVES.register("use_item", ItemUseObjective::new);
        }
    }
}
