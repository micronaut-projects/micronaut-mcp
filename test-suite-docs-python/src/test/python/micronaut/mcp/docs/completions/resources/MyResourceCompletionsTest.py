import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_RESOURCES_READ_REPORT, EXPECTED_RESOURCE_COMPLETION, RESOURCES_READ_REPORT, RESOURCE_COMPLETION_REQUEST
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="MyResourceCompletionsTest")
@MicronautTest
class MyResourceCompletionsTest:
    client: Annotated[HttpClient, Inject, Client("/")]

    @Test
    def resource_completion(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCE_COMPLETION_REQUEST)))
        assert json.loads(result) == json.loads(EXPECTED_RESOURCE_COMPLETION)

    @Test
    def resource_template_read(self) -> None:
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", RESOURCES_READ_REPORT)))
        assert json.loads(result) == json.loads(EXPECTED_RESOURCES_READ_REPORT)
