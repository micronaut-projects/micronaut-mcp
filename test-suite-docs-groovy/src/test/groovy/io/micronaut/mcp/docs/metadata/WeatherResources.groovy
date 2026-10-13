package io.micronaut.mcp.docs.metadata

import io.micronaut.context.annotation.Requires
//tag::imports[]
import io.micronaut.mcp.annotations.Audience
import io.micronaut.mcp.annotations.Resource
import jakarta.inject.Singleton
//end::imports[]

@Requires(property = "spec.name", value = "MetadataSpec")
//tag::clazz[]
@Singleton
class WeatherResources {
    @Resource(uri = "weather://stations", size = 4, audience = Audience.ASSISTANT, priority = 0.5d)
    String stations() {
        return "KSEA"
    }
}
//end::clazz[]
