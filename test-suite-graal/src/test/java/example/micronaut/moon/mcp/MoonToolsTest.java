package example.micronaut.moon.mcp;

import example.micronaut.moon.MoonPhase;
import example.micronaut.moon.MoonPhaseEmoji;
import example.micronaut.moon.MoonPhaseRequest;
import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "moon.enabled", value = StringUtils.TRUE)
@MicronautTest(startApplication = false)
class MoonToolsTest {

    @Test
    void moonPhaseAtSergioBirthday(MoonTools server) {
        MoonPhaseEmoji moonPhaseEmoji = server.moonPhaseAtDate(new MoonPhaseRequest(LocalDate.of(1982, 10, 28)));
        assertEquals(MoonPhase.WAXING_GIBBOUS, moonPhaseEmoji.phase());
    }

    @Test
    void currentMoonPhase(MoonTools server) {
        assertDoesNotThrow(server::currentMoonPhase);
    }


}
