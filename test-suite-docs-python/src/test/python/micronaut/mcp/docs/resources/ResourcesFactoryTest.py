import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_RESOURCES_LIST_PGN, EXPECTED_RESOURCES_TEMPLATES_LIST, RESOURCES_LIST, RESOURCES_READ_ROUND_2, RESOURCES_TEMPLATES_LIST
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from .PgnLoader import PgnLoader


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="pgn.rounda.path", value="classpath:fidewwc2024/round_1.pgn")
@Property(name="pgn.rounda.round", value="1")
@Property(name="pgn.roundb.path", value="classpath:fidewwc2024/round_2.pgn")
@Property(name="pgn.roundb.round", value="2")
@Property(name="pgn.roundc.path", value="classpath:fidewwc2024/round_3.pgn")
@Property(name="pgn.roundc.round", value="3")
@Property(name="spec.name", value="ResourcesFactoryTest")
@MicronautTest
class ResourcesFactoryTest:
    client: Annotated[HttpClient, Inject, Client("/")]
    pgn_loader: Annotated[PgnLoader, Inject]

    @Test
    def resources_list(self) -> None:
        result = json.loads(str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_LIST))))
        expected = json.loads(EXPECTED_RESOURCES_LIST_PGN)
        result["result"]["resources"].sort(key=lambda resource: resource["uri"])
        expected["result"]["resources"].sort(key=lambda resource: resource["uri"])
        assert result == expected

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
        assert self.pgn_loader.load_pgn(2).startswith('[Event "FIDE World Championship Match 2024"]')
