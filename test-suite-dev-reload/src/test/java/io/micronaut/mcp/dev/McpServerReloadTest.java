/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.mcp.dev;

import io.micronaut.context.ApplicationContext;
import io.micronaut.dev.tck.ReloadHarness;
import io.micronaut.dev.tck.ReloadTck;
import io.micronaut.runtime.server.EmbeddedServer;
import io.netty.util.internal.PlatformDependent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs an MCP server over HTTP through the development runtime and changes an application tool: the next generation
 * serves the new tool on the same port, which the retired one released, and nothing of the MCP server keeps a
 * retired generation reachable.
 */
class McpServerReloadTest {

    private static final String TOOLS = """
        package example;

        @jakarta.inject.Singleton
        public class GreetingTools {
            @io.micronaut.mcp.annotations.Tool(name = "%s", description = "Greets")
            public String greet() {
                return "%s";
            }
        }
        """;

    private final HttpClient client = HttpClient.newHttpClient();

    @TempDir
    Path project;

    @BeforeAll
    static void initializeNetty() {
        // Netty records why it cannot use Unsafe in a static exception, whose stack trace holds the classes on the
        // stack when Netty is first loaded. Loaded first by the application's main method, that is generation one's
        // Application class, which Netty would keep reachable for the life of the process: loaded here, it is this test
        PlatformDependent.hasUnsafe();
        // Reactor's global bounded elastic scheduler, which the SDK runs the handlers of a synchronous server on,
        // creates its threads when first used, and a thread keeps the context class loader of the thread that created
        // it: created by the first request, that is generation one's loader, for as long as the idle thread is cached.
        // Created here, the worker and the evictor have this test's loader
        Mono.fromCallable(() -> 1).subscribeOn(Schedulers.boundedElastic()).block();
    }

    @Test
    void theServerFollowsAReloadOnTheSamePortAndLeavesNoRetiredGenerationReachable() throws Exception {
        int port = freePort();
        try (ReloadHarness harness = ReloadHarness.inDirectory(project)) {
            harness.property("micronaut.server.port", String.valueOf(port))
                .property("micronaut.mcp.server.transport", "HTTP")
                .property("micronaut.mcp.server.info.name", "reload")
                .property("micronaut.mcp.server.info.version", "1.0.0");
            harness.source("example.GreetingTools", TOOLS.formatted("greet", "hello"));
            harness.start();
            assertReloaderPresent(harness.context());
            int firstContext = System.identityHashCode(harness.context());

            String tools = call(port, "tools/list", "{}");
            assertTrue(tools.contains("\"greet\""), tools);
            assertTrue(call(port, "tools/call", "{\"name\":\"greet\",\"arguments\":{}}").contains("hello"));

            // the tool is renamed and answers differently
            int generation = harness.generation();
            harness.source("example.GreetingTools", TOOLS.formatted("welcome", "welcome"));
            harness.reload();
            assertTrue(harness.generation() > generation);
            assertReloaderPresent(harness.context());
            // a class change of the application restarts it: a new context builds a new server
            assertNotEquals(firstContext, System.identityHashCode(harness.context()));
            assertEquals(port, harness.context().getBean(EmbeddedServer.class).getPort());

            // served on the same port, which the retired generation released
            tools = call(port, "tools/list", "{}");
            assertTrue(tools.contains("\"welcome\""), tools);
            assertFalse(tools.contains("\"greet\""), tools);
            assertTrue(call(port, "tools/call", "{\"name\":\"welcome\",\"arguments\":{}}").contains("welcome"));

            // neither the server, its transport, the registries nor the development-only reloader keep a retired
            // generation reachable
            ReloadTck.assertRetiredGenerationsCollected(harness);
        }
    }

    private String call(int port, String method, String params) throws IOException, InterruptedException {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/mcp"))
            .header("Content-Type", "application/json")
            .header("Accept", "application/json, text/event-stream")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(200, response.statusCode(), response.body());
        return response.body();
    }

    private static int freePort() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    private static void assertReloaderPresent(ApplicationContext context) {
        // the bean that follows the annotated methods exists in development mode only
        assertTrue(context.containsBean(type(context, "io.micronaut.mcp.server.registry.DevelopmentMcpReloader")));
    }

    private static Class<?> type(ApplicationContext context, String className) {
        try {
            return Class.forName(className, true, context.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new AssertionError(className + " is not in the application", e);
        }
    }
}
