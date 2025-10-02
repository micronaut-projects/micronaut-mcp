package io.micronaut.mcp.server.stdio.async;

import io.micronaut.context.annotation.*;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.naming.Named;
import io.micronaut.core.util.StringUtils;
import io.micronaut.mcp.server.utils.PgnLoader;
import io.micronaut.mcp.server.utils.ResourceLoaderUtils;
import io.micronaut.mcp.server.utils.Stdio;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.json.McpJsonMapper;
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

@Property(name = "pgn.rounda.path", value = "classpath:fidewwc2024/round_1.pgn")
@Property(name = "pgn.rounda.round", value = "1")
@Property(name = "pgn.roundb.path", value = "classpath:fidewwc2024/round_2.pgn")
@Property(name = "pgn.roundb.round", value = "2")
@Property(name = "pgn.roundc.path", value = "classpath:fidewwc2024/round_3.pgn")
@Property(name = "pgn.roundc.round", value = "3")
@Property(name = "pgn.roundd.path", value = "classpath:fidewwc2024/round_4.pgn")
@Property(name = "pgn.roundd.round", value = "4")
@Property(name = "pgn.rounde.path", value = "classpath:fidewwc2024/round_5.pgn")
@Property(name = "pgn.rounde.round", value = "5")
@Property(name = "pgn.roundf.path", value = "classpath:fidewwc2024/round_6.pgn")
@Property(name = "pgn.roundf.round", value = "6")
@Property(name = "pgn.roundg.path", value = "classpath:fidewwc2024/round_7.pgn")
@Property(name = "pgn.roundg.round", value = "7")
@Property(name = "pgn.roundh.path", value = "classpath:fidewwc2024/round_8.pgn")
@Property(name = "pgn.roundh.round", value = "8")
@Property(name = "pgn.roundi.path", value = "classpath:fidewwc2024/round_9.pgn")
@Property(name = "pgn.roundi.round", value = "9")
@Property(name = "pgn.roundj.path", value = "classpath:fidewwc2024/round_10.pgn")
@Property(name = "pgn.roundj.round", value = "10")
@Property(name = "pgn.roundk.path", value = "classpath:fidewwc2024/round_11.pgn")
@Property(name = "pgn.roundk.round", value = "11")
@Property(name = "pgn.roundl.path", value = "classpath:fidewwc2024/round_12.pgn")
@Property(name = "pgn.roundl.round", value = "12")
@Property(name = "pgn.roundm.path", value = "classpath:fidewwc2024/round_13.pgn")
@Property(name = "pgn.roundm.round", value = "13")
@Property(name = "pgn.roundn.path", value = "classpath:fidewwc2024/round_14.pgn")
@Property(name = "pgn.roundn.round", value = "14")
@Property(name = "micronaut.mcp.server.info.name", value="world-chess-championship-2024-pgn")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "STDIO")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
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
    @EachProperty("pgn")
    static class PgnFile implements Named {
        private final String name;
        private String path;
        private Integer round;

        PgnFile(@Parameter String name) {
            this.name = name;
        }

        public Integer getRound() {
            return round;
        }

        public void setRound(Integer round) {
            this.round = round;
        }

        @Override
        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public void setPath(String path) {
            this.path = path;
        }
    }

    @Requires(property = "spec.name", value = "AsyncResourcesTest")
    @Factory
    static class AsyncResourcesFactory implements AutoCloseable {
        public final Stdio stdio = new Stdio();

        @Prototype
        @Replaces(McpServerTransportProvider.class)
        McpServerTransportProvider stdioServerTransportProviderReplacement(McpJsonMapper mcpJsonMapper) {
            return new StdioServerTransportProvider(mcpJsonMapper, stdio.serverStdin, stdio.serverStdout);
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
        private final ResourceLoader resourceLoader;

        ResourcesFactory(ResourceLoader resourceLoader, PgnLoader pgnLoader) {
            this.resourceLoader = resourceLoader;
            this.pgnLoader = pgnLoader;
        }

        @Singleton
        McpServerFeatures.AsyncResourceTemplateSpecification createPgnResourceTemplate() {
            String uriTemplate = "pgn://round/{round}";
            String name = "2024ChessChampionshipRoundPgn";
            String title = "PGN of a round World Chess Championship 2024";
            String description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju";
            return new McpServerFeatures.AsyncResourceTemplateSpecification(
                new McpSchema.ResourceTemplate(uriTemplate, name, title, description, PGN_MIME_TYPE, null, null),
                (mcpTransportContext, readResourceRequest) -> {
                    Integer round = round(readResourceRequest.uri());
                    return Mono.just(readResourceResult(readResourceRequest.uri(), round));
                });
        }

        @EachBean(PgnFile.class)
        @Singleton
        McpServerFeatures.AsyncResourceSpecification createPgnSyncResourceSpecification(PgnFile pgnFile) throws IOException {
            McpSchema.Resource resource = getResource(pgnFile);
            return new McpServerFeatures.AsyncResourceSpecification(resource, (mcpSyncServerExchange, readResourceRequest) -> {
                String uri = readResourceRequest.uri();
                Integer round = round(readResourceRequest.uri());
                return Mono.just(readResourceResult(uri, round));
            });
        }

        private Integer round(String uri) {
            int lastSlash = uri.lastIndexOf('/');
            String roundStr = uri.substring(lastSlash + 1);
            return Integer.parseInt(roundStr);
        }

        private McpSchema.ReadResourceResult readResourceResult(String uri, Integer round) {
            List<McpSchema.ResourceContents> contents = new ArrayList<>();
            pgnLoader.loadPgn(round).ifPresent(text ->
                contents.add(new McpSchema.TextResourceContents(uri, PGN_MIME_TYPE, text)));
            return new McpSchema.ReadResourceResult(contents);
        }

        private McpSchema.Resource getResource(PgnFile pgnFile) throws IOException {
            return ResourceLoaderUtils.size(resourceLoader, pgnFile.getPath())
                .map(size -> {

                    Integer round = pgnFile.getRound();
                    String uri = "pgn://round/" + round;
                    String name = "round" + round + "PgnFideWCC2024";
                    String title = "PGN of the Round " + round + " game of the World Chess Championship";
                    String description = title + " between Ding Liren and Gukesh Dommaraju";
                    return new McpSchema.Resource(uri, name, title, description, PGN_MIME_TYPE, size, null, null);
                }).orElseThrow(() -> new ConfigurationException("unable find resource for path " + pgnFile.getPath()));
        }
    }
}
