package io.micronaut.mcp.server.stateless.sync.resources;

/*
//tag::fakepackage[]
package example.micronaut;

//end::fakepackage[]
*/
import io.micronaut.context.annotation.Requires;
//tag::imports[]
import io.micronaut.mcp.annotations.Resource;
import jakarta.inject.Singleton;
//end::imports[]

@Requires(property = "spec.name", value = "StatelessSyncResourcesAnnotationsTest")
//tag::clazz[]
@Singleton
class Resources {

    @Resource(
        uri = "example://hello",
        name = "hello",
        title = "Hello",
        description = "Hello text",
        mimeType = "text/plain"
    )
    String hello() {
        return "Hello World";
    }
}
//end::clazz[]
