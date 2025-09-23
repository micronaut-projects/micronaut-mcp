plugins {
    `java-library`
}
dependencies {
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mn.micronaut.http.server.netty)
    testAnnotationProcessor(mnJsonSchema.micronaut.json.schema.processor)
    testImplementation(mnJsonSchema.micronaut.json.schema.annotations)

    testAnnotationProcessor(mn.micronaut.inject.java)
    testRuntimeOnly(mnLogging.logback.classic)

    testImplementation(projects.testSuiteMcpHttpTck)
    testImplementation(projects.micronautMcpServerJavaSdk)
    testImplementation(mnTest.junit.platform.suite.api)
    // Add JUnit Jupiter API and engines
    testImplementation(mnTest.junit.jupiter.api)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    // Add JUnit Platform Suite engine to run @Suite tests
    testRuntimeOnly(libs.junit.platform.engine)
    testImplementation("org.junit.platform:junit-platform-launcher")
}
tasks.withType<Test> {
    useJUnitPlatform()
}
