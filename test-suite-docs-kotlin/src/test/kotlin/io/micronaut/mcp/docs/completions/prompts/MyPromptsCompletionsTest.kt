package io.micronaut.mcp.docs.completions.prompts

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_PROMPT_COMPLETION
import io.micronaut.mcp.docs.JsonRpcMessages.PROMPT_COMPLETION_REQUEST
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MyPromptsCompletionsTest")
@MicronautTest
class MyPromptsCompletionsTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun promptCompletion() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", PROMPT_COMPLETION_REQUEST)) }
        JSONAssert.assertEquals(EXPECTED_PROMPT_COMPLETION, result, true)
    }
}
