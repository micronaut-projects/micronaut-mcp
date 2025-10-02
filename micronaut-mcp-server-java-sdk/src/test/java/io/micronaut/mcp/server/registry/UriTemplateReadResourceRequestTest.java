package io.micronaut.mcp.server.registry;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class UriTemplateReadResourceRequestTest {
    @Test
    void arguments() {
        Map<String, Object> m = UriTemplateReadResourceRequest.arguments("pgn://round/{round}/pgn", "pgn://round/14/pgn");
        assertEquals(Map.of("round", "14"), m);
    }
}
