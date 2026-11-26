plugins {
    id("io.micronaut.build.internal.mcp-test-java")
}
dependencies {
    testImplementation(projects.micronautMcpServerJavaSdk)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mn.micronaut.http.server.netty)
    // the development runtime, and the reload harness
    testImplementation(mn.micronaut.dev)
    testImplementation(mn.micronaut.dev.tck)
    // the reload harness compiles the application under test with the processors on the test classpath
    testImplementation(mn.micronaut.inject.java)
}
