package io.micronaut.mcp.docs.completions.resources;

//tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.ResourceCompletion;
import jakarta.inject.Singleton;

import java.util.List;

//end::imports[]
@Requires(property = "spec.name", value = "MyResourceCompletionsTest")
//tag::clazz[]
@Singleton
class MyResourceCompletions {
    @ResourceCompletion(uri = "file:///home/user/documents/{fileName}")
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
