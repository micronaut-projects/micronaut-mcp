package io.micronaut.mcp.docs.tools.fetch

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_FETCH_TOOL_CALL
import io.micronaut.mcp.docs.JsonRpcMessages.FETCH_TOOL_CALL
import io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST
import io.micronaut.mcp.server.tools.fetch.FetchTool
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MicronautModulesFetchTest")
@MicronautTest
class MicronautModulesFetchTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Inject
    lateinit var tool: FetchTool

    @Test
    fun fetchTool() {
        assertEquals("fetch", tool.name)
        assertEquals("Fetch", tool.title)
        val client = httpClient.toBlocking()
        val json = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)) }
        assertTrue(json.contains("\"name\":\"fetch\""), json)
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", FETCH_TOOL_CALL)) }
        JSONAssert.assertEquals(EXPECTED_FETCH_TOOL_CALL, result, true)
    }
}
