import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    api(mn.micronaut.json.core)
    implementation(mnSerde.micronaut.serde.api)
    compileOnly(mn.jackson.databind)
    annotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    api(libs.managed.mcp.java.sdk)
    api(projects.micronautMcp)
    api(projects.micronautMcpAnnotations)
    annotationProcessor(mnJsonSchema.micronaut.json.schema.processor)
    implementation(mnJsonSchema.micronaut.json.schema.annotations)
    implementation(mnJsonSchema.micronaut.json.schema.validation)
    api(mnJsonSchema.micronaut.json.schema.utils)
    api(mnValidation.validation)
    compileOnly(mn.micronaut.http.server)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(mnJsonSchema.micronaut.json.schema.processor)
    testImplementation(mnJsonSchema.micronaut.json.schema.annotations)
    testAnnotationProcessor(mnValidation.micronaut.validation.processor)
    testImplementation(mnValidation.micronaut.validation)
    testImplementation(mn.micronaut.http.client)
    testImplementation(libs.jsonassert)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(mnTest.junit.jupiter.params)
}
micronautBuild {
    testFramework = TestFramework.JUNIT5
}
tasks.withType<Test> {
    useJUnitPlatform()
}
