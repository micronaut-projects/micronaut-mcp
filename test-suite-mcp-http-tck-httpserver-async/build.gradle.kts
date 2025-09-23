plugins {
    `java-library`
}
dependencies {
    testImplementation(projects.testSuiteMcpHttpTck)
    testImplementation(projects.micronautMcpServerJavaSdk)
    testImplementation(libs.mcp.json.jackson2)
    testImplementation(mnTest.junit.platform.suite.api)
    // Add JUnit Jupiter API and engines
    testImplementation(mnTest.junit.jupiter.api)
    testRuntimeOnly(mnTest.junit.jupiter.engine)
    // Add JUnit Platform Suite engine to run @Suite tests
    testRuntimeOnly(libs.junit.platform.engine)
    testImplementation("org.junit.platform:junit-platform-launcher")
}
tasks.withType<Test> {
    useJUnitPlatform()
}
