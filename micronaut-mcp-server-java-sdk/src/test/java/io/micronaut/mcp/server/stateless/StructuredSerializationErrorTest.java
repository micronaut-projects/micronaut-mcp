package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.json.JsonMapper;
import io.micronaut.jsonschema.JsonSchema;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.callTool;
import static io.micronaut.mcp.server.stateless.ExecutionModelSupport.text;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * With an MCP JSON mapper other than the Micronaut one, the tool registry converts structured content itself, and a
 * result it cannot convert is reported like a text result that cannot be serialized.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "StructuredSerializationErrorTest")
@Property(name = "foreign-mcp-json-mapper", value = "true")
@MicronautTest
class StructuredSerializationErrorTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    JsonMapper jsonMapper;

    @Test
    void aStructuredResultThatCannotBeConvertedIsAToolExecutionError() throws IOException {
        Map<String, Object> result = callTool(httpClient, jsonMapper, "notSerializable");
        assertEquals(Boolean.TRUE, result.get("isError"));
        assertEquals("The result of tool notSerializable could not be serialized", text(result));
    }

    /**
     * Has a JSON schema, but no serializer.
     *
     * @param fen The position
     */
    @JsonSchema
    record Unserializable(String fen) {
    }

    @Requires(property = "spec.name", value = "StructuredSerializationErrorTest")
    @Singleton
    static class Tools {
        @Tool
        Unserializable notSerializable() {
            return new Unserializable("start");
        }
    }
}
