import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    api(mn.micronaut.jackson.databind)
    api(libs.managed.mcp.java.sdk)
    testAnnotationProcessor(mn.micronaut.inject.java)
}
micronautBuild {
    testFramework = TestFramework.JUNIT5
}
tasks.withType<Test> {
    useJUnitPlatform()
}
