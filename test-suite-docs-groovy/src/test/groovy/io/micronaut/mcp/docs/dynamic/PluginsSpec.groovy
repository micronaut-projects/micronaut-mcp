package io.micronaut.mcp.docs.dynamic

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import spock.lang.Specification

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.tools.list-changed", value = "true")
@Property(name = "spec.name", value = "PluginsSpec")
@MicronautTest
class PluginsSpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    @Inject
    Plugins plugins

    void "tools are installed and uninstalled at runtime"() {
        when:
        plugins.install("greeter")

        then:
        call("tools/list", "{}").contains('"name":"greeter"')
        call("tools/call", '{"name": "greeter", "arguments": {}}').contains('"text":"installed"')

        when:
        plugins.uninstall("greeter")

        then:
        !call("tools/list", "{}").contains('"name":"greeter"')
    }

    private String call(String method, String params) {
        httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp",
            '{"jsonrpc": "2.0", "id": 1, "method": "' + method + '", "params": ' + params + '}'))
    }
}
