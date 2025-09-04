package io.micronaut.mcp.server.stateless.sync.resources;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.server.utils.JsonRpcMessages;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "StatelessSyncResourcesBinaryAnnotationsTest")
@MicronautTest
class StatelessSyncResourcesBinaryAnnotationsTest {

    @Test
    void resourcesListContainsZip(@Client("/") HttpClient httpClient) throws JSONException {
        var client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", JsonRpcMessages.RESOURCES_LIST);
        var result = assertDoesNotThrow(() -> client.retrieve(req));

        var root = new JSONObject(result);
        var resources = root.getJSONObject("result").getJSONArray("resources");

        var found = false;
        for (var i = 0; i < resources.length(); i++) {
            var r = resources.getJSONObject(i);
            if ("example://zip".equals(r.optString("uri"))) {
                assertEquals("zip", r.optString("name"));
                assertEquals("Zip archive", r.optString("title"));
                assertEquals("A ZIP containing hello.txt", r.optString("description"));
                assertEquals("application/zip", r.optString("mimeType"));
                found = true;
                break;
            }
        }
        assertTrue(found, "Expected to find example://zip in resources list");
    }

    @Test
    void resourcesReadZipReturnsBlob(@Client("/") HttpClient httpClient) throws Exception {
        var client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", JsonRpcMessages.RESOURCES_READ_ZIP);
        var result = assertDoesNotThrow(() -> client.retrieve(req));

        var root = new JSONObject(result);
        var contents = root.getJSONObject("result").getJSONArray("contents");
        assertEquals(1, contents.length(), "Expected a single content element");

        var c0 = contents.getJSONObject(0);
        assertEquals("example://zip", c0.getString("uri"));
        assertEquals("application/zip", c0.getString("mimeType"));
        assertFalse(c0.has("text"), "Binary content should not include 'text'");
        assertTrue(c0.has("blob"), "Binary content must include 'blob' field");

        var blobB64 = c0.getString("blob");
        var zipBytes = Base64.getDecoder().decode(blobB64);

        // Validate the ZIP content and entries
        try (var zis = new ZipInputStream(new java.io.ByteArrayInputStream(zipBytes))) {
            var entry = zis.getNextEntry();
            assertNotNull(entry, "ZIP should contain at least one entry");
            assertEquals("hello.txt", entry.getName(), "Unexpected ZIP entry name");

            var baos = new ByteArrayOutputStream();
            var buffer = new byte[256];
            int read;
            while ((read = zis.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }
            var text = baos.toString(StandardCharsets.UTF_8);
            assertEquals("Hello Zip", text, "Unexpected content inside hello.txt");
        }
    }
}
