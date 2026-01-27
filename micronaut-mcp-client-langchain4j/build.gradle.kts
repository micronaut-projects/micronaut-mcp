import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    api(projects.micronautMcp)
    implementation(platform("dev.langchain4j:langchain4j-bom:${mnLangchain4j.versions.langchain4j.asProvider().get()}"))
    api(libs.langchain4j.mcp)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testImplementation(projects.micronautMcpServerJavaSdk)
    testAnnotationProcessor(mnSerde.micronaut.serde.processor)
    testImplementation(mnSerde.micronaut.serde.jackson)
    testImplementation(mn.micronaut.http.server.netty)
    testImplementation(projects.testSuiteMoon)
}
micronautBuild {
    testFramework = TestFramework.JUNIT6
}
tasks.withType<Test> {
    useJUnitPlatform()
}


