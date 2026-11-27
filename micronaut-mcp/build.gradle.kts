import io.micronaut.build.TestFramework

plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    annotationProcessor(mnSerde.micronaut.serde.processor)
    api(mnSerde.micronaut.serde.api)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mnLogging.logback.classic)
}
micronautBuild {
    testFramework = TestFramework.JUNIT6
}
tasks.withType<Test> {
    useJUnitPlatform()
}
