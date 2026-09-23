package io.micronaut.mcp.docs.completions.resources

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_READ_REPORT
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCE_COMPLETION
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_REPORT
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCE_COMPLETION_REQUEST
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MyResourceCompletionsTest")
@MicronautTest
class MyResourceCompletionsTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun resourceCompletion() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", RESOURCE_COMPLETION_REQUEST)) }
        JSONAssert.assertEquals(EXPECTED_RESOURCE_COMPLETION, result, true)
    }

    @Test
    fun resourceTemplateRead() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_REPORT)) }
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ_REPORT, result, true)
    }
}
