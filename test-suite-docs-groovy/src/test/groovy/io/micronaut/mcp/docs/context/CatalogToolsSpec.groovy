package io.micronaut.mcp.docs.context

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "CatalogToolsSpec")
@MicronautTest
class CatalogToolsSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "progress and log messages are streamed"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp",
            '{"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "importCatalog", "arguments": {"pages": 2}, "_meta": {"progressToken": "import"}}}')
            .header("Accept", "application/json, text/event-stream"))

        then:
        result.contains("notifications/progress") && result.contains("Imported page 2")
        result.contains("notifications/message") && result.contains('"text":"done"')
    }
}
