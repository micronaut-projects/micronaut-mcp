package example.micronaut.moon;

/**
 * Enumeration representing the eight phases of the Moon during its lunar cycle.
 * Each phase includes a description and approximate illumination percentage.
 */
public enum MoonPhase {
    NEW_MOON("New Moon", 0, "The Moon is positioned between Earth and the Sun, appearing completely dark"),
    WAXING_CRESCENT("Waxing Crescent", 25, "A thin crescent of light appears on the right side, growing larger"),
    FIRST_QUARTER("First Quarter", 50, "Exactly half of the Moon's face is illuminated on the right side"),
    WAXING_GIBBOUS("Waxing Gibbous", 75, "More than half illuminated and growing, with a bulging shape"),
    FULL_MOON("Full Moon", 100, "The entire face of the Moon is fully illuminated"),
    WANING_GIBBOUS("Waning Gibbous", 75, "More than half illuminated but shrinking, bulging shape"),
    LAST_QUARTER("Last Quarter", 50, "Exactly half of the Moon's face is illuminated on the left side"),
    WANING_CRESCENT("Waning Crescent", 25, "A thin crescent of light appears on the left side, shrinking");

    private final String displayName;
    private final int illuminationPercentage;
    private final String description;

    /**
     * Constructor for MoonPhase enum.
     *
     * @param displayName The human-readable name of the phase
     * @param illuminationPercentage Approximate percentage of the Moon that is illuminated
     * @param description Detailed description of the phase
     */
    MoonPhase(String displayName, int illuminationPercentage, String description) {
        this.displayName = displayName;
        this.illuminationPercentage = illuminationPercentage;
        this.description = description;
    }

    /**
     * Gets the human-readable display name of the moon phase.
     *
     * @return The display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Gets the approximate illumination percentage of the moon phase.
     *
     * @return The illumination percentage (0-100)
     */
    public int getIlluminationPercentage() {
        return illuminationPercentage;
    }

    /**
     * Gets the description of the moon phase.
     *
     * @return The phase description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the next phase in the lunar cycle.
     *
     * @return The next MoonPhase
     */
    public MoonPhase getNextPhase() {
        MoonPhase[] phases = values();
        return phases[(this.ordinal() + 1) % phases.length];
    }

    /**
     * Gets the previous phase in the lunar cycle.
     *
     * @return The previous MoonPhase
     */
    public MoonPhase getPreviousPhase() {
        MoonPhase[] phases = values();
        return phases[(this.ordinal() - 1 + phases.length) % phases.length];
    }

    /**
     * Determines if this is a waxing phase (growing illumination).
     *
     * @return true if waxing, false otherwise
     */
    public boolean isWaxing() {
        return this == WAXING_CRESCENT || this == FIRST_QUARTER || this == WAXING_GIBBOUS;
    }

    /**
     * Determines if this is a waning phase (decreasing illumination).
     *
     * @return true if waning, false otherwise
     */
    public boolean isWaning() {
        return this == WANING_GIBBOUS || this == LAST_QUARTER || this == WANING_CRESCENT;
    }

    /**
     * Determines if this is a quarter phase (half illuminated).
     *
     * @return true if quarter phase, false otherwise
     */
    public boolean isQuarter() {
        return this == FIRST_QUARTER || this == LAST_QUARTER;
    }

    @Override
    public String toString() {
        return displayName + " (" + illuminationPercentage + "% illuminated)";
    }
}
