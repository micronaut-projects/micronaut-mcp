/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package example.micronaut.structured;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.annotations.ToolArg;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "spec.name", value = "McpJsonRpcHttpTest")
@Property(name = "jackson.deserialization-features.USE_BIG_DECIMAL_FOR_FLOATS", value = "true")
@Property(name = "jackson.deserialization-features.USE_BIG_INTEGER_FOR_INTS", value = "true")
@Property(name = "jackson.deserialization-features.USE_JAVA_ARRAY_FOR_JSON_ARRAY", value = "true")
@MicronautTest
class McpJsonRpcHttpTest {

    @Test
    void invalidRequestIncludesNullId(@Client("/") HttpClient httpClient) throws JSONException {
        var request = HttpRequest.POST("/mcp", """
            {"method":"ping","id":1}""");

        var ex = assertThrows(HttpClientResponseException.class,
            () -> httpClient.toBlocking().exchange(request, String.class));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":null,"error":{"code":-32600,"message":"Invalid Request: \\"jsonrpc\\" must be \\"2.0\\""}}""",
            ex.getResponse().getBody(String.class).orElseThrow(), true);
    }

    @Test
    void toolReceivesExactDecimal(@Client("/") HttpClient httpClient) throws JSONException {
        var request = HttpRequest.POST("/mcp", """
            {
              "jsonrpc":"2.0",
              "id":9223372036854775807,
              "method":"tools/call",
              "params":{"name":"exactDecimal","arguments":{"value":0.12345678901234567890123456789}}
            }""");

        var response = httpClient.toBlocking().exchange(request, String.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":9223372036854775807,"result":{"content":[{"type":"text","text":"0.12345678901234567890123456789"}],"isError":false}}""",
            response.body(), true);
    }

    @Test
    void arrayParametersAreAcceptedWithCustomNumberSettings(@Client("/") HttpClient httpClient) throws JSONException {
        var request = HttpRequest.POST("/mcp", """
            {"jsonrpc":"2.0","id":1,"method":"ping","params":[]}""");

        var response = httpClient.toBlocking().exchange(request, String.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":1,"result":{}}""", response.body(), true);
    }

    @Requires(property = "spec.name", value = "McpJsonRpcHttpTest")
    @Singleton
    static class Tools {
        @Tool
        String exactDecimal(@ToolArg(name = "value") BigDecimal value) {
            return value.toPlainString();
        }
    }
}
