package io.micronaut.mcp.server.json;

import io.modelcontextprotocol.json.McpJsonDefaults;
import io.modelcontextprotocol.json.McpJsonMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MicronautMcpJsonMapperSupplierTest {
    @Test
    void testMcpJsonViaSpi() {
        McpJsonMapper jsonMapper = assertDoesNotThrow(McpJsonDefaults::getMapper);
        assertNotNull(jsonMapper);
        assertInstanceOf(MicronautMcpJsonMapper.class, jsonMapper);
    }
}
