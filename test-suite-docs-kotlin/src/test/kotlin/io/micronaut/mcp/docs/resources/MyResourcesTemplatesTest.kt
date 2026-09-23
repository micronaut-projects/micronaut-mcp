package io.micronaut.mcp.docs.resources

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_TEMPLATES_LIST
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_ROUND_2
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_ROUND_99
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_TEMPLATES_LIST
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MyResourcesTemplatesTest")
@MicronautTest
class MyResourcesTemplatesTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun resourcesTemplatesList() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", RESOURCES_TEMPLATES_LIST)) }
        JSONAssert.assertEquals(EXPECTED_RESOURCES_TEMPLATES_LIST, result, true)
    }

    @Test
    fun resourcesRead() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_2)) }
        assertTrue(result.contains("\"uri\":\"pgn://round/2\""), result)
        assertTrue(result.contains("\"mimeType\":\"application/x-chess-pgn\""), result)
        assertTrue(result.contains("[Round \\\"2\\\"]"), result)
    }

    @Test
    fun resourcesReadNotFound() {
        val client = httpClient.toBlocking()
        val ex = assertThrows(HttpClientResponseException::class.java) { client.retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_99)) }
        val body = ex.response.getBody(String::class.java).orElse("")
        assertTrue(body.contains("\"error\""), body)
        assertTrue(body.contains("resource for round not found"), body)
    }
}
