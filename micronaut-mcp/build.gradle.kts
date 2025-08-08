plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    annotationProcessor(mnValidation.micronaut.validation.processor)
    api(mnValidation.micronaut.validation)
}
