package io.micronaut.mcp.server.stateless.sync.tools.toolargdescription;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_CALL;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_TOOLS_CALL;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "StatelessSyncToolArgDescriptionTest")
@MicronautTest
class StatelessSyncToolArgDescriptionTest {

    private static final String EXPECTED_TOOLS_LIST_WITH_TOOLARG_DESCRIPTION = """
        {"jsonrpc":"2.0","id":3,"result":{"tools":[{"name":"fenEvaluation","description":"Evaluate a chess position using a FEN string.","inputSchema":{"type":"object","properties":{"fen":{"type":"string","description":"A Chess position in Forsyth–Edwards Notation"}},"required":["fen"]}}]}}""";

    @Test
    void toolsList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", TOOLS_LIST);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_TOOLS_LIST_WITH_TOOLARG_DESCRIPTION, result, true);
    }

    @Test
    void toolsCall(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", TOOLS_CALL);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_TOOLS_CALL, result, true);
    }

}