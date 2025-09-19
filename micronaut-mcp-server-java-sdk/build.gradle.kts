import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    api(mn.micronaut.json.core)
    compileOnly(mn.jackson.databind)
    testImplementation(mn.micronaut.jackson.databind)

    api(libs.managed.mcp.java.sdk)
    api(projects.micronautMcp)
    api(projects.micronautMcpAnnotations)
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
