package io.micronaut.mcp.jsonrpc.tck;

import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.jsonrpc.Error;
import io.micronaut.mcp.jsonrpc.ErrorCode;
import io.micronaut.mcp.jsonrpc.ErrorResponse;
import io.micronaut.mcp.jsonrpc.SuccessfulResponse;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(startApplication = false)
public class PingResponseSerializationTest {
    @Test
    void errorResponseSerialization(JsonMapper jsonMapper) throws IOException {
        String expected = """
            {"jsonrpc":"2.0","result":{},"id":"123"}""";
        var response = new SuccessfulResponse<>(Collections.emptyMap(), "123");
        var json = jsonMapper.writeValueAsString(response);
        assertEquals(expected, json);
    }
}
