package de.erethon.questsxl.common;

import de.erethon.questsxl.QuestsXL;
import de.erethon.questsxl.action.AddEventParticipationAction;
import de.erethon.questsxl.action.DelayAction;
import de.erethon.questsxl.action.DialogueAction;
import de.erethon.questsxl.action.DisplayLocationMarkerAction;
import de.erethon.questsxl.action.DummyAction;
import de.erethon.questsxl.action.GiveItemAction;
import de.erethon.questsxl.action.HideIBCAction;
import de.erethon.questsxl.action.ModifyAttributeAction;
import de.erethon.questsxl.action.ObjectiveDisplayTextAction;
import de.erethon.questsxl.action.PasteSchematicAction;
import de.erethon.questsxl.action.PermissionAction;
import de.erethon.questsxl.action.PlayAnimationAction;
import de.erethon.questsxl.action.PlayCutsceneAction;
import de.erethon.questsxl.action.PlaySoundAction;
import de.erethon.questsxl.action.QAction;
import de.erethon.questsxl.action.RemoveItemAction;
import de.erethon.questsxl.action.StartQuestAction;
import de.erethon.questsxl.action.RemoveQuestAction;
import de.erethon.questsxl.action.RepeatAction;
import de.erethon.questsxl.action.ResetIBCAction;
import de.erethon.questsxl.action.RunAsAction;
import de.erethon.questsxl.action.RunCommandAction;
import de.erethon.questsxl.action.ScoreAction;
import de.erethon.questsxl.action.SendMessageAction;
import de.erethon.questsxl.action.SendTitleAction;
import de.erethon.questsxl.action.SetBlockAction;
import de.erethon.questsxl.action.SetTrackedEventAction;
import de.erethon.questsxl.action.SetTrackedQuestAction;
import de.erethon.questsxl.action.ShowIBCAction;
import de.erethon.questsxl.action.StageAction;
import de.erethon.questsxl.action.StartEventAction;
import de.erethon.questsxl.action.StopEventAction;
import de.erethon.questsxl.action.TalkAction;
import de.erethon.questsxl.action.TeleportPlayerAction;
import de.erethon.questsxl.action.VelocityAction;
import de.erethon.questsxl.condition.ActiveQuestCondition;
import de.erethon.questsxl.condition.AttributeCondition;
import de.erethon.questsxl.condition.CompletedQuestCondition;
import de.erethon.questsxl.condition.EventRangeCondition;
import de.erethon.questsxl.condition.EventStateCondition;
import de.erethon.questsxl.condition.FireCondition;
import de.erethon.questsxl.condition.FreezingCondition;
import de.erethon.questsxl.condition.GlobalScoreCondition;
import de.erethon.questsxl.condition.GroupSizeCondition;
import de.erethon.questsxl.condition.HealthCondition;
import de.erethon.questsxl.condition.IdleCondition;
import de.erethon.questsxl.condition.InventoryCondition;
import de.erethon.questsxl.condition.InvertedCondition;
import de.erethon.questsxl.condition.ItemInHandCondition;
import de.erethon.questsxl.condition.JobCondition;
import de.erethon.questsxl.condition.LevelCondition;
import de.erethon.questsxl.condition.LocationCondition;
import de.erethon.questsxl.condition.LookingAtCondition;
import de.erethon.questsxl.condition.MinecraftTimeCondition;
import de.erethon.questsxl.condition.MountedCondition;
import de.erethon.questsxl.condition.PassengersCondition;
import de.erethon.questsxl.condition.PermissionCondition;
import de.erethon.questsxl.condition.PlayerScoreCondition;
import de.erethon.questsxl.condition.PlayersInRangeCondition;
import de.erethon.questsxl.condition.QCondition;
import de.erethon.questsxl.condition.RainCondition;
import de.erethon.questsxl.condition.RegionCondition;
import de.erethon.questsxl.condition.SneakingCondition;
import de.erethon.questsxl.condition.SprintingCondition;
import de.erethon.questsxl.condition.StageCondition;
import de.erethon.questsxl.condition.TimeCondition;
import de.erethon.questsxl.condition.VelocityCondition;
import de.erethon.questsxl.condition.WetCondition;
import de.erethon.questsxl.objective.BlockInteractObjective;
import de.erethon.questsxl.objective.BreakBlockObjective;
import de.erethon.questsxl.objective.BreedObjective;
import de.erethon.questsxl.objective.ChatObjective;
import de.erethon.questsxl.objective.CompleteEventObjective;
import de.erethon.questsxl.objective.CompleteQuestObjective;
import de.erethon.questsxl.objective.ConsumeItemObjective;
import de.erethon.questsxl.objective.DeathObjective;
import de.erethon.questsxl.objective.DropItemObjective;
import de.erethon.questsxl.objective.EnterRegionObjective;
import de.erethon.questsxl.objective.EntityInteractObjective;
import de.erethon.questsxl.objective.ExperienceObjective;
import de.erethon.questsxl.objective.FeedMobObjective;
import de.erethon.questsxl.objective.ImpossibleObjective;
import de.erethon.questsxl.objective.InstantObjective;
import de.erethon.questsxl.objective.ItemPickupObjective;
import de.erethon.questsxl.objective.ItemPlaceInContainerObjective;
import de.erethon.questsxl.objective.ItemUseObjective;
import de.erethon.questsxl.objective.JobCraftItemObjective;
import de.erethon.questsxl.objective.JumpObjective;
import de.erethon.questsxl.objective.KillPlayerObjective;
import de.erethon.questsxl.objective.LeaveRegionObjective;
import de.erethon.questsxl.objective.LocationObjective;
import de.erethon.questsxl.objective.LoginObjective;
import de.erethon.questsxl.objective.QObjective;
import de.erethon.questsxl.objective.ServerCommandObjective;
import de.erethon.questsxl.objective.SignEditObjective;
import de.erethon.questsxl.objective.SleepObjective;
import de.erethon.questsxl.objective.SneakObjective;
import de.erethon.questsxl.objective.TakeDamageObjective;
import de.erethon.questsxl.objective.TimerObjective;
import de.erethon.questsxl.objective.WaitObjective;

