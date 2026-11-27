import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    compileOnly(mn.micronaut.http.server)
    api(projects.micronautMcpJavaSdkJson)
    api(projects.micronautMcp)
    runtimeOnly(mnSerde.micronaut.serde.jackson)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(projects.micronautMcpServerJavaSdk)
    testImplementation(projects.testSuiteMoon)
}
micronautBuild {
    testFramework = TestFramework.JUNIT6
}
tasks.withType<Test> {
    useJUnitPlatform()
}
