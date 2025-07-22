package example.micronaut;

import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@MicronautTest(startApplication = false)
class JsonSchemaValidationTest {

    @Test
    void jsonSchemaValidation(JsonMapper jsonMapper) {
        assertDoesNotThrow(() -> jsonMapper.readValue(ForecastTools.ALERTS_SCHEMA, McpSchema.JsonSchema.class));
    }
}
