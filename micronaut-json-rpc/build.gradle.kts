import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    annotationProcessor(mnSerde.micronaut.serde.processor)
    annotationProcessor(mnValidation.micronaut.validation.processor)
    api(mnValidation.micronaut.validation)

    implementation(mnSerde.micronaut.serde.api)
    implementation("com.fasterxml.jackson.core:jackson-annotations")
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testRuntimeOnly(mnLogging.logback.classic)
}
micronautBuild {
    testFramework = TestFramework.JUNIT5
}
tasks.withType<Test> {
    useJUnitPlatform()
}
