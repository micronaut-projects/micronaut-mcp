package io.micronaut.mcp.server.stdio;

import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.util.StringUtils;
import io.micronaut.json.JsonMapper;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.mcp.server.context.McpRequestContext;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A tool with an {@link McpRequestContext} over STDIO: its notifications reach the client, and it can ask the client for
 * information while it runs. Subclasses run it on the synchronous and the reactive server.
 */
abstract class AbstractRequestContextStdioTest {
    static final String PROPERTY = "request-context-stdio-test";

    @Inject
    StdioFactory factory;

    @Inject
    JsonMapper jsonMapper;

    /**
     * @return The name of the tool to call
     */
    abstract String tool();

    @Test
    @Timeout(30)
    void notificationsElicitationsAndSamplingGoThroughTheExchange() throws IOException {
        Stdio stdio = factory.stdio;
        BufferedReader server = new BufferedReader(new InputStreamReader(stdio.serverToClient, StandardCharsets.UTF_8));
        stdio.sendRequest(INITIALIZE);
        assertTrue(server.readLine().contains("\"logging\":{}"), "the logging capability is advertised");
        stdio.sendRequest(INITIALIZED);
        // STDIO messages are delimited by newlines, so each one is a single line
        stdio.sendRequest("""
            {"jsonrpc": "2.0", "id": 1, "method": "tools/call", \
            "params": {"name": "%s", "arguments": {}, "_meta": {"progressToken": "p1"}}}""".formatted(tool()));

        Map<?, ?> progress = read(server);
        assertEquals(McpSchema.METHOD_NOTIFICATION_PROGRESS, progress.get("method"));
        assertEquals("p1", ((Map<?, ?>) progress.get("params")).get("progressToken"));
        Map<?, ?> log = read(server);
        assertEquals(McpSchema.METHOD_NOTIFICATION_MESSAGE, log.get("method"));
        assertEquals("asking for a name", ((Map<?, ?>) log.get("params")).get("data"));

        Map<?, ?> elicitation = read(server);
        assertEquals(McpSchema.METHOD_ELICITATION_CREATE, elicitation.get("method"));
        answer(elicitation, Map.of("action", "accept", "content", Map.of("name", "Ada")));
        Map<?, ?> sampling = read(server);
        assertEquals(McpSchema.METHOD_SAMPLING_CREATE_MESSAGE, sampling.get("method"));
        answer(sampling, Map.of("role", "assistant", "content", Map.of("type", "text", "text", "Bonjour"),
            "model", "model", "stopReason", "endTurn"));

        Map<?, ?> result = read(server);
        assertEquals(1, ((Number) result.get("id")).intValue());
        assertTrue(String.valueOf(result.get("result")).contains("Bonjour Ada (END_TURN)"), String.valueOf(result));
    }

    private void answer(Map<?, ?> request, Map<String, Object> result) throws IOException {
        factory.stdio.sendRequest(jsonMapper.writeValueAsString(Map.of("jsonrpc", "2.0", "id", request.get("id"), "result", result)));
    }

    private Map<?, ?> read(BufferedReader server) throws IOException {
        return jsonMapper.readValue(server.readLine(), Map.class);
    }

    @Requires(property = PROPERTY, value = StringUtils.TRUE)
    @Factory
    static class StdioFactory implements AutoCloseable {
        final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(McpJsonMapper jsonMapper) {
            return new StdioServerTransportProvider(jsonMapper, stdio.serverStdin, stdio.serverStdout);
        }

        @PreDestroy
        @Override
        public void close() {
            stdio.close();
        }
    }

    @Requires(property = PROPERTY, value = StringUtils.TRUE)
    @Singleton
    static class Greeter {
        @Tool
        String greet(McpRequestContext context) {
            context.progress(1, 2.0, "asking");
            context.log(McpSchema.LoggingLevel.INFO, "greeter", "asking for a name");
            McpSchema.ElicitResult name = context.elicit(McpSchema.ElicitRequest.builder()
                .message("What is your name?")
                .requestedSchema(Map.of("type", "object", "properties", Map.of("name", Map.of("type", "string"))))
                .build());
            McpSchema.CreateMessageResult greeting = context.sample(McpSchema.CreateMessageRequest.builder()
                .messages(List.of(new McpSchema.SamplingMessage(McpSchema.Role.USER, new McpSchema.TextContent("Say hello in French"))))
                .maxTokens(10)
                .build());
            return ((McpSchema.TextContent) greeting.content()).text() + " " + name.content().get("name") + " (" + greeting.stopReason() + ")";
        }

        // A reactive server invokes a method without an executor on the thread that reads the messages of the client
        @Tool
        @ExecuteOn(TaskExecutors.BLOCKING)
        String greetOnBlockingExecutor(McpRequestContext context) {
            return greet(context);
        }
    }
}
