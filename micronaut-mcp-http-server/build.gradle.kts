import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    api(mn.micronaut.http)
    api(projects.micronautJsonRpc)
    annotationProcessor(mnValidation.micronaut.validation.processor)
    api(mnValidation.micronaut.validation)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(mn.micronaut.jackson.databind)
    testImplementation(mn.micronaut.http.client)
    testImplementation(mn.micronaut.http.server.netty)
    testRuntimeOnly(mnLogging.logback.classic)
}
micronautBuild {
    testFramework = TestFramework.JUNIT5
}
tasks.withType<Test> {
    useJUnitPlatform()
}
