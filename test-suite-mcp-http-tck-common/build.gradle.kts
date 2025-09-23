plugins {
    `java-library`
}
dependencies {
    api(projects.micronautMcpServerJavaSdk)
    annotationProcessor(mnSerde.micronaut.serde.processor)
    implementation(mnSerde.micronaut.serde.jackson)
    implementation(mn.micronaut.http.server.netty)
    annotationProcessor(mnJsonSchema.micronaut.json.schema.processor)
    implementation(mnJsonSchema.micronaut.json.schema.annotations)
    annotationProcessor(mn.micronaut.inject.java)
}
