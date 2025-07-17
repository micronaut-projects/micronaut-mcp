package io.micronaut.mcp.jsonrpc.tck;

import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.jsonrpc.Error;
import io.micronaut.mcp.jsonrpc.ErrorCode;
import io.micronaut.mcp.jsonrpc.ErrorResponse;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(startApplication = false)
class ErrorResponseTest {

    @Test
    void errorResponseSerialization(JsonMapper jsonMapper) throws IOException {
        String expected = """
            {"jsonrpc":"2.0","error":{"code":-32601,"message":"Method not found"},"id":"1"}""";
        var response = new ErrorResponse<>(new Error<>(ErrorCode.METHOD_NOT_FOUND), "1");
        String json = jsonMapper.writeValueAsString(response);
        assertEquals(expected, json);
    }
}
