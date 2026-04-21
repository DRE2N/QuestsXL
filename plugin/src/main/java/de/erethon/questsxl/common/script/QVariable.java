package de.erethon.questsxl.common.script;

/**
 * A runtime variable with a String or Number value.
 * Both types are stored as a String internally; numeric conversion is lossy
 */
public class QVariable {

    private final String value;

    public QVariable(String value) {
        this.value = value == null ? "" : value;
    }

    public QVariable(double value) {
        // Strip trailing ".0" for whole numbers so %group_size% reads "3" not "3.0"
        if (value == Math.floor(value) && !Double.isInfinite(value)) {
            this.value = String.valueOf((long) value);
        } else {
            this.value = String.valueOf(value);
        }
    }

    public QVariable(long value) {
        this.value = String.valueOf(value);
    }

    public QVariable(int value) {
        this.value = String.valueOf(value);
    }

    /** Returns the raw string value. */
    public String asString() {
        return value;
    }

    /**
     * Parses the value as a double.
     * Returns 0.0 and logs a debug message if parsing fails.
     */
    public double asNumber() {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    @Override
    public String toString() {
        return value;
    }
}

