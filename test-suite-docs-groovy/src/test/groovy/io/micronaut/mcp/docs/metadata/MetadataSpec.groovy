package io.micronaut.mcp.docs.metadata

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import spock.lang.Specification

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "MetadataSpec")
@MicronautTest
class MetadataSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    void "icons and meta of a tool"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp",
            '{"jsonrpc": "2.0", "id": 1, "method": "tools/list", "params": {}}'))

        then:
        JSONAssert.assertEquals('''
            {"result": {"tools": [{"name": "forecast",
              "icons": [{"src": "https://example.com/sun.svg", "mimeType": "image/svg+xml", "sizes": ["any"]}],
              "_meta": {"com.example/category": "weather"}}]}}''', result, JSONCompareMode.LENIENT)
    }

    void "annotations of a resource"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp",
            '{"jsonrpc": "2.0", "id": 1, "method": "resources/list", "params": {}}'))

        then:
        JSONAssert.assertEquals('''
            {"result": {"resources": [{"uri": "weather://stations", "size": 4,
              "annotations": {"audience": ["assistant"], "priority": 0.5}}]}}''', result, JSONCompareMode.LENIENT)
    }
}
