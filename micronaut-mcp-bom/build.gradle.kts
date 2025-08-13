plugins {
    id("io.micronaut.build.internal.mcp-base")
    id("io.micronaut.build.internal.bom")
}
micronautBuild {
    binaryCompatibility.enabledAfter("1.0.0")
}
