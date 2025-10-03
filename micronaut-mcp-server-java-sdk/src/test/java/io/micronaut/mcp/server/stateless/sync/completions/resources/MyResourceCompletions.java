package io.micronaut.mcp.server.stateless.sync.completions.resources;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.ResourceCompletion;
import jakarta.inject.Singleton;

import java.util.List;

@Requires(property = "spec.name", value = "StatelessSyncResourceCompletionsTest")
//tag::clazz[]
@Singleton
class MyResourceCompletions {
    @ResourceCompletion(uri =  "file:///home/user/documents/{fileName}")
    List<String> resourcesCompletions(String fileName) {
        return List.of(
                "report.pdf",
                "data.csv",
                "notes.txt"
            ).stream()
            .filter(name -> name.startsWith(fileName))
            .toList();
    }
}
//end::clazz[]
