package example.micronaut.weather;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LocationTest {
    private final double guadalajaraLatitude = 40.6331;
    private final double guadalajaraLongitude = -3.1601;

    @Test
    void locationOf() {
        assertEquals(new Location(guadalajaraLatitude, guadalajaraLongitude),
            Location.of(Map.of("latitude", guadalajaraLatitude, "longitude", guadalajaraLongitude)).get());
        assertTrue(Location.of(Map.of("latitude", guadalajaraLatitude)).isEmpty());
        assertTrue(Location.of(Map.of("longitude", guadalajaraLongitude)).isEmpty());
        assertTrue(Location.of(Map.of("latitude", guadalajaraLatitude, "longitude", "foobar")).isEmpty());
        assertTrue(Location.of(Map.of("latitude", "foobar", "longitude", guadalajaraLongitude)).isEmpty());

    }
}
