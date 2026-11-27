package io.micronaut.mcp.docs.tools.errors

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.junit5.annotation.MicronautTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.skyscreamer.jsonassert.JSONAssert

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "WeatherToolsTest")
@MicronautTest
class WeatherToolsTest {

    @Inject
    @field:Client("/")
    lateinit var httpClient: HttpClient

    @Test
    fun anExpectedFailureIsAnErrorResult() {
        JSONAssert.assertEquals("""
            {"result":{"content":[{"type":"text","text":"Unknown city Paris, use Madrid or London"}],"isError":true}}""",
            call("temperature", "Paris"), false)
    }

    @Test
    fun anExceptionIsAToolExecutionError() {
        JSONAssert.assertEquals("""
            {"result":{"content":[{"type":"text","text":"Tool forecast failed"}],"isError":true}}""",
            call("forecast", "Madrid"), false)
    }

    @Test
    fun anMcpErrorIsAProtocolError() {
        val client = httpClient.toBlocking()
        val request = request("alerts", "Madrid")
        val e = assertThrows(HttpClientResponseException::class.java) { client.retrieve(request) }
        JSONAssert.assertEquals("""
            {"error":{"code":-32603,"message":"Weather alerts are disabled on this server"}}""",
            e.response.getBody(String::class.java).orElseThrow(), false)
    }

    private fun call(tool: String, city: String): String {
        val client = httpClient.toBlocking()
        val request = request(tool, city)
        return assertDoesNotThrow<String> { client.retrieve(request) }
    }

    private fun request(tool: String, city: String): HttpRequest<String> =
        HttpRequest.POST("/mcp", """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"$tool","arguments":{"city":"$city"}}}""")
}
