import io.micronaut.build.TestFramework
plugins {
    id("io.micronaut.build.internal.mcp-module")
}
dependencies {
    api(mn.micronaut.jackson.databind)
    api(libs.managed.mcp.java.sdk)
    compileOnly(mn.micronaut.http.server)
    testAnnotationProcessor(mn.micronaut.inject.java)
    testAnnotationProcessor(mnValidation.micronaut.validation.processor)
    testImplementation(mnValidation.micronaut.validation)
    testImplementation(mn.micronaut.http.client)
    testImplementation(libs.jsonassert)
    testImplementation(mn.micronaut.http.server.netty)
}
micronautBuild {
    testFramework = TestFramework.JUNIT5
}
tasks.withType<Test> {
    useJUnitPlatform()
}
