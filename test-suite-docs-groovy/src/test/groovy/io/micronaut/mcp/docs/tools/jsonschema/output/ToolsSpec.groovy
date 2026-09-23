package io.micronaut.mcp.docs.tools.jsonschema.output

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_TOOLS_CALL_WITH_OUTPUT_SCHEMA
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_TOOLS_LIST_WITH_INPUT_AND_OUTPUT_SCHEMA
import static io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_CALL
import static io.micronaut.mcp.docs.JsonRpcMessages.TOOLS_LIST

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "OutputJsonSchemaToolsSpec")
@MicronautTest
class ToolsSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "tools list"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_LIST))

        then:
        JSONAssert.assertEquals(EXPECTED_TOOLS_LIST_WITH_INPUT_AND_OUTPUT_SCHEMA, result, true)
    }

    void "tools call"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_CALL))

        then:
        JSONAssert.assertEquals(EXPECTED_TOOLS_CALL_WITH_OUTPUT_SCHEMA, result, true)
    }
}
