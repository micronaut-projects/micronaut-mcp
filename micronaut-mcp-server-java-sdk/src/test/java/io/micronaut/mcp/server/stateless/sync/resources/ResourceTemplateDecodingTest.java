package io.micronaut.mcp.server.stateless.sync.resources;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sends resources/read requests through the HTTP transport, so that the SDK routes them to the resource template and
 * maps the errors to JSON-RPC errors.
 */
@Property(name = "micronaut.mcp.server.info.name", value = "mcp-server")
@Property(name = "micronaut.mcp.server.info.version", value = "0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ResourceTemplateDecodingTest")
@MicronautTest
class ResourceTemplateDecodingTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    JsonMapper jsonMapper;

    @Test
    void aSimpleVariableIsPercentDecoded() throws IOException {
        Map<String, Object> contents = contents(read("book://book/A%20Tale%20of%20Two%20Cities"));
        assertEquals("Book A Tale of Two Cities", contents.get("text"));
        assertEquals("text/plain", contents.get("mimeType"));
    }

    @Test
    void aReservedVariableMayContainAnEncodedSlash() throws IOException {
        Map<String, Object> contents = contents(read("file://files/docs%2Fread%20me.txt"));
        assertEquals("File docs/read me.txt", contents.get("text"));
        assertEquals("text/markdown", contents.get("mimeType"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"book://book/%zz", "book://book/%FF", "book://book/AC%2FDC"})
    void anInvalidValueIsAnInvalidParamsError(String uri) throws IOException {
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error(read(uri)).get("code"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"book://book/C++", "book://book/a;b"})
    void aUriRoutedToTheTemplateThatDoesNotMatchItIsAnInvalidParamsError(String uri) throws IOException {
        Map<String, Object> error = error(read(uri));
        assertEquals(McpSchema.ErrorCodes.INVALID_PARAMS, error.get("code"));
        assertTrue(error.get("message").toString().startsWith("The URI does not match the URI template book://book/{title}"), error.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"book://book/AC/DC", "file://files/docs/read.txt"})
    void aLiteralSlashIsNotRoutedToTheTemplate(String uri) throws IOException {
        assertEquals(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND, error(read(uri)).get("code"));
    }

    private Map<String, Object> read(String uri) throws IOException {
        HttpRequest<String> request = HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "resources/read", "params": {"uri": "%s"}}""".formatted(uri));
        String body;
        try {
            body = httpClient.toBlocking().retrieve(request);
        } catch (HttpClientResponseException e) {
            body = e.getResponse().getBody(String.class).orElseThrow();
        }
        return jsonMapper.readValue(body, Argument.mapOf(String.class, Object.class));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> contents(Map<String, Object> response) {
        Map<String, Object> result = (Map<String, Object>) response.get("result");
        return ((List<Map<String, Object>>) result.get("contents")).getFirst();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> error(Map<String, Object> response) {
        return (Map<String, Object>) response.get("error");
    }

    @Requires(property = "spec.name", value = "ResourceTemplateDecodingTest")
    @Singleton
    static class Templates {
        @ResourceTemplate(uriTemplate = "book://book/{title}")
        String book(String title) {
            return "Book " + title;
        }

        @ResourceTemplate(uriTemplate = "file://files/{+path}", mimeType = "text/markdown")
        String file(String path) {
            return "File " + path;
        }
    }
}
