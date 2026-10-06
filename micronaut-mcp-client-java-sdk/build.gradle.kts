import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    compileOnly(mn.micronaut.http.server)
    compileOnly(mn.micronaut.http.client.core)
    compileOnly(projects.micronautMcpServerJavaSdk)
    api(libs.managed.mcp.java.sdk)
    api(projects.micronautMcp)
    implementation(mn.micronaut.core.reactive)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mn.micronaut.http.client)
    testImplementation(projects.micronautMcpServerJavaSdk)
    testImplementation(projects.testSuiteMoon)
}
micronautBuild {
    testFramework = TestFramework.JUNIT6
}
tasks.withType<Test> {
    useJUnitPlatform()
}
