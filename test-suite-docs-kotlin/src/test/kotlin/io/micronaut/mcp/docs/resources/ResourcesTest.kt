package io.micronaut.mcp.docs.resources

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_LIST_HELLO
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_READ_HELLO
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_LIST
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_HELLO
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ResourcesTest")
@MicronautTest
class ResourcesTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun resourcesList() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", RESOURCES_LIST)) }
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST_HELLO, result, true)
    }

    @Test
    fun resourcesRead() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_HELLO)) }
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ_HELLO, result, true)
    }
}
