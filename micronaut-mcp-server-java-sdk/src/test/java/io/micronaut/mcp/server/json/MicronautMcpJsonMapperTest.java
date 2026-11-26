package io.micronaut.mcp.server.json;

import io.micronaut.json.JsonMapper;
import io.micronaut.json.tree.JsonNode;
import io.modelcontextprotocol.json.TypeRef;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MicronautMcpJsonMapperTest {

    private static final JsonMapper JSON_MAPPER = JsonMapper.createDefault();

    @Test
    void convertValueReadsATreeWithoutWritingItAgain() throws Exception {
        MicronautMcpJsonMapper mapper = new MicronautMcpJsonMapper(jsonMapperThatCannotWriteTrees());
        JsonNode params = JSON_MAPPER.readValue("""
            {"name": "evaluate", "arguments": {"depth": 12, "moves": ["e4", "e5"], "position": {"fen": null}}}"""
            .getBytes(StandardCharsets.UTF_8), JsonNode.class);

        McpSchema.CallToolRequest request = mapper.convertValue(params, McpSchema.CallToolRequest.class);
        Map<String, Object> map = mapper.convertValue(params, new TypeRef<Map<String, Object>>() { });

        assertEquals("evaluate", request.name());
        assertEquals(12, ((Number) request.arguments().get("depth")).intValue());
        assertEquals(List.of("e4", "e5"), request.arguments().get("moves"));
        assertNull(((Map<?, ?>) request.arguments().get("position")).get("fen"));
        assertEquals("evaluate", map.get("name"));
    }

    @Test
    void convertValueWritesOtherValuesToATree() {
        MicronautMcpJsonMapper mapper = new MicronautMcpJsonMapper(JSON_MAPPER);

        McpSchema.CallToolRequest request = mapper.convertValue(Map.of("name", "evaluate"), McpSchema.CallToolRequest.class);

        assertEquals("evaluate", request.name());
    }

    private static JsonMapper jsonMapperThatCannotWriteTrees() {
        return (JsonMapper) Proxy.newProxyInstance(MicronautMcpJsonMapperTest.class.getClassLoader(), new Class<?>[] {JsonMapper.class},
            (proxy, method, args) -> {
                if (method.getName().equals("writeValueToTree")) {
                    throw new AssertionError("a JsonNode must be read directly, not written to a tree: " + Arrays.toString(args));
                }
                try {
                    return method.invoke(JSON_MAPPER, args);
                } catch (InvocationTargetException e) {
                    throw e.getCause();
                }
            });
    }
}
