package io.micronaut.mcp.server.stdio.async;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.EachBean;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Prototype;
import io.micronaut.context.annotation.Replaces;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.mcp.server.utils.PgnLoader;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.server.transport.StdioServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import io.modelcontextprotocol.spec.McpServerTransportProvider;
import jakarta.annotation.PreDestroy;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_LIST_TEMPLATES;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_READ;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZE;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.INITIALIZED;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_READ;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_TEMPLATES_LIST;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Property(name = "micronaut.mcp.server.info.name", value="world-chess-championship-2024-pgn")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.type", value = "ASYNC")
@Property(name = "spec.name", value = "AsyncResourcesTest")
@MicronautTest
class AsyncResourcesTest {
    @Inject
    AsyncResourcesFactory factory;

    @SuppressWarnings("java:S2925")
    @Test
    void asyncResources() throws IOException, InterruptedException, JSONException {
        factory.stdio.sendRequest(INITIALIZE);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(INITIALIZED);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(RESOURCES_READ);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(RESOURCES_TEMPLATES_LIST);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        factory.stdio.sendRequest(RESOURCES_LIST);
        Thread.sleep(TimeUnit.SECONDS.toMillis(1));
        List<String> responses = factory.stdio.readResponses();
        assertEquals(4, responses.size());
        String readJsonRpc = responses.get(1);
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ, readJsonRpc, true);

        String templatesJsonRpc = responses.get(2);
        String expectedTemplatedList = EXPECTED_RESOURCES_LIST_TEMPLATES;
        JSONAssert.assertEquals(expectedTemplatedList, templatesJsonRpc, true);

        String resourcesListJsonRpc = responses.get(3);
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST, resourcesListJsonRpc, true);
    }

    @Requires(property = "spec.name", value = "AsyncResourcesTest")
    @Factory
    static class AsyncResourcesFactory implements AutoCloseable {
        public final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(ObjectMapper objectMapper) {
            return new StdioServerTransportProvider(objectMapper, stdio.serverStdin, stdio.serverStdout);
        }

        @PreDestroy
        @Override
        public void close() throws IOException {
            stdio.close();
        }
    }

    @Requires(property = "spec.name", value = "AsyncResourcesTest")
    @Factory
    static class ResourcesFactory {
        public static final String PGN_MIME_TYPE = "application/x-chess-pgn";

        private final PgnLoader pgnLoader;

        ResourcesFactory(BeanContext beanContext, ResourceLoader resourceLoader, PgnLoader pgnLoader) {
            this.pgnLoader = pgnLoader;
            for (int round = 1; round <= 14; round++) {
                Optional<InputStream> roundPgnInputStreamOptional = resourceLoader.getResourceAsStream("classpath:fidewwc2024/round_" + round + ".pgn");
                if (roundPgnInputStreamOptional.isPresent()) {
                    try {
                        Long size = Long.valueOf(roundPgnInputStreamOptional.get().readAllBytes().length);
                        String uri = "pgn://round/" + round;
                        String name = "round" + round + "PgnFideWCC2024";
                        String title = "PGN of the Round " + round + " game of the World Chess Championship";
                        String description = title + " between Ding Liren and Gukesh Dommaraju";
                        beanContext.registerSingleton(new McpSchema.Resource(uri, name, title, description, PGN_MIME_TYPE, size, null, null));
                    } catch (IOException e) {
                        throw new ConfigurationException("unable to calculate the size of the resource");
                    }
                }
            }
        }

        @Singleton
        McpSchema.ResourceTemplate createPgnResourceTemplate() {
            String uriTemplate = "pgn://round/{round}";
            String name = "2024ChessChampionshipRoundPgn";
            String title = "PGN of a round World Chess Championship 2024";
            String description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju";
            return new McpSchema.ResourceTemplate(uriTemplate, name, title, description, PGN_MIME_TYPE, null, null);
        }

        @EachBean(McpSchema.Resource.class)
        @Singleton
        McpServerFeatures.AsyncResourceSpecification createPgnSyncResourceSpecification(McpSchema.Resource resource) {
            return new McpServerFeatures.AsyncResourceSpecification(resource, (mcpSyncServerExchange, readResourceRequest) -> {
                String uri = readResourceRequest.uri();
                int lastSlash = uri.lastIndexOf('/');
                String roundStr = uri.substring(lastSlash + 1);
                int round = Integer.parseInt(roundStr);
                List<McpSchema.ResourceContents> contents = new ArrayList<>();
                pgnLoader.loadPgn(round).ifPresent(text ->
                    contents.add(new McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, text)));
                return Mono.just(new McpSchema.ReadResourceResult(contents));
            });
        }
    }
}
