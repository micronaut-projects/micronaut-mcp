package io.micronaut.mcp.server.stateless.async;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.*;
import io.micronaut.context.exceptions.ConfigurationException;
import io.micronaut.core.io.ResourceLoader;
import io.micronaut.core.naming.Named;
import io.micronaut.core.util.StringUtils;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.server.utils.PgnLoader;
import io.micronaut.mcp.server.utils.ResourceLoaderUtils;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_LIST_TEMPLATES;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_READ;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_LIST;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_READ;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_TEMPLATES_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "micronaut.mcp.server.reactive", value = StringUtils.TRUE)
@Property(name = "spec.name", value = "StatelessAsyncResourceListTest")
@MicronautTest
class StatelessAsyncResourceListTest {

    @Inject
    BeanContext beanContext;

    @Test
    void pgnFiles() {
        Collection<PgnFile> files = beanContext.getBeansOfType(PgnFile.class);
        assertEquals(14, files.size());
    }

    @Test
    void testResourceRead(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", RESOURCES_READ);
        String jsonRpc = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_READ, jsonRpc, true);
    }

    @Test
    void testResourcesTemplates(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        HttpRequest<?> req = HttpRequest.POST("/mcp", RESOURCES_TEMPLATES_LIST);
        String jsonRpc = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST_TEMPLATES, jsonRpc, true);
    }

    @Test
    void testResourceList(@Client("/") HttpClient httpClient) throws JSONException {
        BlockingHttpClient client = httpClient.toBlocking();
        String input = RESOURCES_LIST;
        HttpRequest<?> req = HttpRequest.POST("/mcp", input);
        String jsonRpc = assertDoesNotThrow(() -> client.retrieve(req));
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST, jsonRpc, true);
    }

    @Requires(property = "spec.name", value = "StatelessAsyncResourceListTest")
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

    @Requires(property = "spec.name", value = "StatelessAsyncResourceListTest")
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
        McpStatelessServerFeatures.AsyncResourceTemplateSpecification createPgnResourceTemplate() {
            String uriTemplate = "pgn://round/{round}";
            String name = "2024ChessChampionshipRoundPgn";
            String title = "PGN of a round World Chess Championship 2024";
            String description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju";
            return new McpStatelessServerFeatures.AsyncResourceTemplateSpecification(
                new McpSchema.ResourceTemplate(uriTemplate, name, title, description, PGN_MIME_TYPE, null, null),
                (mcpTransportContext, readResourceRequest) -> {
                    Integer round = round(readResourceRequest.uri());
                    return Mono.just(readResourceResult(readResourceRequest.uri(), round));
                });
        }

        @EachBean(PgnFile.class)
        @Singleton
        McpStatelessServerFeatures.AsyncResourceSpecification createPgnSyncResourceSpecification(PgnFile pgnFile) throws IOException {
            McpSchema.Resource resource = getResource(pgnFile);
            return new McpStatelessServerFeatures.AsyncResourceSpecification(resource, (mcpSyncServerExchange, readResourceRequest) -> {
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