public class QRegistries {

    private static final QuestsXL qxl = QuestsXL.get();

    public static final QRegistry<QAction> ACTIONS = new QRegistry<>();
    public static final QRegistry<QCondition> CONDITIONS = new QRegistry<>();
    public static final QRegistry<QObjective> OBJECTIVES = new QRegistry<>();

    public static void init() {
        QuestsXL.log("Initializing QRegistries...");
        initActions();
        QuestsXL.log("Initialised " + ACTIONS.size() + " actions.");
        initConditions();
        QuestsXL.log("Initialised " + CONDITIONS.size() + " conditions.");
        initObjectives();
        QuestsXL.log("Initialised " + OBJECTIVES.size() + " objectives.");
        QuestsXL.log("Successfully initialised all QRegistries.");
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
        ACTIONS.register("play_sound", PlaySoundAction::new);
        ACTIONS.register("remove_item", RemoveItemAction::new);
        ACTIONS.register("remove_quest", RemoveQuestAction::new);
        ACTIONS.register("repeat", RepeatAction::new);
        ACTIONS.register("reset_ibc", ResetIBCAction::new);
        ACTIONS.register("run_as", RunAsAction::new);
        ACTIONS.register("command", RunCommandAction::new);
        ACTIONS.register("run_command", RunCommandAction::new); // Alias
        ACTIONS.register("score", ScoreAction::new);
        ACTIONS.register("message", SendMessageAction::new);
        ACTIONS.register("title", SendTitleAction::new);
        ACTIONS.register("set_block", SetBlockAction::new);
        ACTIONS.register("set_tracked_event", SetTrackedEventAction::new);
        ACTIONS.register("set_tracked_quest", SetTrackedQuestAction::new);
        ACTIONS.register("show_ibc", ShowIBCAction::new);
        ACTIONS.register("stage", StageAction::new);
        ACTIONS.register("start_event", StartEventAction::new);
        ACTIONS.register("start_quest", StartQuestAction::new);
        ACTIONS.register("stop_event", StopEventAction::new);
        ACTIONS.register("talk", TalkAction::new);
        ACTIONS.register("teleport", TeleportPlayerAction::new);
        ACTIONS.register("velocity", VelocityAction::new);

        // Instance system actions
        ACTIONS.register("enter_instance", de.erethon.questsxl.action.EnterInstanceAction::new);
        ACTIONS.register("exit_instance", de.erethon.questsxl.action.ExitInstanceAction::new);
        ACTIONS.register("reset_instance", de.erethon.questsxl.action.ResetInstanceAction::new);
        ACTIONS.register("set_instance_block", de.erethon.questsxl.action.SetInstanceBlockAction::new);

        if (qxl.isWEEnabled()) {
            QuestsXL.log("Found WorldEdit, enabling WorldEdit actions.");
            ACTIONS.register("paste_schematic", PasteSchematicAction::new);
        }
    }

