package io.micronaut.mcp.docs.tools.fetch

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.server.tools.fetch.FetchTool
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_FETCH_TOOL_CALL
import static io.micronaut.mcp.docs.JsonRpcMessages.FETCH_TOOL_CALL
import static io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MicronautModulesFetchSpec")
@MicronautTest
class MicronautModulesFetchSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    @Inject
    FetchTool tool

    void "fetch tool"() {
        expect:
        tool.name == "fetch"
        tool.title == "Fetch"

        when:
        String json = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_LIST))

        then:
        json.contains('"name":"fetch"')

        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", FETCH_TOOL_CALL))

        then:
        JSONAssert.assertEquals(EXPECTED_FETCH_TOOL_CALL, result, true)
    }
}
