package io.micronaut.mcp.jsonrpc;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(startApplication = false)
class RequestTest {

    @Test
    void testRequest() {
        List<Integer> params = List.of(42, 23);
        Integer id = 1;
        String method = "subtract";
        var req = new Request<>(method, params, id);
        assertEquals("2.0", req.jsonrpc());
        assertEquals(method, req.method());
        assertEquals(params, req.params());
        assertEquals(id, req.id());
    }
}
