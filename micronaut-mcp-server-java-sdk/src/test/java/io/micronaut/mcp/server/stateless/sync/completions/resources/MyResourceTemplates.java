package io.micronaut.mcp.server.stateless.sync.completions.resources;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.ResourceTemplate;
import jakarta.inject.Singleton;

@Requires(property = "spec.name", value = "StatelessSyncResourceCompletionsTest")
//tag::clazz[]
@Singleton
class  MyResourceTemplates {
    @ResourceTemplate(
        uriTemplate = "file:///home/user/documents/{fileName}",
        name = "userDocument",
        title = "User Document"
    )
    String ref(String fileName) {
        if (fileName.equals("report.pdf")) {
            return "Report PDF";
        } else if (fileName.equals("data.csv")) {
            return "Data CSV";
        } else if (fileName.equals("notes.txt")) {
            return "Notes TXT";
        }
        return "";
    }
}
//end::clazz[]
