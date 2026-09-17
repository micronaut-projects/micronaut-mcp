package io.micronaut.mcp.docs.completions.resources;

//tag::imports[]
import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.ResourceTemplate;
import jakarta.inject.Singleton;

//end::imports[]
@Requires(property = "spec.name", value = "MyResourceCompletionsTest")
//tag::clazz[]
@Singleton
class MyResourceTemplates {
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
