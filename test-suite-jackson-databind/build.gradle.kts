plugins {
    id("io.micronaut.build.internal.mcp-native-tests")
}
dependencies {
    implementation(mn.micronaut.jackson.databind)
    implementation(projects.micronautMcpServerJavaSdk)
    annotationProcessor(mnValidation.micronaut.validation.processor)
    implementation(mnValidation.micronaut.validation)
    annotationProcessor(mnJsonSchema.micronaut.json.schema.processor)
    implementation(mnJsonSchema.micronaut.json.schema.annotations)
    implementation(mn.micronaut.http.client)
    runtimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.jsonassert)
}
