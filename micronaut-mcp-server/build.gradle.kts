import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    api(mn.micronaut.jackson.databind)
    api(libs.managed.mcp.java.sdk)
    api(projects.micronautMcp)
    testAnnotationProcessor(mn.micronaut.inject.java)
}
micronautBuild {
    testFramework = TestFramework.JUNIT5
}
tasks.withType<Test> {
    useJUnitPlatform()
}
