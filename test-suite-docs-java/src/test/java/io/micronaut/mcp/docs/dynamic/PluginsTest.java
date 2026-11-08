package io.micronaut.mcp.docs.dynamic;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.tools.list-changed", value = "true")
@Property(name = "spec.name", value = "PluginsTest")
@MicronautTest
class PluginsTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    Plugins plugins;

    @Test
    void toolsAreInstalledAndUninstalledAtRuntime() {
        plugins.install("greeter");
        assertTrue(call("tools/list", "{}").contains("\"name\":\"greeter\""));
        assertTrue(call("tools/call", "{\"name\": \"greeter\", \"arguments\": {}}").contains("\"text\":\"installed\""));
        plugins.uninstall("greeter");
        assertFalse(call("tools/list", "{}").contains("\"name\":\"greeter\""));
    }

    private String call(String method, String params) {
        return httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp",
            "{\"jsonrpc\": \"2.0\", \"id\": 1, \"method\": \"" + method + "\", \"params\": " + params + "}"));
    }
}
