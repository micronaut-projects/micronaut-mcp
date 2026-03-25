package io.micronaut.mcp.server.serialization;

import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(startApplication = false)
class RoleSerializationTest {
    @Inject
    JsonMapper jsonMapper;

    @Test
    void roleSerialization() throws IOException {
        String json = jsonMapper.writeValueAsString(McpSchema.Role.USER);
        assertEquals("\"user\"", json);
    }

    @ParameterizedTest
    @ValueSource(strings = {"assistant", "ASSISTANT"})
    void roleDeserialization(String input) throws IOException {
        McpSchema.Role role = jsonMapper.readValue("\"" + input + "\"", McpSchema.Role.class);
        assertEquals(McpSchema.Role.ASSISTANT, role);
    }
}
