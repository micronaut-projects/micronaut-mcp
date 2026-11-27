package io.micronaut.mcp.server.stdio;

import io.micronaut.context.annotation.Property;
import io.micronaut.core.util.StringUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Property(name = AbstractRequestContextStdioTest.PROPERTY, value = StringUtils.TRUE)
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@MicronautTest(startApplication = false)
class AsyncRequestContextStdioTest extends AbstractRequestContextStdioTest {
    @Override
    String tool() {
        return "greetOnBlockingExecutor";
    }

    @Override
    void thenOnTheSameSession(BufferedReader server) throws IOException {
        // Without an executor, the method runs on the thread that reads the answers of the client, so it fails fast
        int id = 2;
        for (String name : List.of("greet", "deferredGreet")) {
            factory.stdio.sendRequest("""
                {"jsonrpc": "2.0", "id": %d, "method": "tools/call", "params": {"name": "%s", "arguments": {}}}""".formatted(id, name));
            Map<?, ?> log = read(server);
            assertEquals(McpSchema.METHOD_NOTIFICATION_MESSAGE, log.get("method"));
            Map<?, ?> result = read(server);
            assertEquals(id, ((Number) result.get("id")).intValue());
            String text = String.valueOf(result);
            // the advice to annotate the method with @ExecuteOn(TaskExecutors.BLOCKING), meant for the developer, is logged
            assertTrue(text.contains("isError=true") && text.contains("Tool " + name + " failed"), text);
            id++;
        }
    }
}
