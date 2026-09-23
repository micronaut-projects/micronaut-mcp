package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class NoCapabilityTest {
    protected abstract String getRequest();
    protected abstract String getExpectedResponse();

    protected void testNoCapabilityTest(HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", getRequest());
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class, () -> client.retrieve(req));
        HttpResponse<?> rsp = ex.getResponse();
        assertEquals(rsp.getStatus(), HttpStatus.BAD_REQUEST);
        Optional<String> jsonOptional = rsp.getBody(String.class);
        assertTrue(jsonOptional.isPresent());
        String jsonRpc = jsonOptional.get();
        String expected = getExpectedResponse();
        JSONAssert.assertEquals(expected, jsonRpc, true);
    }
}
