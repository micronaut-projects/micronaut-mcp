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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Property(name = "micronaut.mcp.server.info.name", value = "weather")
@Property(name = "micronaut.mcp.server.info.version", value = "1.0.0")
@Property(name = "micronaut.mcp.server.info.title", value = "Weather")
@Property(name = "micronaut.mcp.server.info.description", value = "Forecasts by city")
@Property(name = "micronaut.mcp.server.info.website-url", value = "https://example.com")
@Property(name = "micronaut.mcp.server.info.icons[0].src", value = "https://example.com/weather.png")
@Property(name = "micronaut.mcp.server.info.icons[0].mime-type", value = "")
@Property(name = "micronaut.mcp.server.info.icons[0].theme", value = "")
@Property(name = "micronaut.mcp.server.info.icons[1].src", value = "https://example.com/weather-dark.svg")
@Property(name = "micronaut.mcp.server.info.icons[1].mime-type", value = "image/svg+xml")
@Property(name = "micronaut.mcp.server.info.icons[1].sizes", value = "any")
@Property(name = "micronaut.mcp.server.info.icons[1].theme", value = "dark")
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
                             "icons": [{"src": "https://example.com/weather.png"}, {"src": "https://example.com/weather-dark.svg", "mimeType": "image/svg+xml", "sizes": ["any"], "theme": "dark"}], "websiteUrl": "https://example.com"},
              "instructions": "Ask for a city first."}}""", call("initialize", """
            {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "test", "version": "1"}}"""), JSONCompareMode.LENIENT);
    }

    @Test
    void emptyConfiguredIconValuesAreOmitted() throws JSONException {
        JSONObject icon = new JSONObject(call("initialize", """
            {"protocolVersion": "2025-06-18", "capabilities": {}, "clientInfo": {"name": "test", "version": "1"}}"""))
            .getJSONObject("result").getJSONObject("serverInfo").getJSONArray("icons").getJSONObject(0);
        assertFalse(icon.has("mimeType"));
        assertFalse(icon.has("theme"));
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
              "icons": [{"src": "https://example.com/station.png"}], "_meta": {"com.example/source": "noaa"}},
              {"uri": "weather://empty", "size": 0, "annotations": {"priority": 0}}]}}""",
            call("resources/list", "{}"), JSONCompareMode.LENIENT);
    }

    @Test
    void resourceTemplates() throws JSONException {
        JSONAssert.assertEquals("""
            {"result": {"resourceTemplates": [{"uriTemplate": "weather://cities/{city}",
              "annotations": {"audience": ["user", "assistant"]}, "_meta": {"com.example/source": "noaa"}},
              {"uriTemplate": "weather://alerts/{region}", "annotations": {"priority": 1}, "icons": [{"src": "https://example.com/alert.png"}]}]}}""",
            call("resources/templates/list", "{}"), JSONCompareMode.LENIENT);
    }

    @Test
    void undeclaredMetadataIsOmitted() throws JSONException {
        JSONArray templates = new JSONObject(call("resources/templates/list", "{}"))
            .getJSONObject("result").getJSONArray("resourceTemplates");
        JSONObject template = null;
        for (int i = 0; i < templates.length(); i++) {
            if (templates.getJSONObject(i).getString("uriTemplate").equals("weather://cities/{city}")) {
                template = templates.getJSONObject(i);
            }
        }
        assertNotNull(template);
        assertFalse(template.has("icons"));
        assertFalse(template.getJSONObject("annotations").has("priority"));
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

        @Resource(uri = "weather://empty", size = 0, priority = 0)
        String empty() {
            return "";
        }

        @ResourceTemplate(uriTemplate = "weather://alerts/{region}", priority = 1)
        @Icon(src = "https://example.com/alert.png")
        String alerts(String region) {
            return region;
        }

        @ResourceTemplate(uriTemplate = "weather://cities/{city}", audience = {Audience.USER, Audience.ASSISTANT})
        @Meta(key = "com.example/source", value = "noaa")
        String city(String city) {
            return city;
        }
    }
}
