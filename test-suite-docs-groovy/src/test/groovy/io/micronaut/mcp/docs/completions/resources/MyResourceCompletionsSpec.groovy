package io.micronaut.mcp.docs.completions.resources

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_READ_REPORT
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCE_COMPLETION
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_REPORT
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCE_COMPLETION_REQUEST

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MyResourceCompletionsSpec")
@MicronautTest
class MyResourceCompletionsSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "resource completion"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCE_COMPLETION_REQUEST))

        then:
        JSONAssert.assertEquals(EXPECTED_RESOURCE_COMPLETION, result, true)
    }

    void "resource template read"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_REPORT))

        then:
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ_REPORT, result, true)
    }
}
