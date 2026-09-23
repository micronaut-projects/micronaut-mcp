package io.micronaut.mcp.docs.resources

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_LIST_PGN
import io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_TEMPLATES_LIST
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_LIST
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_ROUND_2
import io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_TEMPLATES_LIST
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "pgn.rounda.path", value = "classpath:fidewwc2024/round_1.pgn")
@Property(name = "pgn.rounda.round", value = "1")
@Property(name = "pgn.roundb.path", value = "classpath:fidewwc2024/round_2.pgn")
@Property(name = "pgn.roundb.round", value = "2")
@Property(name = "pgn.roundc.path", value = "classpath:fidewwc2024/round_3.pgn")
@Property(name = "pgn.roundc.round", value = "3")
@Property(name = "spec.name", value = "ResourcesFactoryTest")
@MicronautTest
class ResourcesFactoryTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Inject
    lateinit var pgnLoader: PgnLoader

    @Test
    fun resourcesList() {
        val client = httpClient.toBlocking()
        val result = assertDoesNotThrow<String> { client.retrieve(HttpRequest.POST("/mcp", RESOURCES_LIST)) }
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST_PGN, result, JSONCompareMode.NON_EXTENSIBLE)
    }

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
        assertTrue(pgnLoader.loadPgn(2).orElseThrow().startsWith("[Event \"FIDE World Championship Match 2024\"]"))
    }
}
