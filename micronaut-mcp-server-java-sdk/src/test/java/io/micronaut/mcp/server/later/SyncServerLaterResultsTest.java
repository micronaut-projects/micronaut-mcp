package io.micronaut.mcp.server.later;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "LaterResultsTest")
@MicronautTest
class SyncServerLaterResultsTest extends AbstractLaterResultsTest {

    @Test
    void aFutureThatFailsIsAnsweredAsAMethodThatThrows() throws JSONException {
        JSONAssert.assertEquals(failure("fails"), failure("futureFails"), true);
    }
}
