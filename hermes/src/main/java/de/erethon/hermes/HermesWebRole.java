package de.erethon.hermes;

public enum HermesWebRole {
    NONE,
    VIEWER,
    EDITOR,
    ADMIN;

    public boolean canView() {
        return this == VIEWER || this == EDITOR || this == ADMIN;
    }

    public boolean canEdit() {
        return this == EDITOR || this == ADMIN;
    }

    public boolean canAdmin() {
        return this == ADMIN;
    }

    public static HermesWebRole fromDatabase(String value) {
        if (value == null || value.isBlank()) {
            return EDITOR;
        }
        try {
            return HermesWebRole.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ignored) {
            return EDITOR;
        }
    }
}
