package de.erethon.questsxl.action;

import java.util.function.Supplier;

public enum Action {

    ANIMATION(PlayAnimationAction::new),
    COMMAND(RunCommandAction::new),
    CUTSCENE(PlayCutsceneAction::new),
    DELAY(DelayAction::new),
    DIALOGUE(DialogueAction::new),
    EVENT_PARTICIPATION(AddEventParticipation::new),
    GIVE_ITEM(GiveItemAction::new),
    HIDE_IBC(HideIBCAction::new),
    JOB_EXP(JobExpAction::new),
    MESSAGE(SendMessageAction::new),
    MOB_FOLLOW_PLAYER(MobFollowPlayerAction::new),
    PASTE_SCHEMATIC(PasteSchematicAction.class), // debug
    PERMISSION(PermissionAction::new),
    REPEAT(RepeatAction::new),
    RESET_IBC(ResetIBCAction::new),
    RUN_AS(RunAsAction::new),
    SHOW_BEAM(DisplayLocationMarkerAction::new),
    SHOW_IBC(ShowIBCAction::new),
    SPAWNER(SpawnerAction::new),
    SPAWN_MOB(SpawnMobAction::new),
    STAGE(StageAction::new),
    START_EVENT(StartEventAction::new),
    START_QUEST(QuestAction::new),
    TELEPORT(TeleportPlayerAction::new),
    TITLE(SendTitleAction::new);

    private final Supplier<QAction> supplier;

    Action(Supplier<QAction> supplier) {
        this.supplier = supplier;
    }

    Action(Class<? extends QAction> clazz) { // for debugging reason (ugly)
        supplier = () -> {
            try {
                return clazz.newInstance();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        };
    }

    public QAction newInstance() {
        return supplier.get();
    }
}
