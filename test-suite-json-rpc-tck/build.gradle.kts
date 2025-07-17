plugins {
    id("groovy")
    id("java-library")
}
dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    implementation(mn.micronaut.json.core)
    implementation(mnTest.micronaut.test.junit5)
    implementation(projects.micronautJsonRpc)
}
