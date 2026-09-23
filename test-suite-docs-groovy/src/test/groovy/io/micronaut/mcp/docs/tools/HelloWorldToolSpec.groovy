package io.micronaut.mcp.docs.tools

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_HELLO_WORLD_TOOL_ANNOTATIONS
import static io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "HelloWorldToolSpec")
@MicronautTest
class HelloWorldToolSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "tool annotations"() {
        when:
        String json = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_LIST))

        then:
        json.contains(EXPECTED_HELLO_WORLD_TOOL_ANNOTATIONS)
    }
}
