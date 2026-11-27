plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    api(libs.managed.mcp.java.sdk)
    api(mn.micronaut.json.core)
    api(mnJsonSchema.micronaut.json.schema.validation)
    implementation(mnSerde.micronaut.serde.api)
    annotationProcessor(mnSerde.micronaut.serde.processor)
}
micronautBuild {
    binaryCompatibility.enabledAfter("2.2.0")
}
