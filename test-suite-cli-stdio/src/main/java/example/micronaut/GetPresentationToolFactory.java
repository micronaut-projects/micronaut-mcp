package example.micronaut;

import io.micronaut.context.annotation.Factory;
import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Factory
class GetPresentationToolFactory {
    private static final PresentationTools presentationTools = new PresentationTools();

    @Singleton
    McpServerFeatures.SyncToolSpecification createSyncToolSpecification() {
        var schema = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                "operation" : {
                  "type" : "string"
                }
              }
            }
            """;
        var syncToolSpecification = new McpServerFeatures.SyncToolSpecification(
                new McpSchema.Tool("get_presentations", "Get a list of all presentations from JavaOne", schema),
                (exchange, arguments) -> {
                    List<Presentation> presentations = presentationTools.getPresentations();
                    List<McpSchema.Content> contents = new ArrayList<>();
                    for (Presentation presentation : presentations) {
                        contents.add(new McpSchema.TextContent(presentation.toString()));
                    }
                    return new McpSchema.CallToolResult(contents, false);
                }
        );
        return syncToolSpecification;
    }
}
