package io.micronaut.mcp.jsonrpc.tck;

import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.jsonrpc.Request;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(startApplication = false)
public class RequestTest {
    @Test
    void requestSerialization(JsonMapper jsonMapper) throws IOException {
        List<Integer> params = List.of(42, 23);
        Integer id = 1;
        String method = "subtract";
        var req = new Request<>(method, params, id);
        var json = jsonMapper.writeValueAsString(req);
        assertEquals("""
            {"jsonrpc":"2.0","method":"subtract","params":[42,23],"id":1}""", json);

    }
}
