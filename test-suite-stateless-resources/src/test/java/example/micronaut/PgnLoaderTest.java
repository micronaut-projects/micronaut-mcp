package example.micronaut;

import example.micronaut.utils.PgnLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class PgnLoaderTest {

    @Test
    void test(PgnLoader pgnLoader) {
        assertTrue(pgnLoader.loadPgn(1).isPresent());
    }
}
