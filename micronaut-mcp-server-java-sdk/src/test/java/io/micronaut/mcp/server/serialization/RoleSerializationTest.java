package io.micronaut.mcp.server.serialization;

import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(startApplication = false)
class RoleSerializationTest {

    @Test
    void roleSerialization(JsonMapper jsonMapper) throws IOException {
        String json = jsonMapper.writeValueAsString(McpSchema.Role.USER);
        assertEquals("\"user\"", json);
    }

    @Test
    void roleDeserialization(JsonMapper jsonMapper) throws IOException {
        McpSchema.Role role = jsonMapper.readValue("\"assistant\"", McpSchema.Role.class);
        assertEquals(McpSchema.Role.ASSISTANT, role);
    }
}
