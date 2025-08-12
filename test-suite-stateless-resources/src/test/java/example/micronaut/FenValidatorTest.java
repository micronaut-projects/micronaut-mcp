package example.micronaut;

import example.micronaut.utils.FenValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FenValidatorTest {

    @Test
    void testFenValidator() {
        assertTrue(FenValidator.isValidFen("r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R w KQkq - 1 8"));
        assertFalse(FenValidator.isValidFen("r1bqk2r/ppp2ppp/2n5/1BbpP3/3Nn3/8/PPP2PPP/RNBQK2R g KQkq - 1 8"), "g is not a valid color");
    }
}
