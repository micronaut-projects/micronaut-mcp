/*
 * Copyright 2025 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package example.micronaut.moon;

import jakarta.inject.Singleton;

import java.time.LocalDate;

@Singleton
public class MoonPhasesService {

    // Eccentricity of Earth's orbit
    private static final Double ECCENTRICITY_EARTH_ORBIT = 0.016718;
    // Ecliptic longitude of the Sun at epoch 1980.0
    private static final Double SUN_ECLIPTIC_LONGITUDE_EPOCH = 278.833540;
    // Ecliptic longitude of the Sun at perigee
    private static final Double SUN_ECLIPTIC_LONGITUDE_PERIGEE = 282.596403;
    private static final Double TO_RADIANS = Math.PI / 180.0;
    public static final int SECONDS_IN_A_DAY = 86400;

    /**
     * Normalizes an angle to the range [0, 360) degrees.
     *
     * @param angleDegrees The angle in degrees.
     * @return The normalized angle in degrees.
     */
    private static double fixAngle(double angleDegrees) {
        return ((angleDegrees % 360.0) + 360.0) % 360.0;
    }

    /**
     * Calculates the moon's phase angle in radians.
     * The phase angle is the elongation of the Moon from the Sun in the ecliptic.
     * 0 radians: New Moon
     * PI/2 radians (90 degrees): First Quarter
     * PI radians (180 degrees): Full Moon
     * 3*PI/2 radians (270 degrees): Last Quarter
     * <p>
     * Algorithm translated from: <a href="https://github.com/oliverkwebb/moonphase">oliverkwebb/moonphase</a>
     *
     * @param unixTimestamp The current time as a Unix timestamp (seconds since 1970-01-01T00:00:00Z).
     * @return The moon's phase angle in radians. Can be outside the [0, 2*PI) range if not subsequently normalized.
     */
    private static double calculateMoonPhaseRadians(long unixTimestamp) {
        // Convert Unix timestamp to Julian Day, then to days since epoch 1980.0
        // Julian Day for 1970-01-01 00:00:00 UTC is 2440587.5
        // Julian Day for 1980-01-01 00:00:00 UTC is 2444238.5
        double daysSinceEpoch1980 = (unixTimestamp / 86400.0 + 2440587.5) - 2444238.5;

        // Sun's position calculations
        // Sun's mean anomaly (M)
        double sunMeanAnomalyRad = TO_RADIANS * fixAngle(((360.0 / 365.2422) * daysSinceEpoch1980) + SUN_ECLIPTIC_LONGITUDE_EPOCH - SUN_ECLIPTIC_LONGITUDE_PERIGEE);

        // Solve Kepler's equation for eccentric anomaly (e)
        // E - ecc*sin(E) = M  => delta = E - ecc*sin(E) - M  E_new = E - delta / (1 - ecc*cos(E))
        double eccentricAnomalyRad = sunMeanAnomalyRad; // Initial guess
        double deltaRad;
        do {
            deltaRad = eccentricAnomalyRad - ECCENTRICITY_EARTH_ORBIT * Math.sin(eccentricAnomalyRad) - sunMeanAnomalyRad;
            eccentricAnomalyRad = eccentricAnomalyRad - deltaRad / (1.0 - ECCENTRICITY_EARTH_ORBIT * Math.cos(eccentricAnomalyRad));
        } while (Math.abs(deltaRad) > 1E-6);

        // Sun's true anomaly
        double sunTrueAnomalyRad = 2.0 * Math.atan(Math.sqrt((1.0 + ECCENTRICITY_EARTH_ORBIT) / (1.0 - ECCENTRICITY_EARTH_ORBIT)) * Math.tan(eccentricAnomalyRad / 2.0));

        // Sun's geocentric ecliptic longitude (Lambdasun)
        double sunLongitudeDeg = fixAngle((sunTrueAnomalyRad * (180.0 / Math.PI)) + SUN_ECLIPTIC_LONGITUDE_PERIGEE);

        // Moon's position calculations
        // Moon's mean longitude (ml)
        double moonMeanLongitudeDeg = fixAngle(13.1763966 * daysSinceEpoch1980 + 64.975464);
        // Moon's mean anomaly (MM)
        double moonMeanAnomalyDeg = fixAngle(moonMeanLongitudeDeg - 0.1114041 * daysSinceEpoch1980 - 349.383063);

        // Evection
        double evectionDeg = 1.2739 * Math.sin(TO_RADIANS * (2.0 * (moonMeanLongitudeDeg - sunLongitudeDeg) - moonMeanAnomalyDeg));
        // Annual equation - perturbation due to Sun's varying apparent speed
        double annualEquationDeg = 0.1858 * Math.sin(sunMeanAnomalyRad);
        // Corrected moon's anomaly
        double correctedMoonAnomalyRad = TO_RADIANS * (moonMeanAnomalyDeg + evectionDeg - annualEquationDeg - (0.37 * Math.sin(sunMeanAnomalyRad)));

        // Corrected moon's longitude including equation of center for Moon
        double correctedMoonLongitudeDeg = moonMeanLongitudeDeg + evectionDeg + (6.2886 * Math.sin(correctedMoonAnomalyRad)) - annualEquationDeg + (0.214 * Math.sin(2.0 * correctedMoonAnomalyRad));
        // Further correction to moon's true longitude
        double moonTrueLongitudeDeg = correctedMoonLongitudeDeg + (0.6583 * Math.sin(TO_RADIANS * (2.0 * (correctedMoonLongitudeDeg - sunLongitudeDeg))));

        // Age of the Moon in degrees (phase angle)
        // This is (Moon's True Longitude - Sun's True Longitude)
        double moonAgeDegrees = moonTrueLongitudeDeg - sunLongitudeDeg;

        return moonAgeDegrees * TO_RADIANS; // Return phase angle in radians
    }

    public MoonPhaseEmoji currentMoonPhase() {
        return moonPhaseAtUnixTimestamp(System.currentTimeMillis() / 1000L);
    }

    public MoonPhaseEmoji moonPhaseAtDate(LocalDate localDate) {
        return moonPhaseAtUnixTimestamp(localDate.toEpochDay() * SECONDS_IN_A_DAY);
    }

    public MoonPhaseEmoji moonPhaseAtUnixTimestamp(long timeSeconds) {
        double moonPhaseRadians = calculateMoonPhaseRadians(timeSeconds);
        double moonPhaseDegrees = moonPhaseRadians * (180.0 / Math.PI);

        // Normalize degrees to [0, 360) for interpretation
        double normalizedMoonPhaseDegrees = fixAngle(moonPhaseDegrees);

        // Approximate interpretation of the phase
        return getMoonPhaseEmoji(normalizedMoonPhaseDegrees);
    }

    private static MoonPhaseEmoji getMoonPhaseEmoji(double normalizedMoonPhaseDegrees) {
        if (normalizedMoonPhaseDegrees < 22.5) { // Includes cases where normalized is close to 360
            return new MoonPhaseEmoji(MoonPhase.NEW_MOON, "🌑");
        } else if (normalizedMoonPhaseDegrees < 67.5) {
            return new MoonPhaseEmoji(MoonPhase.WAXING_CRESCENT, "🌒");
        } else if (normalizedMoonPhaseDegrees < 112.5) {
            return new MoonPhaseEmoji(MoonPhase.FIRST_QUARTER, "🌓");
        } else if (normalizedMoonPhaseDegrees < 157.5) {
            return new MoonPhaseEmoji(MoonPhase.WAXING_GIBBOUS, "🌔");
        } else if (normalizedMoonPhaseDegrees < 202.5) {
            return new MoonPhaseEmoji(MoonPhase.FULL_MOON, "🌕");
        } else if (normalizedMoonPhaseDegrees < 247.5) {
            return new MoonPhaseEmoji(MoonPhase.WANING_GIBBOUS, "🌖");
        } else if (normalizedMoonPhaseDegrees < 292.5) {
            return new MoonPhaseEmoji(MoonPhase.LAST_QUARTER, "🌗");
        } else if (normalizedMoonPhaseDegrees < 337.5) {
            return new MoonPhaseEmoji(MoonPhase.WANING_CRESCENT, "🌘");
        } else { // Between 337.5 and 360
            return new MoonPhaseEmoji(MoonPhase.NEW_MOON, "🌑");
        }
    }
}
