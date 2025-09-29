plugins {
    `java-library`
}
dependencies {
    implementation(projects.micronautMcpServerJavaSdk)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(mnValidation.micronaut.validation.processor)
    implementation(mnValidation.micronaut.validation)
    annotationProcessor(mnJsonSchema.micronaut.json.schema.processor)
    implementation(mnJsonSchema.micronaut.json.schema.annotations)
    annotationProcessor(mnSerde.micronaut.serde.processor)
    implementation(mnSerde.micronaut.serde.jackson)
}
java {
    sourceCompatibility = JavaVersion.toVersion("17")
    targetCompatibility = JavaVersion.toVersion("17")
}
