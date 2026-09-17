package io.micronaut.mcp.docs.resources;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_LIST_PGN;
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_TEMPLATES_LIST;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_LIST;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_ROUND_2;
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_TEMPLATES_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "pgn.rounda.path", value = "classpath:fidewwc2024/round_1.pgn")
@Property(name = "pgn.rounda.round", value = "1")
@Property(name = "pgn.roundb.path", value = "classpath:fidewwc2024/round_2.pgn")
@Property(name = "pgn.roundb.round", value = "2")
@Property(name = "pgn.roundc.path", value = "classpath:fidewwc2024/round_3.pgn")
@Property(name = "pgn.roundc.round", value = "3")
@Property(name = "spec.name", value = "ResourcesFactoryTest")
@MicronautTest
class ResourcesFactoryTest {

    @Test
    void resourcesList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", RESOURCES_LIST)));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST_PGN, result, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void resourcesTemplatesList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", RESOURCES_TEMPLATES_LIST)));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_TEMPLATES_LIST, result, true);
    }

    @Test
    void resourcesRead(@Client("/") HttpClient httpClient, PgnLoader pgnLoader) {
        BlockingHttpClient client = httpClient.toBlocking();
        String result = assertDoesNotThrow(() -> client.retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_2)));
        assertTrue(result.contains("\"uri\":\"pgn://round/2\""), result);
        assertTrue(result.contains("\"mimeType\":\"application/x-chess-pgn\""), result);
        assertTrue(result.contains("[Round \\\"2\\\"]"), result);
        assertTrue(pgnLoader.loadPgn(2).orElseThrow().startsWith("[Event \"FIDE World Championship Match 2024\"]"));
    }
}
