package io.micronaut.mcp.docs.context

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "CatalogToolsTest")
@MicronautTest
class CatalogToolsTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun progressAndLogMessagesAreStreamed() {
        val result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp",
            """{"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "importCatalog", "arguments": {"pages": 2}, "_meta": {"progressToken": "import"}}}""")
            .header("Accept", "application/json, text/event-stream"))
        assertTrue(result.contains("notifications/progress") && result.contains("Imported page 2"), result)
        assertTrue(result.contains("notifications/message") && result.contains("\"text\":\"done\""), result)
    }
}
