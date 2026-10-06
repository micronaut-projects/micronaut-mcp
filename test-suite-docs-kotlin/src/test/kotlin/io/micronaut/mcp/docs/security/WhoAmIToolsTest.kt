package io.micronaut.mcp.docs.security

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "WhoAmIToolsTest")
@MicronautTest
class WhoAmIToolsTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun withoutAnAuthenticatedUser() {
        val result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp",
            """{"jsonrpc": "2.0", "id": 1, "method": "tools/call", "params": {"name": "whoami", "arguments": {}}}"""))
        assertTrue(result.contains("\"text\":\"anonymous\""), result)
    }
}
