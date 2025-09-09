package example.micronaut.mcp;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.server.EmbeddedServer;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.util.Map;

import static example.micronaut.ResourceLoaderUtils.readResource;
import static example.micronaut.mcp.ToolsTest.MICRONAUT_HTTP_SERVICES_WEATHER_URL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolsCallGetAlertsTest {

    @Test
    void toolsCallAlertTest() throws IOException, JSONException {
        try (EmbeddedServer mockServer = ApplicationContext.run(EmbeddedServer.class)) {
            Map<String, Object> config = Map.of(MICRONAUT_HTTP_SERVICES_WEATHER_URL, mockServer.getURL().toString());
            try (EmbeddedServer server = ApplicationContext.run(EmbeddedServer.class, config)) {
                HttpClient httpClient = server.getApplicationContext().createBean(HttpClient.class, server.getURL());
                ResourceLoader resourceLoader = server.getApplicationContext().getBean(ResourceLoader.class);
                BlockingHttpClient client = httpClient.toBlocking();

                HttpRequest<?> request = HttpRequest.POST("/mcp", readResource(resourceLoader, "classpath:toolCallAlerts.json").orElseThrow());
                String json = assertDoesNotThrow(() -> client.retrieve(request));
                assertNotNull(json);
                String expectedJson = readResource(resourceLoader, "classpath:toolCallAlertsResponse.json").orElseThrow();
                JSONAssert.assertEquals(expectedJson, json, true);
            }
        }
    }
}
