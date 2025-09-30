plugins {
    id("io.micronaut.build.internal.mcp-native-tests")
}
dependencies {
    annotationProcessor(mnSerde.micronaut.serde.processor)
    implementation(mnSerde.micronaut.serde.jackson)
    implementation(projects.micronautMcpServerJavaSdk)
    testImplementation(projects.micronautMcpClientJavaSdk)
    testImplementation(projects.testSuiteMoon)
    annotationProcessor(mnValidation.micronaut.validation.processor)
    implementation(mnValidation.micronaut.validation)
    annotationProcessor(mnJsonSchema.micronaut.json.schema.processor)
    implementation(mnJsonSchema.micronaut.json.schema.annotations)
    implementation(mn.micronaut.http.client)
    runtimeOnly(mnLogging.logback.classic)
    testImplementation(libs.jsonassert)
}
