package io.micronaut.mcp.conf.client;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class McpClientRequestHeadersTest {

    @Test
    void aHeaderReplacesOneWithTheSameNameWhateverItsCase() {
        Map<String, String> headers = new LinkedHashMap<>(Map.of("authorization", "Bearer static"));
        McpClientRequestHeaders.merge(headers, Map.of("Authorization", "Bearer caller"));
        assertEquals(Map.of("Authorization", "Bearer caller"), headers);
    }

    @Test
    void headersWithOtherNamesAreKept() {
        Map<String, String> headers = new LinkedHashMap<>(Map.of("X-Tenant", "acme"));
        McpClientRequestHeaders.merge(headers, Map.of("Authorization", "Bearer caller"));
        assertEquals(Map.of("X-Tenant", "acme", "Authorization", "Bearer caller"), headers);
    }
}
