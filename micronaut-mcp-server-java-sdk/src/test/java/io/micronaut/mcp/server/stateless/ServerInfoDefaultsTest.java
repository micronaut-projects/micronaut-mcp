package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@Property(name = "micronaut.mcp.server.info.name", value = "weather")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest
class ServerInfoDefaultsTest {

    @Test
    void unsetServerDescriptionIsOmitted(@Client("/") HttpClient httpClient) throws JSONException {
        JSONObject result = new JSONObject(httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "initialize",
             "params": {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "test", "version": "1"}}}""")))
            .getJSONObject("result");
        assertFalse(result.has("instructions"));
        JSONObject serverInfo = result.getJSONObject("serverInfo");
        assertEquals("weather", serverInfo.getString("name"));
        for (String key : new String[] {"title", "description", "icons", "websiteUrl"}) {
            assertFalse(serverInfo.has(key), key);
        }
    }
}
