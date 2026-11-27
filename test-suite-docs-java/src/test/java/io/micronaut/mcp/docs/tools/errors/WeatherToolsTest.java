package io.micronaut.mcp.docs.tools.errors;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "WeatherToolsTest")
@MicronautTest
class WeatherToolsTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Test
    void anExpectedFailureIsAnErrorResult() throws JSONException {
        JSONAssert.assertEquals("""
            {"result":{"content":[{"type":"text","text":"Unknown city Paris, use Madrid or London"}],"isError":true}}""",
            call("temperature", "Paris"), false);
    }

    @Test
    void anExceptionIsAToolExecutionError() throws JSONException {
        JSONAssert.assertEquals("""
            {"result":{"content":[{"type":"text","text":"Tool forecast failed"}],"isError":true}}""",
            call("forecast", "Madrid"), false);
    }

    @Test
    void anMcpErrorIsAProtocolError() throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<String> request = request("alerts", "Madrid");
        HttpClientResponseException e = assertThrows(HttpClientResponseException.class, () -> client.retrieve(request));
        JSONAssert.assertEquals("""
            {"error":{"code":-32603,"message":"Weather alerts are disabled on this server"}}""",
            e.getResponse().getBody(String.class).orElseThrow(), false);
    }

    private String call(String tool, String city) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<String> request = request(tool, city);
        return assertDoesNotThrow(() -> client.retrieve(request));
    }

    private static HttpRequest<String> request(String tool, String city) {
        return HttpRequest.POST("/mcp", """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"%s","arguments":{"city":"%s"}}}""".formatted(tool, city));
    }
}
