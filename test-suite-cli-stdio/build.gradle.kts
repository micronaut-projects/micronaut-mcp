plugins {
    id("io.micronaut.application") version "4.5.4"
    id("com.gradleup.shadow") version "8.3.8"
}

version = "0.1"
group = "io.micronaut.mcp"

repositories {
    mavenCentral()
}
dependencies {
    annotationProcessor("info.picocli:picocli-codegen")
    implementation("info.picocli:picocli")
    implementation("io.micronaut.picocli:micronaut-picocli")
    implementation(projects.micronautMcpServer)
    testImplementation(projects.micronautMcpClientLangchain4j)
    testAnnotationProcessor(mnLangchain4j.micronaut.langchain4j.processor)
    testImplementation(mnLangchain4j.micronaut.langchain4j.openai)
    testImplementation(mnLangchain4j.micronaut.langchain4j.core)
    runtimeOnly(mnLogging.logback.classic)
}


application {
    mainClass = "example.micronaut.McpServerCommand"
}
java {
    sourceCompatibility = JavaVersion.toVersion("21")
    targetCompatibility = JavaVersion.toVersion("21")
}

micronaut {
    version = libs.versions.micronaut.platform.get()
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("example.micronaut")
    }
}



