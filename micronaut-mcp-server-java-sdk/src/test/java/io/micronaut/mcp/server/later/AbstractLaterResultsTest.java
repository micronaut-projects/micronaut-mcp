package io.micronaut.mcp.server.later;

import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.BlockingHttpClient;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import jakarta.inject.Inject;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.skyscreamer.jsonassert.JSONAssert;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * A method that answers with a {@link java.util.concurrent.CompletionStage} or a publisher is answered for with
 * what that settles with, in a server that blocks and in one that is reactive.
 */
abstract class AbstractLaterResultsTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @ParameterizedTest
    @CsvSource({
        "futureText,+0.12 for e4",
        "stageText,+0.13 for e4",
        "monoText,+0.14 for e4",
        "futureResult,built for e4",
    })
    void aToolIsAnsweredWithWhatItsResultSettlesWith(String tool, String text) throws JSONException {
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":4,"result":{"content":[{"type":"text","text":"%s"}],"isError":false}}""".formatted(text), callTool(tool), true);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "fluxTexts|[\"e4\",\"d4\"]",
        "publisherTexts|[\"c4\",\"Nf3\"]",
    })
    void aPublisherOfSeveralIsAnsweredWithAllOfThem(String tool, String json) throws JSONException {
        JSONObject result = new JSONObject(callTool(tool)).getJSONObject("result");
        assertFalse(result.getBoolean("isError"));
        JSONAssert.assertEquals(json, result.getJSONArray("content").getJSONObject(0).getString("text"), true);
    }

    @Test
    void theSchemaOfWhatAFutureSettlesWithIsTheOutputSchemaOfTheTool() throws JSONException {
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":4,"result":{"isError":false,"structuredContent":{"evaluation":"+0.12","fen":"e4"}}}""", callTool("futureObject"), false);

        String listed = post("""
            {"jsonrpc":"2.0","id":3,"method":"tools/list","params":{}}""");
        org.json.JSONArray tools = new JSONObject(listed).getJSONObject("result").getJSONArray("tools");
        for (int i = 0; i < tools.length(); i++) {
            JSONObject tool = tools.getJSONObject(i);
            assertEquals(tool.getString("name").equals("futureObject"), tool.has("outputSchema"), tool.toString());
        }
    }

    @ParameterizedTest
    @CsvSource({
        "futureFails",
        "emptyMono",
        "futureOfNoResult",
    })
    void aToolThatFailsLaterOrCompletesWithNothingIsAnsweredWithAnError(String tool) throws JSONException {
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":4,"error":{"code":-32603}}""", failure(tool), false);
    }

    @ParameterizedTest
    @CsvSource({
        "future-prompt,Statistics for sergio",
        "mono-prompt,Games of sergio",
    })
    void aPromptIsWrittenFromWhatItsResultSettlesWith(String prompt, String text) throws JSONException {
        String answer = post("""
            {"jsonrpc":"2.0","id":0,"method":"prompts/get","params":{"name":"%s","arguments":{"name":"sergio"}}}""".formatted(prompt));
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":0,"result":{"messages":[{"role":"assistant","content":{"type":"text","text":"%s"}}]}}""".formatted(text), answer, false);
    }

    @ParameterizedTest
    @CsvSource({
        "later://hello,Hello later",
        "later://mono,Hello mono",
        "later://round/7,Round 7",
    })
    void aResourceIsReadFromWhatItsResultSettlesWith(String uri, String text) throws JSONException {
        String answer = post("""
            {"jsonrpc":"2.0","id":9,"method":"resources/read","params":{"uri":"%s"}}""".formatted(uri));
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":9,"result":{"contents":[{"uri":"%s","mimeType":"text/plain","text":"%s"}]}}""".formatted(uri, text), answer, true);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "future-prompt|[\"sergio\",\"serena\"]",
        "mono-prompt|[\"sera\",\"serb\"]",
    })
    void aCompletionIsWhatItsResultSettlesWith(String prompt, String values) throws JSONException {
        String answer = post("""
            {"jsonrpc":"2.0","id":1,"method":"completion/complete","params":{"ref":{"type":"ref/prompt","name":"%s"},"argument":{"name":"name","value":"ser"}}}""".formatted(prompt));
        JSONAssert.assertEquals("""
            {"jsonrpc":"2.0","id":1,"result":{"completion":{"values":%s,"total":2,"hasMore":false}}}""".formatted(values), answer, true);
    }

    private String callTool(String tool) {
        return post("""
            {"jsonrpc":"2.0","id":4,"method":"tools/call","params":{"name":"%s","arguments":{"fen":"e4"}}}""".formatted(tool));
    }

    // What a call that fails is answered with: the body, whatever the status
    String failure(String tool) {
        try {
            return callTool(tool);
        } catch (HttpClientResponseException e) {
            return e.getResponse().getBody(String.class).orElseThrow();
        }
    }

    private String post(String message) {
        BlockingHttpClient client = httpClient.toBlocking();
        return client.retrieve(HttpRequest.POST("/mcp", message));
    }
}
