package io.micronaut.mcp.server.stateless.sync.tools;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_TOOLS_CALL_OBJECT_RETURN;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_CALL;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.TOOLS_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.type", value = "STATELESS_SYNC")
@Property(name = "spec.name", value = "StatelessSyncToolsAnnotationsObjectReturnTest")
@MicronautTest
class StatelessSyncToolsAnnotationsObjectReturnTest {
    @Test
    void toolsCall(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> listReq = HttpRequest.POST("/mcp", TOOLS_LIST);
        String listResult = assertDoesNotThrow(() -> client.retrieve(listReq));
        //JSONAssert.assertEquals(EXPECTED_TOOLS_LIST_OBJECT_RETURN, listResult, true);

        HttpRequest<?> req = HttpRequest.POST("/mcp", TOOLS_CALL);
        String result = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_TOOLS_CALL_OBJECT_RETURN, result, true);
    }

}
