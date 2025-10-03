package example.micronaut.mcp;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class PromptsListTest {

    /**
     * MCP Clients. For example, Claude Desktop may not respect the capabilities negotation and
     * sends prompts/list resource/list and tools/list requests even if the server did not
     * advertise support for these features.
     *
     * This test verifies an MCP server without prompts does not crash when receiving a prompts/list request.
     */
    @Test
    void promptsList(@Client("/") HttpClient httpClient) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpResponse<String> rsp = assertDoesNotThrow(() -> client.exchange(HttpRequest.POST("/mcp", """
                {"jsonrpc":"2.0","id":2,"method":"prompts/list","params":{}}"""), String.class));
        assertEquals(rsp.getStatus(), HttpStatus.OK);
        Optional<String> jsonOptional = rsp.getBody(String.class);
        assertTrue(jsonOptional.isPresent());
        String json = jsonOptional.get();
        String expected = """
            {"jsonrpc":"2.0","id":2,"result":{"prompts":[]}}""";
        assertEquals(expected, json);
    }
}
