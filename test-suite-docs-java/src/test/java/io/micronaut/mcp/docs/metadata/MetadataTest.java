package io.micronaut.mcp.docs.metadata;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MetadataTest")
@MicronautTest
class MetadataTest {

    @Test
    void iconsAndMetaOfATool(@Client("/") HttpClient httpClient) throws JSONException {
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}"""));
        JSONAssert.assertEquals("""
            {"result": {"tools": [{"name": "forecast",
              "icons": [{"src": "https://example.com/sun.svg", "mimeType": "image/svg+xml", "sizes": ["any"]}],
              "_meta": {"com.example/category": "weather"}}]}}""", result, JSONCompareMode.LENIENT);
    }

    @Test
    void annotationsOfAResource(@Client("/") HttpClient httpClient) throws JSONException {
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "resources/list", "params": {}}"""));
        JSONAssert.assertEquals("""
            {"result": {"resources": [{"uri": "weather://stations", "size": 4,
              "annotations": {"audience": ["assistant"], "priority": 0.5}}]}}""", result, JSONCompareMode.LENIENT);
    }
}
