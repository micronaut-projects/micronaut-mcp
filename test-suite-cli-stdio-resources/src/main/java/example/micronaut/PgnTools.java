package example.micronaut;

import io.micronaut.context.annotation.Factory;
import io.micronaut.mcp.resources.Resource;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import java.util.List;
import java.util.stream.Collectors;

@Factory
public class PgnTools {

    private final String pgn;

    public PgnTools(List<Resource> resources) {
        pgn = resources.stream()
            .filter(resource -> PgnResourceFactory.PGN_MIME_TYPE.equals(resource.mimeType()))
            .map(Resource::uri)
            .map(FileUtils::text)
            .collect(Collectors.joining("\n"));
    }

    public static final String GET_PGN_SCHEMA = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
              }
            }
            """;

    @Named("getPgns")
    @Singleton
    McpServerFeatures.SyncToolSpecification getPgns() {
        McpSchema.Tool tool = new McpSchema.Tool("getPgns",
            "Tells how toget my over the board chess games as PGN format.", GET_PGN_SCHEMA);
        return new McpServerFeatures.SyncToolSpecification(tool,
            (exchange, arguments) -> {
                return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(pgn)), false);
            }
        );
    }
}
