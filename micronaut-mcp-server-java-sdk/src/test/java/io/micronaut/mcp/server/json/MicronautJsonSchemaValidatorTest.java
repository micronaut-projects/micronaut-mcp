package io.micronaut.mcp.server.json;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest(startApplication = false)
class MicronautJsonSchemaValidatorTest {
    private static final Map<String, Object> SCHEMA = Map.of(
        "type", "object",
        "properties", Map.of("name", Map.of("type", "string"), "age", Map.of("type", "integer")),
        "required", List.of("name", "age"));

    @Inject
    JsonSchemaValidator validator;

    @Test
    void validContentIsSerializedOnce() {
        JsonSchemaValidator.ValidationResponse response = validator.validate(SCHEMA, Map.of("name", "Alice", "age", 30));
        assertTrue(response.valid());
        assertTrue(response.jsonStructuredOutput().contains("\"name\":\"Alice\""), response.jsonStructuredOutput());
    }

    @Test
    void everyViolationIsReported() {
        JsonSchemaValidator.ValidationResponse response = validator.validate(SCHEMA, Map.of("age", "thirty"));
        assertFalse(response.valid());
        assertTrue(response.errorMessage().contains("name") && response.errorMessage().contains("age")
            && response.errorMessage().contains("; "), response.errorMessage());
    }

    @Test
    void theSchemaAndTheContentAreRequired() {
        assertEquals("Schema must not be null",
            assertThrows(IllegalArgumentException.class, () -> validator.validate(null, Map.of())).getMessage());
        assertEquals("Structured content must not be null",
            assertThrows(IllegalArgumentException.class, () -> validator.validate(SCHEMA, null)).getMessage());
    }
}
