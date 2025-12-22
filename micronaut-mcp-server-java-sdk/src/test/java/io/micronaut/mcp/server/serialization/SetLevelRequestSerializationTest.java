package io.micronaut.mcp.server.serialization;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@MicronautTest(startApplication = false)
class SetLevelRequestSerializationTest {
    @Test
    void testSetLevelRequestSerialization(McpJsonMapper jsonMapper) throws IOException {

        Object params = Map.of("level", "debug");
        assertDoesNotThrow(() -> jsonMapper.convertValue(params, new TypeRef<McpSchema.SetLevelRequest>() {}));
    }
}
