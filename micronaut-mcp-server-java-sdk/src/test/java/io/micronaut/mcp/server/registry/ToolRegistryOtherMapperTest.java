package io.micronaut.mcp.server.registry;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.json.MicronautMcpJsonMapperSupplier;
import io.micronaut.mcp.server.stateless.sync.EvaluationResult;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * With an MCP JSON mapper other than the Micronaut one, structured results are converted to a Map first.
 */
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ToolRegistryOtherMapperTest")
@MicronautTest(startApplication = false)
class ToolRegistryOtherMapperTest {

    @Inject
    ToolRegistry toolRegistry;

    @Test
    void structuredResultsAreConvertedToAMap() {
        McpSchema.CallToolResult result = toolRegistry.getStatelessSyncSpecs().stream()
            .filter(s -> s.tool().name().equals("evaluate")).findFirst().orElseThrow()
            .callHandler().apply(McpTransportContext.create(Map.of()), new McpSchema.CallToolRequest("evaluate", Map.of()));
        assertEquals(Map.of("fen", "start", "evaluation", "+0.2"), result.structuredContent());
    }

    @Requires(property = "spec.name", value = "ToolRegistryOtherMapperTest")
    @Singleton
    static class Tools {
        @Tool
        EvaluationResult evaluate() {
            return new EvaluationResult("start", "+0.2");
        }
    }

    /**
     * Stands in for another MCP JSON mapper, such as the Jackson databind one.
     */
    @Requires(property = "spec.name", value = "ToolRegistryOtherMapperTest")
    @Replaces(McpJsonMapper.class)
    @Singleton
    static class OtherMcpJsonMapper implements McpJsonMapper {
        private final McpJsonMapper delegate = new MicronautMcpJsonMapperSupplier().get();

        @Override
        public <T> T readValue(String content, Class<T> type) throws IOException {
            return delegate.readValue(content, type);
        }

        @Override
        public <T> T readValue(byte[] content, Class<T> type) throws IOException {
            return delegate.readValue(content, type);
        }

        @Override
        public <T> T readValue(String content, TypeRef<T> type) throws IOException {
            return delegate.readValue(content, type);
        }

        @Override
        public <T> T readValue(byte[] content, TypeRef<T> type) throws IOException {
            return delegate.readValue(content, type);
        }

        @Override
        public <T> T convertValue(Object fromValue, Class<T> type) {
            return delegate.convertValue(fromValue, type);
        }

        @Override
        public <T> T convertValue(Object fromValue, TypeRef<T> type) {
            return delegate.convertValue(fromValue, type);
        }

        @Override
        public String writeValueAsString(Object value) throws IOException {
            return delegate.writeValueAsString(value);
        }

        @Override
        public byte[] writeValueAsBytes(Object value) throws IOException {
            return delegate.writeValueAsBytes(value);
        }
    }
}
