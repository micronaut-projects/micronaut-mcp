package io.micronaut.mcp.server.json;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest(startApplication = false)
class MicronautJsonSchemaValidatorCacheTest {

    @Inject
    JsonSchemaValidator validator;

    @Test
    void schemasAreCachedByIdentity() {
        Map<String, Object> schema = new HashMap<>(Map.of(
            "type", "object",
            "properties", Map.of("name", Map.of("type", "string")),
            "required", List.of("name")));
        assertTrue(validator.validate(schema, Map.of("name", "Alice")).valid());
        assertFalse(validator.validate(schema, Map.of()).valid());

        // Another schema instance gets its own entry, and the first one is unaffected
        Map<String, Object> otherSchema = new HashMap<>(schema);
        otherSchema.put("required", List.of());
        assertTrue(validator.validate(otherSchema, Map.of()).valid());
        assertFalse(validator.validate(schema, Map.of()).valid());
    }
}
