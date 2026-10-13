package io.micronaut.mcp.docs.dynamic

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.tools.list-changed", value = "true")
@Property(name = "spec.name", value = "PluginsTest")
@MicronautTest
class PluginsTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Inject
    lateinit var plugins: Plugins

    @Test
    fun toolsAreInstalledAndUninstalledAtRuntime() {
        plugins.install("greeter")
        assertTrue(call("tools/list", "{}").contains("\"name\":\"greeter\""))
        assertTrue(call("tools/call", """{"name": "greeter", "arguments": {}}""").contains("\"text\":\"installed\""))
        plugins.uninstall("greeter")
        assertFalse(call("tools/list", "{}").contains("\"name\":\"greeter\""))
    }

    private fun call(method: String, params: String): String =
        httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp",
            """{"jsonrpc": "2.0", "id": 1, "method": "$method", "params": $params}"""))
}
