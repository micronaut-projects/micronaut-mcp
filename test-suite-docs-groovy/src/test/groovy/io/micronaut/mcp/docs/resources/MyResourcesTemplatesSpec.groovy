package io.micronaut.mcp.docs.resources

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.http.client.exceptions.HttpClientResponseException
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_TEMPLATES_LIST
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_ROUND_2
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_ROUND_99
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_TEMPLATES_LIST

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MyResourcesTemplatesSpec")
@MicronautTest
class MyResourcesTemplatesSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "resources templates list"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_TEMPLATES_LIST))

        then:
        JSONAssert.assertEquals(EXPECTED_RESOURCES_TEMPLATES_LIST, result, true)
    }

    void "resources read"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_2))

        then:
        result.contains('"uri":"pgn://round/2"')
        result.contains('"mimeType":"application/x-chess-pgn"')
        result.contains('[Round \\"2\\"]')
    }

    void "resources read not found"() {
        when:
        httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_99))

        then:
        HttpClientResponseException ex = thrown()
        String body = ex.response.getBody(String).orElse("")
        body.contains('"error"')
        body.contains("resource for round not found")
    }
}
