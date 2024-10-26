package de.erethon.questsxl.livingworld;

public enum EventState {
    ACTIVE, // This event is currently running and progressing its stages
    COMPLETED, // This event has been completed and is awaiting its cooldown
    NOT_STARTED, // The cooldown has expired and the event is ready to be started when all conditions are met
    DISABLED // This event is disabled and will not be started, ever
}
