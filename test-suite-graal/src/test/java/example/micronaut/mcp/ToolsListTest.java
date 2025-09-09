package example.micronaut.mcp;

import io.micronaut.core.io.ResourceLoader;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static example.micronaut.mcp.JsonRpcMessages.TOOLS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static example.micronaut.ResourceLoaderUtils.readResource;

@MicronautTest
class ToolsListTest {

    @Test
    void toolsList(@Client("/") HttpClient httpClient, ResourceLoader resourceLoader) throws IOException, JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> request = HttpRequest.POST("/mcp", TOOLS_LIST);
        String json = assertDoesNotThrow(() -> client.retrieve(request));
        assertNotNull(json);
        String expectedJson = readResource(resourceLoader, "classpath:toolsList.json").orElseThrow();
        JSONAssert.assertEquals(expectedJson, json, true);
    }
}
