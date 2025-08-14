package io.micronaut.mcp.server.stateless.sync;
/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
 */
//tag::imports[]
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Tool;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.util.List;
import java.util.Map;
//end::imports[]

@Requires(property = "spec.name", value = "StatelessSyncToolAnnotationTest")
//tag::clazz[]
@Singleton
class Tools {
    @Tool(description = "Evaluate a chess position using a FEN string.")
    String fenEvaluation(String fen) {
        return "+0.27";
    }
}
//end::clazz[]
