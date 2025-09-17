package example.micronaut.mcp;

import io.micronaut.http.HttpRequest;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        HttpClientResponseException ex = assertThrows(HttpClientResponseException.class,
            () -> client.exchange(HttpRequest.POST("/mcp", """
                {"jsonrpc":"2.0","id":2,"method":"prompts/list","params":{}}"""), String.class));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, ex.getStatus());
        Optional<String> jsonOptional = ex.getResponse().getBody(String.class);
        assertTrue(jsonOptional.isPresent());
        String json = jsonOptional.get();
        String expected = """
            {"jsonrpc":"2.0","id":2,"error":{"code":-32603,"message":"Missing handler for request type: prompts/list"}}""";
        assertEquals(expected, json);
    }
}
