plugins {
    id("io.micronaut.build.internal.mcp-test-java")
}
dependencies {
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mn.micronaut.http.server.netty)

    testAnnotationProcessor(mnJsonSchema.micronaut.json.schema.processor)
    testImplementation(mnJsonSchema.micronaut.json.schema.annotations)

    testImplementation(projects.testSuiteMcpHttpTckCommon)
    testImplementation(projects.testSuiteMcpHttpTck)
    testImplementation(mnTest.junit.platform.suite.api)
    // Add JUnit Platform Suite engine to run @Suite tests
    testRuntimeOnly(libs.junit.platform.engine)
}
