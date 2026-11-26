package io.micronaut.mcp.server.json;

import io.micronaut.context.annotation.Property;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.schema.JsonSchemaValidator;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest(startApplication = false)
class MicronautJsonSchemaValidatorCacheTest {

    @Inject
    JsonSchemaValidator validator;

    @Test
    void eachSchemaInstanceValidatesAgainstItsOwnJson() {
        Map<String, Object> schema = requiredNameSchema();
        assertTrue(validator.validate(schema, Map.of("name", "Alice")).valid());
        assertFalse(validator.validate(schema, Map.of()).valid());

        // Another schema instance gets its own entry, and the first one is unaffected
        Map<String, Object> otherSchema = new HashMap<>(schema);
        otherSchema.put("required", List.of());
        assertTrue(validator.validate(otherSchema, Map.of()).valid());
        assertFalse(validator.validate(schema, Map.of()).valid());
    }

    @Test
    void schemaJsonIsCachedByIdentity() {
        Map<String, Object> schema = requiredNameSchema();
        assertFalse(validator.validate(schema, Map.of()).valid());

        // Mutating a schema Map after its first validation is not supported; it is done here only to observe the cache.
        // The cached JSON of the mutated instance is used, while an equal but distinct instance gets its own JSON.
        schema.put("required", List.of());
        assertFalse(validator.validate(schema, Map.of()).valid());
        assertTrue(validator.validate(new HashMap<>(schema), Map.of()).valid());
    }

    @Test
    void schemasBeyondTheCacheCapacityValidateConcurrently() throws Exception {
        List<Map<String, Object>> schemas = IntStream.range(0, 600).mapToObj(i -> requiredNameSchema()).toList();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<Boolean>> results = new ArrayList<>();
            for (int thread = 0; thread < 8; thread++) {
                results.add(executor.submit(() -> schemas.stream().allMatch(schema ->
                    validator.validate(schema, Map.of("name", "Alice")).valid() && !validator.validate(schema, Map.of()).valid())));
            }
            for (Future<Boolean> result : results) {
                assertTrue(result.get(1, TimeUnit.MINUTES));
            }
        } finally {
            executor.shutdownNow();
        }
        // The first schemas were evicted, and they are serialized again
        assertFalse(validator.validate(schemas.getFirst(), Map.of()).valid());
    }

    @Test
    void schemaThatCannotBeSerializedIsInvalid() {
        Map<String, Object> schema = Map.of("type", "object", "default", new Unserializable());
        for (int i = 0; i < 2; i++) {
            // Nothing is cached for the schema, so the error is reported on every validation
            JsonSchemaValidator.ValidationResponse response = validator.validate(schema, Map.of());
            assertFalse(response.valid());
            assertTrue(response.errorMessage().startsWith("Unexpected validation error: "), response.errorMessage());
            assertTrue(response.errorMessage().contains(Unserializable.class.getSimpleName()), response.errorMessage());
        }
    }

    private static Map<String, Object> requiredNameSchema() {
        return new HashMap<>(Map.of(
            "type", "object",
            "properties", Map.of("name", Map.of("type", "string")),
            "required", List.of("name")));
    }

    @Test
    void structuredContentThatCannotBeSerializedIsInvalid() {
        JsonSchemaValidator.ValidationResponse response = validator.validate(Map.of("type", "object"), new Unserializable());
        assertFalse(response.valid());
        assertEquals("Structured content cannot be serialized", response.errorMessage());
    }

    /**
     * Has no serializable introspection.
     */
    static final class Unserializable {
    }
}
