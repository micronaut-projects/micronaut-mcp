package io.micronaut.mcp.server.utils;

import io.micronaut.core.type.Argument;
import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@MicronautTest(startApplication = false)
class InitializeRequestUtilsTest {
    @Test
    void initializeRequestUtils(JsonMapper jsonMapper) throws IOException {

        assertFalse(InitializeRequestUtils.createInitializeRequest(jsonMapper, null).isPresent());
        assertFalse(InitializeRequestUtils.createInitializeRequest(jsonMapper, Collections.emptyMap()).isPresent());
        assertFalse(InitializeRequestUtils.createInitializeRequest(jsonMapper, jsonMapper.readValue("""
        {"jsonrpc":"2.0","method":"ping","id":"123"}""", Argument.mapOf(String.class, Object.class))).isPresent());
        assertFalse(InitializeRequestUtils.createInitializeRequest(jsonMapper, jsonMapper.readValue("""
            {"jsonrpc":"2.0","id":0,"method":"initialize"}""", Argument.mapOf(String.class, Object.class))).isPresent());
        String json = """
            {"jsonrpc":"2.0","id":0,"method":"initialize","params":{"protocolVersion":"2025-06-18","capabilities":{"sampling":{},"elicitation":{},"roots":{"listChanged":true}},"clientInfo":{"name":"mcp-inspector","version":"0.16.3"}}}""";
        Map<String, Object> m = jsonMapper.readValue(json, Argument.mapOf(String.class, Object.class));
        Optional<McpSchema.InitializeRequest> initializeRequestOptional = InitializeRequestUtils.createInitializeRequest(jsonMapper, m);
        assertTrue(initializeRequestOptional.isPresent());
        McpSchema.InitializeRequest initializeRequest = initializeRequestOptional.get();

        McpSchema.InitializeRequest expected = new McpSchema.InitializeRequest(
            "2025-06-18",
            new McpSchema.ClientCapabilities(null,
                new McpSchema.ClientCapabilities.RootCapabilities(true),
                new McpSchema.ClientCapabilities.Sampling(),
                new McpSchema.ClientCapabilities.Elicitation()),
            new McpSchema.Implementation("mcp-inspector", "0.16.3"),
            null);
            assertEquals(expected, initializeRequest);
    }
}
