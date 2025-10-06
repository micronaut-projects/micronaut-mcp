package io.micronaut.mcp.server.serialization;

import io.micronaut.json.JsonMapper;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@MicronautTest(startApplication = false)
class ReadResourceResultSerializationTest {

    @Test
    void serialize(JsonMapper jsonMapper) {
        //language=JSON
        String json = """
            {
              "contents": [
                {
                  "uri": "guidemetadata://micronaut-oauth2-auth0",
                  "mimeType": "application/json",
                  "text": "{\\"title\\":\\"Secure a Micronaut application with Auth0\\",\\"intro\\":\\"Learn how to create a Micronaut application and secure it with an Authorization Server provided by Auth0.\\",\\"authors\\":[\\"Sergio del Amo\\"],\\"tags\\":[\\"security-jwt\\",\\"security\\",\\"authorization-code\\",\\"auth0\\",\\"graalvm\\",\\"thymeleaf\\",\\"oauth2\\",\\"oidc\\",\\"security-oauth2\\",\\"yaml\\"],\\"category\\":\\"Authorization Code\\",\\"publicationDate\\":\\"2021-09-03\\",\\"slug\\":\\"micronaut-oauth2-auth0\\",\\"url\\":\\"https://guides.micronaut.io/latest/micronaut-oauth2-auth0.html\\",\\"options\\":[{\\"buildTool\\":\\"GRADLE\\",\\"language\\":\\"JAVA\\",\\"url\\":\\"https://guides.micronaut.io/latest/micronaut-oauth2-auth0-gradle-java.html\\"},{\\"buildTool\\":\\"GRADLE\\",\\"language\\":\\"GROOVY\\",\\"url\\":\\"https://guides.micronaut.io/latest/micronaut-oauth2-auth0-gradle-groovy.html\\"},{\\"buildTool\\":\\"GRADLE\\",\\"language\\":\\"KOTLIN\\",\\"url\\":\\"https://guides.micronaut.io/latest/micronaut-oauth2-auth0-gradle-kotlin.html\\"},{\\"buildTool\\":\\"MAVEN\\",\\"language\\":\\"JAVA\\",\\"url\\":\\"https://guides.micronaut.io/latest/micronaut-oauth2-auth0-maven-java.html\\"},{\\"buildTool\\":\\"MAVEN\\",\\"language\\":\\"GROOVY\\",\\"url\\":\\"https://guides.micronaut.io/latest/micronaut-oauth2-auth0-maven-groovy.html\\"},{\\"buildTool\\":\\"MAVEN\\",\\"language\\":\\"KOTLIN\\",\\"url\\":\\"https://guides.micronaut.io/latest/micronaut-oauth2-auth0-maven-kotlin.html\\"}]}"
                }
              ]
            }""";
        assertDoesNotThrow(() -> jsonMapper.readValue(json, McpSchema.ReadResourceResult.class));
    }
}
