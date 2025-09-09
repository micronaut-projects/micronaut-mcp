import org.gradle.kotlin.dsl.testImplementation
import org.gradle.kotlin.dsl.testRuntimeOnly

plugins {
    id("io.micronaut.application") version "4.5.4"
}
version = "0.1"
group = "example.micronaut"
repositories {
    mavenCentral()
}
dependencies {
    implementation(mn.micronaut.jackson.databind)
    implementation(projects.micronautMcpServerJavaSdk)
    annotationProcessor(mnJsonSchema.micronaut.json.schema.processor)
    implementation(mnJsonSchema.micronaut.json.schema.annotations)
    implementation(mn.micronaut.http.client)
    runtimeOnly(mnLogging.logback.classic)
    testImplementation(libs.jsonassert)
}
application {
    mainClass = "example.micronaut.Application"
}
micronaut {
    version(libs.versions.micronaut.platform.get())
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("example.micronaut.*")
    }
}
