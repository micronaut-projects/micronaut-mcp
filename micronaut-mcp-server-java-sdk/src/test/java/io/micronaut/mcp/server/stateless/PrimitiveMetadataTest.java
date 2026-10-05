package io.micronaut.mcp.server.stateless;

import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.mcp.annotations.Audience;
import io.micronaut.mcp.annotations.Icon;
import io.micronaut.mcp.annotations.Meta;
import io.micronaut.mcp.annotations.Prompt;
import io.micronaut.mcp.annotations.Resource;
import io.micronaut.mcp.annotations.ResourceTemplate;
import io.micronaut.mcp.annotations.Tool;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

@Property(name = "micronaut.mcp.server.info.name", value = "weather")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.info.title", value = "Weather")
@Property(name = "micronaut.mcp.server.info.description", value = "Forecasts by city")
@Property(name = "micronaut.mcp.server.info.website-url", value = "https://example.com")
@Property(name = "micronaut.mcp.server.info.icons[0]", value = "https://example.com/weather.png")
@Property(name = "micronaut.mcp.server.info.instructions", value = "Ask for a city first.")
@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "spec.name", value = "PrimitiveMetadataTest")
@MicronautTest
class PrimitiveMetadataTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Test
    void serverInfo() throws JSONException {
        JSONAssert.assertEquals("""
            {"result": {
              "serverInfo": {"name": "weather", "title": "Weather", "version": "1.0.0", "description": "Forecasts by city",
                             "icons": [{"src": "https://example.com/weather.png"}], "websiteUrl": "https://example.com"},
              "instructions": "Ask for a city first."}}""", call("initialize", """
            {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "test", "version": "1"}}"""), JSONCompareMode.LENIENT);
    }

    @Test
    void tools() throws JSONException {
        JSONAssert.assertEquals("""
            {"result": {"tools": [{"name": "forecast",
              "icons": [{"src": "https://example.com/sun.svg", "mimeType": "image/svg+xml", "sizes": ["any"], "theme": "light"}],
              "_meta": {"com.example/category": "weather"}}]}}""", call("tools/list", "{}"), JSONCompareMode.LENIENT);
    }

    @Test
    void prompts() throws JSONException {
        JSONAssert.assertEquals("""
            {"result": {"prompts": [{"name": "plan", "icons": [{"src": "https://example.com/plan.png"}], "_meta": {"com.example/kind": "trip"}}]}}""",
            call("prompts/list", "{}"), JSONCompareMode.LENIENT);
    }

    @Test
    void resources() throws JSONException {
        JSONAssert.assertEquals("""
            {"result": {"resources": [{"uri": "weather://stations", "size": 42,
              "annotations": {"audience": ["assistant"], "priority": 0.5},
              "icons": [{"src": "https://example.com/station.png"}], "_meta": {"com.example/source": "noaa"}}]}}""",
            call("resources/list", "{}"), JSONCompareMode.LENIENT);
    }

    @Test
    void resourceTemplates() throws JSONException {
        JSONAssert.assertEquals("""
            {"result": {"resourceTemplates": [{"uriTemplate": "weather://cities/{city}",
              "annotations": {"audience": ["user", "assistant"]}, "_meta": {"com.example/source": "noaa"}}]}}""",
            call("resources/templates/list", "{}"), JSONCompareMode.LENIENT);
    }

    private String call(String method, String params) {
        return httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", """
            {"jsonrpc": "2.0", "id": 1, "method": "%s", "params": %s}""".formatted(method, params)));
    }

    @Requires(property = "spec.name", value = "PrimitiveMetadataTest")
    @Singleton
    static class Primitives {
        @Tool
        @Icon(src = "https://example.com/sun.svg", mimeType = "image/svg+xml", sizes = "any", theme = "light")
        @Meta(key = "com.example/category", value = "weather")
        String forecast(String city) {
            return "sunny";
        }

        @Prompt
        @Icon(src = "https://example.com/plan.png")
        @Meta(key = "com.example/kind", value = "trip")
        String plan() {
            return "Plan a trip";
        }

        @Resource(uri = "weather://stations", size = 42, audience = Audience.ASSISTANT, priority = 0.5)
        @Icon(src = "https://example.com/station.png")
        @Meta(key = "com.example/source", value = "noaa")
        String stations() {
            return "KSEA";
        }

        @ResourceTemplate(uriTemplate = "weather://cities/{city}", audience = {Audience.USER, Audience.ASSISTANT})
        @Meta(key = "com.example/source", value = "noaa")
        String city(String city) {
            return city;
        }
    }
}
