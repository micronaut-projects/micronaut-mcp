package io.micronaut.mcp.jsonrpc.tck;

import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.jsonrpc.SuccessfulResponse;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(startApplication = false)
class SuccessfulResponseSerializationTest {

    @Test
    void successfulResponseSerialization(JsonMapper jsonMapper) throws IOException {
        String expected = """
            {"jsonrpc":"2.0","result":19,"id":1}""";
        var response = new SuccessfulResponse<>(19, 1);
        String json = jsonMapper.writeValueAsString(response);
        assertEquals(expected, json);
    }
}