    private static void initConditions() {
        CONDITIONS.register("active_quest", ActiveQuestCondition::new);
        CONDITIONS.register("attribute", AttributeCondition::new);
        CONDITIONS.register("completed_quest", CompletedQuestCondition::new);
        CONDITIONS.register("event_range", EventRangeCondition::new);
        CONDITIONS.register("event_state", EventStateCondition::new);
        CONDITIONS.register("fire", FireCondition::new);
        CONDITIONS.register("freezing", FreezingCondition::new);
        CONDITIONS.register("global_score", GlobalScoreCondition::new);
        CONDITIONS.register("health", HealthCondition::new);
        CONDITIONS.register("idle", IdleCondition::new);
        CONDITIONS.register("inventory_contains", InventoryCondition::new);
        CONDITIONS.register("inverted", InvertedCondition::new);
        CONDITIONS.register("item_in_hand", ItemInHandCondition::new);
        CONDITIONS.register("job", JobCondition::new);
        CONDITIONS.register("level", LevelCondition::new);
        CONDITIONS.register("location", LocationCondition::new);
        CONDITIONS.register("looking_at", LookingAtCondition::new);
        CONDITIONS.register("minecraft_time", MinecraftTimeCondition::new);
        CONDITIONS.register("mounted", MountedCondition::new);
        CONDITIONS.register("passengers", PassengersCondition::new);
        CONDITIONS.register("permission", PermissionCondition::new);
        CONDITIONS.register("player_score", PlayerScoreCondition::new);
        CONDITIONS.register("players_in_range", PlayersInRangeCondition::new);
        CONDITIONS.register("rain", RainCondition::new);
        CONDITIONS.register("region", RegionCondition::new);
        CONDITIONS.register("sneaking", SneakingCondition::new);
        CONDITIONS.register("sprinting", SprintingCondition::new);
        CONDITIONS.register("stage", StageCondition::new);
        CONDITIONS.register("time", TimeCondition::new);
        CONDITIONS.register("velocity", VelocityCondition::new);
        CONDITIONS.register("wet", WetCondition::new);

        // Instance system conditions
        CONDITIONS.register("in_instance", de.erethon.questsxl.condition.InInstanceCondition::new);
        CONDITIONS.register("instance_block", de.erethon.questsxl.condition.InstanceBlockCondition::new);

        if (qxl.isAergiaEnabled()) {
            QuestsXL.log("Found Aergia, enabling Aergia conditions.");
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
        OBJECTIVES.register("complete_event", CompleteEventObjective::new);
        OBJECTIVES.register("complete_quest", CompleteQuestObjective::new);
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
        OBJECTIVES.register("timer", TimerObjective::new);
        OBJECTIVES.register("wait", WaitObjective::new);
        if (qxl.isHephaestusEnabled()) {
            QuestsXL.log("Found Hephaestus, enabling Hephaestus objectives.");
            OBJECTIVES.register("consume_item", ConsumeItemObjective::new);
            OBJECTIVES.register("break_block", BreakBlockObjective::new);
            OBJECTIVES.register("job_craft_item", JobCraftItemObjective::new);
            OBJECTIVES.register("drop_item", DropItemObjective::new);
            OBJECTIVES.register("pickup_item", ItemPickupObjective::new);
            OBJECTIVES.register("place_item", ItemPlaceInContainerObjective::new);
            OBJECTIVES.register("use_item", ItemUseObjective::new);
        }
    }
}
