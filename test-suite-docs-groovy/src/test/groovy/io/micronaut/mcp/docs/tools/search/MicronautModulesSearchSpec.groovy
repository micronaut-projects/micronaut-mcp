package io.micronaut.mcp.docs.tools.search

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.mcp.server.tools.search.SearchTool
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_SEARCH_TOOL_CALL
import static io.micronaut.mcp.docs.JsonRpcMessages.SEARCH_TOOL_CALL
import static io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MicronautModulesSearchSpec")
@MicronautTest
class MicronautModulesSearchSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    @Inject
    SearchTool tool

    void "search tool"() {
        expect:
        tool.name == "search"
        tool.title == "Search"

        when:
        String json = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_LIST))

        then:
        json.contains('"name":"search"')

        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", SEARCH_TOOL_CALL))

        then:
        JSONAssert.assertEquals(EXPECTED_SEARCH_TOOL_CALL, result, true)
    }
}
