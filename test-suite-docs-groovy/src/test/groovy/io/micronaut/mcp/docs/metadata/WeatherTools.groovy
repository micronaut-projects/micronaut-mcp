package io.micronaut.mcp.docs.metadata

import io.micronaut.context.annotation.Requires
//tag::imports[]
import io.micronaut.mcp.annotations.Icon
import io.micronaut.mcp.annotations.Meta
import io.micronaut.mcp.annotations.Tool
import jakarta.inject.Singleton
//end::imports[]

@Requires(property = "spec.name", value = "MetadataSpec")
//tag::clazz[]
@Singleton
class WeatherTools {
    @Tool
    @Icon(src = "https://example.com/sun.svg", mimeType = "image/svg+xml", sizes = "any")
    @Meta(key = "com.example/category", value = "weather")
    String forecast(String city) {
        return "sunny"
    }
}
//end::clazz[]
