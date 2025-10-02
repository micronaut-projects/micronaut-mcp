package io.micronaut.mcp.server.stateless.sync.resources;

import io.micronaut.context.annotation.EachProperty;
import io.micronaut.context.annotation.Parameter;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.naming.Named;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.server.utils.PgnLoader;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_LIST_TEMPLATES;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.EXPECTED_RESOURCES_READ;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_READ;
import static io.micronaut.mcp.server.utils.JsonRpcMessages.RESOURCES_TEMPLATES_LIST;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Property(name = "micronaut.mcp.server.info.name", value="world-chess-championship-2024-pgn")
@Property(name = "micronaut.mcp.server.info.version", value="0.0.1")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
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
@Property(name = "spec.name", value = "ResourceTemplatePathVariablesTest")
@MicronautTest
class ResourceTemplatePathVariablesTest {

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

    @Requires(property = "spec.name", value = "ResourceTemplatePathVariablesTest")
    @Singleton
    static class MyResources {
        private static final String PGN_MIME_TYPE = "application/x-chess-pgn";
        private final PgnLoader pgnLoader;

        MyResources(PgnLoader pgnLoader) {
            this.pgnLoader = pgnLoader;
        }

        @ResourceTemplate(uriTemplate = "pgn://round/{round}",
            mimeType = PGN_MIME_TYPE,
            name = "2024ChessChampionshipRoundPgn",
            title = "PGN of a round World Chess Championship 2024",
            description = "Given a round, it returns a PGN of the World Chess Championship 2024 between Ding Liren and Gukesh Dommaraju")
        String pgn(Integer round) {
            return pgnLoader.loadPgn(round)
                .orElseThrow(() -> new McpError(new McpSchema.JSONRPCResponse.JSONRPCError(McpSchema.ErrorCodes.RESOURCE_NOT_FOUND, "resource for round not found", null)));
        }
    }

    @Requires(property = "spec.name", value = "ResourceTemplatePathVariablesTest")
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
}
