package io.micronaut.mcp.docs.resources

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_LIST_HELLO
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_READ_HELLO
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_LIST
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_HELLO

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "ResourcesSpec")
@MicronautTest
class ResourcesSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "resources list"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_LIST))

        then:
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST_HELLO, result, true)
    }

    void "resources read"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_HELLO))

        then:
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ_HELLO, result, true)
    }
}
