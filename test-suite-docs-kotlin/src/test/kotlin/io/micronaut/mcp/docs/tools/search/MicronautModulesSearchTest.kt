package io.micronaut.mcp.docs.tools.search

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_SEARCH_TOOL_CALL
import io.micronaut.mcp.docs.JsonRpcMessages.SEARCH_TOOL_CALL
import io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST
import io.micronaut.mcp.server.tools.search.SearchTool
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MicronautModulesSearchTest")
@MicronautTest
class MicronautModulesSearchTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Inject
    lateinit var tool: SearchTool

    @Test
    fun searchTool() {
        assertEquals("search", tool.name)
        assertEquals("Search", tool.title)
        val client = httpClient.toBlocking()
        val json = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)) }
        assertTrue(json.contains("\"name\":\"search\""), json)
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", SEARCH_TOOL_CALL)) }
        JSONAssert.assertEquals(EXPECTED_SEARCH_TOOL_CALL, result, true)
    }
}
