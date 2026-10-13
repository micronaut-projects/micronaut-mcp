package example.micronaut.structured;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "spec.name", value = "StructuredToolsHttpTest")
@MicronautTest
class StructuredToolsHttpTest {

    @Test
    void structuredResult(@Client("/") HttpClient httpClient) throws JSONException {
        String result = call(httpClient, "startEvaluation");
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"{\\"fen\\":\\"start\\",\\"evaluation\\":\\"+0.2\\"}"}],"isError":false,"structuredContent":{"fen":"start","evaluation":"+0.2"}}}""",
            result, true);
    }

    @Test
    void nullStructuredResultIsToolError(@Client("/") HttpClient httpClient) throws JSONException {
        String result = call(httpClient, "missingEvaluation");
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":1,"result":{"content":[{"type":"text","text":"Tool missingEvaluation returned no structured content"}],"isError":true}}""",
            result, true);
    }

    private static String call(HttpClient httpClient, String tool) {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"%s","arguments":{}}}""".formatted(tool));
        return assertDoesNotThrow(() -> client.retrieve(req));
    }
}
