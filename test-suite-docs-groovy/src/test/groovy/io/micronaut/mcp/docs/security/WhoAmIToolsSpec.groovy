package io.micronaut.mcp.docs.security

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "WhoAmIToolsSpec")
@MicronautTest
class WhoAmIToolsSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "without an authenticated user"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp",
            '{"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "whoami", "arguments": {}}}'))

        then:
        result.contains('"text":"anonymous"')
    }
}
