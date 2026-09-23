package io.micronaut.mcp.docs.tools

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_HELLO_WORLD_TOOL_ANNOTATIONS
import io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "HelloWorldToolTest")
@MicronautTest
class HelloWorldToolTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun toolAnnotations() {
        val client = httpClient.toBlocking()
        val json = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)) }
        assertTrue(json.contains(EXPECTED_HELLO_WORLD_TOOL_ANNOTATIONS), json)
    }
}
