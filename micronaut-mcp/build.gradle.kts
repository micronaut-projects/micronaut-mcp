import io.micronaut.build.TestFramework

plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    annotationProcessor(mnSerde.micronaut.serde.processor)
    api(mnSerde.micronaut.serde.api)
    compileOnly(mn.micronaut.http)
    compileOnly(mn.micronaut.http.client.core)
    compileOnly(mn.micronaut.context)
    compileOnly(mn.reactor)
    testImplementation(mn.micronaut.http)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testRuntimeOnly(mnLogging.logback.classic)
}
micronautBuild {
    testFramework = TestFramework.JUNIT6
}
tasks.withType<Test> {
    useJUnitPlatform()
}
