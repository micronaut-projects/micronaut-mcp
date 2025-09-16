package example.micronaut.moon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoonPhaseTest {

    @Test
    void getters_returnExpectedValues() {
        MoonPhase full = MoonPhase.FULL_MOON;
        assertEquals("Full Moon", full.getDisplayName());
        assertEquals(100, full.getIlluminationPercentage());
        assertTrue(full.getDescription().toLowerCase().contains("fully illuminated"));

        MoonPhase firstQuarter = MoonPhase.FIRST_QUARTER;
        assertEquals("First Quarter", firstQuarter.getDisplayName());
        assertEquals(50, firstQuarter.getIlluminationPercentage());
        assertTrue(firstQuarter.getDescription().toLowerCase().contains("half"));
    }

    @Test
    void next_and_previous_wrapAroundCorrectly() {
        assertEquals(MoonPhase.WAXING_CRESCENT, MoonPhase.NEW_MOON.getNextPhase());
        assertEquals(MoonPhase.NEW_MOON, MoonPhase.WANING_CRESCENT.getNextPhase());

        assertEquals(MoonPhase.WANING_CRESCENT, MoonPhase.NEW_MOON.getPreviousPhase());
        assertEquals(MoonPhase.NEW_MOON, MoonPhase.WAXING_CRESCENT.getPreviousPhase());
    }

    @Test
    void predicates_identifyWaxingWaningAndQuarter() {
        // Waxing
        assertTrue(MoonPhase.WAXING_CRESCENT.isWaxing());
        assertTrue(MoonPhase.FIRST_QUARTER.isWaxing());
        assertTrue(MoonPhase.WAXING_GIBBOUS.isWaxing());
        assertFalse(MoonPhase.NEW_MOON.isWaxing());
        assertFalse(MoonPhase.FULL_MOON.isWaxing());
        assertFalse(MoonPhase.WANING_GIBBOUS.isWaxing());

        // Waning
        assertTrue(MoonPhase.WANING_GIBBOUS.isWaning());
        assertTrue(MoonPhase.LAST_QUARTER.isWaning());
        assertTrue(MoonPhase.WANING_CRESCENT.isWaning());
        assertFalse(MoonPhase.NEW_MOON.isWaning());
        assertFalse(MoonPhase.FULL_MOON.isWaning());
        assertFalse(MoonPhase.WAXING_CRESCENT.isWaning());

        // Quarter
        assertTrue(MoonPhase.FIRST_QUARTER.isQuarter());
        assertTrue(MoonPhase.LAST_QUARTER.isQuarter());
        assertFalse(MoonPhase.NEW_MOON.isQuarter());
        assertFalse(MoonPhase.FULL_MOON.isQuarter());
    }

    @Test
    void toString_returnsFriendlySummary() {
        assertEquals("Full Moon (100% illuminated)", MoonPhase.FULL_MOON.toString());
        assertEquals("New Moon (0% illuminated)", MoonPhase.NEW_MOON.toString());
    }
}

