package io.micronaut.mcp.docs.prompts

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_PROMPTS_GET
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_PROMPTS_LIST
import io.micronaut.mcp.docs.JsonRpcMessages.PROMPTS_GET
import io.micronaut.mcp.docs.JsonRpcMessages.PROMPTS_LIST
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "PromptsTest")
@MicronautTest
class PromptsTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun promptsList() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", PROMPTS_LIST)) }
        JSONAssert.assertEquals(EXPECTED_PROMPTS_LIST, result, true)
    }

    @Test
    fun promptsGet() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", PROMPTS_GET)) }
        JSONAssert.assertEquals(EXPECTED_PROMPTS_GET, result, true)
    }
}
