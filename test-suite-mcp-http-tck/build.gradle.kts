import io.micronaut.build.TestFramework

plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    api(projects.micronautMcpServerJavaSdk)
    api(mnTest.junit.jupiter.api)
    implementation(libs.jsonassert)
}
micronautBuild {
    testFramework = TestFramework.JUNIT6
}
tasks.withType<Test> {
    useJUnitPlatform()
}
