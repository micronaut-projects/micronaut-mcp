package io.micronaut.mcp.docs.tools.toolarg

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_TOOLS_CALL
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_TOOLS_LIST
import io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_CALL
import io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ToolArgToolsTest")
@MicronautTest
class ToolsTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun toolsList() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)) }
        JSONAssert.assertEquals(EXPECTED_TOOLS_LIST, result, true)
    }

    @Test
    fun toolsCall() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", TOOLS_CALL)) }
        JSONAssert.assertEquals(EXPECTED_TOOLS_CALL, result, true)
    }
}
