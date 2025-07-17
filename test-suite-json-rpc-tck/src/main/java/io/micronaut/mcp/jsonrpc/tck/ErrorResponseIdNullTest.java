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
class ErrorResponseIdNullTest {
    @Test
    void errorResponseSerialization(JsonMapper jsonMapper) throws IOException {
        String expected = """
            {"jsonrpc":"2.0","error":{"code":-32700,"message":"Parse error"},"id":null}""";
        var response = new ErrorResponse<>(new Error<>(ErrorCode.PARSE_ERROR));
        var json = jsonMapper.writeValueAsString(response);
        assertEquals(expected, json);
    }
}
