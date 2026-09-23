package io.micronaut.mcp.server.stateless.sync.prompts;

import io.micronaut.context.annotation.Property;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.server.stateless.NoCapabilityTest;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.json.JSONException;
import org.junit.jupiter.api.Test;

@Property(name = "micronaut.mcp.server.info.name", value="test mcp server")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@MicronautTest
class PromptsListNoPromptsCapabilityTest extends NoCapabilityTest {
    @Override
    @Test
    public void testNoCapabilityTest(@Client("/") HttpClient httpClient) throws JSONException {
        super.testNoCapabilityTest(httpClient);
    }

    @Override
    protected String getRequest() {
        return """
        {"jsonrpc": "2.0","id": 1,"method":"prompts/list","params":{}}""";
    }

    @Override
    protected String getExpectedResponse() {
        return """
{"jsonrpc":"2.0","id":1,"error":{"code":-32601,"message":"Method not found: prompts/list"}}""";
    }
}
