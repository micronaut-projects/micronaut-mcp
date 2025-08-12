package example.micronaut.tools;

import io.micronaut.context.annotation.Factory;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.util.ArrayList;
import java.util.List;

@Factory
class FenEvaluationTool {
    public static final String FEN_EVALUATION_SCHEMA = """
            {
              "type" : "object",
              "id" : "urn:jsonschema:Operation",
              "properties" : {
                "fen" : {
                  "type" : "string"
                }
              }
            }
            """;;

    @Named("fenEvaluation")
    @Singleton
    McpStatelessServerFeatures.SyncToolSpecification getAlertsTools() {
        McpSchema.Tool tool = new McpSchema.Tool("fenEvaluation",
            "Evaluate a chess position using a FEN string.", FEN_EVALUATION_SCHEMA);
        return new McpStatelessServerFeatures.SyncToolSpecification(tool,
            (exchange, arguments) -> {
                //TODO invoke stockfish
                List<McpSchema.Content> contents = new ArrayList<>();
                contents.add(new McpSchema.TextContent("+0.23"));
                return new McpSchema.CallToolResult(contents, false);
            }
        );
    }
}
