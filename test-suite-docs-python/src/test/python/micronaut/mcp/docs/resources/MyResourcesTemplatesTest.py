import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.http.client.exceptions import HttpClientResponseException
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_RESOURCES_TEMPLATES_LIST, RESOURCES_READ_ROUND_2, RESOURCES_READ_ROUND_99, RESOURCES_TEMPLATES_LIST
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="MyResourcesTemplatesTest")
@MicronautTest
class MyResourcesTemplatesTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def resources_templates_list(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_TEMPLATES_LIST)))
        assert json.loads(result) == json.loads(EXPECTED_RESOURCES_TEMPLATES_LIST)

    @Test
    def resources_read(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_2)))
        assert '"uri":"pgn://round/2"' in result, result
        assert '"mimeType":"application/x-chess-pgn"' in result, result
        assert '[Round \\"2\\"]' in result, result

    @Test
    def resources_read_not_found(self) -> None:
        try:
            self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_ROUND_99))
        except HttpClientResponseException as ex:
            body = str(ex.getResponse().body())
            assert "error" in body, body
            assert "resource for round not found" in body, body
        else:
            assert False, "an error response was expected"
