package io.micronaut.mcp.server.json;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.json.JsonMapper;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.json.TypeRef;
import jakarta.inject.Singleton;

import java.io.IOException;

/**
 * Replaces the MCP JSON mapper with one that is not a {@link MicronautMcpJsonMapper}, as an application using the
 * Jackson databind mapper of the SDK would, so that the tool registry converts structured content itself.
 */
@Requires(property = "foreign-mcp-json-mapper")
@Factory
public class ForeignMcpJsonMapperFactory {

    @Replaces(bean = McpJsonMapper.class, factory = McpJsonFactory.class)
    @Singleton
    McpJsonMapper foreignMcpJsonMapper(JsonMapper jsonMapper) {
        McpJsonMapper delegate = new MicronautMcpJsonMapper(jsonMapper);
        return new McpJsonMapper() {
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
        };
    }
}
