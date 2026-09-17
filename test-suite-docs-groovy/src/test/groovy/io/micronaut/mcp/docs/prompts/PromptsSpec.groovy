package io.micronaut.mcp.docs.prompts

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_PROMPTS_GET
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_PROMPTS_LIST
import static io.micronaut.mcp.docs.JsonRpcMessages.PROMPTS_GET
import static io.micronaut.mcp.docs.JsonRpcMessages.PROMPTS_LIST

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "PromptsSpec")
@MicronautTest
class PromptsSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "prompts list"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", PROMPTS_LIST))

        then:
        JSONAssert.assertEquals(EXPECTED_PROMPTS_LIST, result, true)
    }

    void "prompts get"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", PROMPTS_GET))

        then:
        JSONAssert.assertEquals(EXPECTED_PROMPTS_GET, result, true)
    }
}
