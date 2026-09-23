package io.micronaut.mcp.docs.completions.prompts

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_PROMPT_COMPLETION
import static io.micronaut.mcp.docs.JsonRpcMessages.PROMPT_COMPLETION_REQUEST

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MyPromptsCompletionsSpec")
@MicronautTest
class MyPromptsCompletionsSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "prompt completion"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", PROMPT_COMPLETION_REQUEST))

        then:
        JSONAssert.assertEquals(EXPECTED_PROMPT_COMPLETION, result, true)
    }
}
