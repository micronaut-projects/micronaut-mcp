plugins {
    id("io.micronaut.build.internal.mcp-test-java")
}
dependencies {
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(projects.micronautMcpServerJavaSdk)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnSecurity.micronaut.security.jwt)
    testImplementation(mnSecurity.micronaut.security.oauth2)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mn.micronaut.http.client)
    testImplementation(libs.jsonassert)
}
dependencies {
    testImplementation(mnTest.micronaut.test.junit5)
}
