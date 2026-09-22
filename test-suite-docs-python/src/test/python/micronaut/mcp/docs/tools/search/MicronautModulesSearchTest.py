import json
from typing import Annotated

from jakarta.inject import Inject
from micronaut.context.annotation import Property
from micronaut.http import HttpRequest
from micronaut.http.client import HttpClient
from micronaut.http.client.annotation import Client
from micronaut.mcp.server.tools.search import SearchTool
from micronaut.mcp.docs.JsonRpcMessages import EXPECTED_SEARCH_TOOL_CALL, SEARCH_TOOL_CALL, TOOLS_LIST
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test


@Property(name="micronaut.mcp.server.transport", value="HTTP")
@Property(name="spec.name", value="MicronautModulesSearchTest")
@MicronautTest
class MicronautModulesSearchTest:
    client: Annotated[HttpClient, Inject, Client("/")]
    tool: Annotated[SearchTool, Inject]

    @Test
    def search_tool(self) -> None:
        assert self.tool.getName() == "search"
        assert self.tool.getTitle() == "Search"
        tools = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", TOOLS_LIST)))
        assert '"name":"search"' in tools, tools
        assert '"title":"Search"' in tools, tools
        assert '"description":"Returns a list of relevant search results, given a user\'s query."' in tools, tools
        result = str(self.client.toBlocking().retrieve(HttpRequest.POST("/mcp", SEARCH_TOOL_CALL)))
        assert json.loads(result) == json.loads(EXPECTED_SEARCH_TOOL_CALL)
