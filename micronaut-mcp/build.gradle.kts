import io.micronaut.build.TestFramework

plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    testAnnotationProcessor(mn.micronaut.inject.java)
    testRuntimeOnly(mnLogging.logback.classic)
}
micronautBuild {
    testFramework = TestFramework.JUNIT5
}
tasks.withType<Test> {
    useJUnitPlatform()
}
