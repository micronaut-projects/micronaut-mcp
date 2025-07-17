plugins {
    id("io.micronaut.build.internal.mcp-tck-suite")
}
dependencies {
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
}

