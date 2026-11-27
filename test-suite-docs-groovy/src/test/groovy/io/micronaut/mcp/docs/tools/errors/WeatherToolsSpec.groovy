package io.micronaut.mcp.docs.tools.errors

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "WeatherToolsSpec")
@MicronautTest
class WeatherToolsSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "an expected failure is an error result"() {
        expect:
        JSONAssert.assertEquals('''
            {"result":{"content":[{"type":"text","text":"Unknown city Paris, use Madrid or London"}],"isError":true}}''',
            call("temperature", "Paris"), false)
    }

    void "an exception is a tool execution error"() {
        expect:
        JSONAssert.assertEquals('''
            {"result":{"content":[{"type":"text","text":"Tool forecast failed"}],"isError":true}}''',
            call("forecast", "Madrid"), false)
    }

    void "an McpError is a protocol error"() {
        when:
        httpClient.toBlocking().retrieve(request("alerts", "Madrid"))

        then:
        HttpClientResponseException e = thrown()
        JSONAssert.assertEquals('''
            {"error":{"code":-32603,"message":"Weather alerts are disabled on this server"}}''',
            e.response.getBody(String).orElseThrow(), false)
    }

    private String call(String tool, String city) {
        httpClient.toBlocking().retrieve(request(tool, city))
    }

    private static HttpRequest<String> request(String tool, String city) {
        HttpRequest.POST("/mcp", """
            {"jsonrpc":"2.0","id":1,"method":"tools/call","params":{"name":"${tool}","arguments":{"city":"${city}"}}}""".toString())
    }
}
