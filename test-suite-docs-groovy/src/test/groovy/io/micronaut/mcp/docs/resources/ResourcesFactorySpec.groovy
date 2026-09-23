package io.micronaut.mcp.docs.resources

import io.micronaut.context.annotation.Property
import io.micronaut.http.HttpRequest
import io.micronaut.http.client.HttpClient
import io.micronaut.http.client.annotation.Client
import io.micronaut.test.extensions.spock.annotation.MicronautTest
import jakarta.inject.Inject
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode
import spock.lang.Specification

import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_LIST_PGN
import static io.micronaut.mcp.docs.JsonRpcMessages.EXPECTED_RESOURCES_TEMPLATES_LIST
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_LIST
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_READ_ROUND_2
import static io.micronaut.mcp.docs.JsonRpcMessages.RESOURCES_TEMPLATES_LIST

@Property(name = "micronaut.mcp.server.transport", value = "HTTP")
@Property(name = "pgn.rounda.path", value = "classpath:fidewwc2024/round_1.pgn")
@Property(name = "pgn.rounda.round", value = "1")
@Property(name = "pgn.roundb.path", value = "classpath:fidewwc2024/round_2.pgn")
@Property(name = "pgn.roundb.round", value = "2")
@Property(name = "pgn.roundc.path", value = "classpath:fidewwc2024/round_3.pgn")
@Property(name = "pgn.roundc.round", value = "3")
@Property(name = "spec.name", value = "ResourcesFactorySpec")
@MicronautTest
class ResourcesFactorySpec extends Specification {

    @Inject
    @Client("/")
    HttpClient httpClient

    @Inject
    PgnLoader pgnLoader

    void "resources list"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_LIST))

        then:
        JSONAssert.assertEquals(EXPECTED_RESOURCES_LIST_PGN, result, JSONCompareMode.NON_EXTENSIBLE)
    }

    void "resources templates list"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_TEMPLATES_LIST))

        then:
        JSONAssert.assertEquals(EXPECTED_RESOURCES_TEMPLATES_LIST, result, true)
    }

    void "resources read"() {
        when:
        String result = httpClient.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_2))

        then:
        result.contains('"uri":"pgn://round/2"')
        result.contains('"mimeType":"application/x-chess-pgn"')
        result.contains('[Round \\"2\\"]')
        pgnLoader.loadPgn(2).orElseThrow().startsWith('[Event "FIDE World Championship Match 2024"]')
    }
}
