import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    implementation(platform("dev.langchain4j:langchain4j-bom:${mnLangchain4j.versions.langchain4j.asProvider().get()}"))
    api(libs.langchain4j.mcp)
    testAnnotationProcessor(mn.micronaut.inject.java)
}
micronautBuild {
    testFramework = TestFramework.JUNIT5
}
tasks.withType<Test> {
    useJUnitPlatform()
}


