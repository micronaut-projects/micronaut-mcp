package io.micronaut.mcp.primitives.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;

class StringInterpolatorTest {

    @Test
    void stringInterpolationWithMap() {
        String result = StringInterpolator.interpolate("Name: ${firstName} Last name: ${lastName} Complete name: ${firstName} ${lastName}",
            Map.of("firstName", "Sergio", "lastName", "del Amo"));
        String expected = "Name: Sergio Last name: del Amo Complete name: Sergio del Amo";
        assertEquals(expected, result);
    }
}
