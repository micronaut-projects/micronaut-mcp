/*
 * Copyright 2017-2025 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.mcp.server.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.micronaut.context.annotation.ClassImport;
import io.micronaut.context.annotation.Mixin;
import io.micronaut.core.annotation.Internal;
import io.micronaut.serde.annotation.Serdeable;
import io.modelcontextprotocol.spec.McpSchema.CompleteResult.CompleteCompletion;

@ClassImport(classes = {
    io.modelcontextprotocol.spec.McpSchema.JSONRPCRequest.class,
    io.modelcontextprotocol.spec.McpSchema.JSONRPCNotification.class,
    io.modelcontextprotocol.spec.McpSchema.JSONRPCResponse.class,
    io.modelcontextprotocol.spec.McpSchema.JSONRPCResponse.JSONRPCError.class,
    io.modelcontextprotocol.spec.McpSchema.InitializeRequest.class,
    io.modelcontextprotocol.spec.McpSchema.InitializeResult.class,
    io.modelcontextprotocol.spec.McpSchema.ClientCapabilities.class,
    io.modelcontextprotocol.spec.McpSchema.ClientCapabilities.RootCapabilities.class,
    io.modelcontextprotocol.spec.McpSchema.ClientCapabilities.Sampling.class,
    io.modelcontextprotocol.spec.McpSchema.ClientCapabilities.Elicitation.class,
    io.modelcontextprotocol.spec.McpSchema.ClientCapabilities.Elicitation.Form.class,
    io.modelcontextprotocol.spec.McpSchema.ClientCapabilities.Elicitation.Url.class,
    io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.class,
    io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.CompletionCapabilities.class,
    io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.LoggingCapabilities.class,
    io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.PromptCapabilities.class,
    io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.ResourceCapabilities.class,
    io.modelcontextprotocol.spec.McpSchema.ServerCapabilities.ToolCapabilities.class,
    io.modelcontextprotocol.spec.McpSchema.Implementation.class,
    io.modelcontextprotocol.spec.McpSchema.Role.class,
    io.modelcontextprotocol.spec.McpSchema.Annotations.class,
    io.modelcontextprotocol.spec.McpSchema.Resource.class,
    io.modelcontextprotocol.spec.McpSchema.ResourceTemplate.class,
    io.modelcontextprotocol.spec.McpSchema.ListResourcesResult.class,
    io.modelcontextprotocol.spec.McpSchema.ListResourceTemplatesResult.class,
    io.modelcontextprotocol.spec.McpSchema.ReadResourceRequest.class,
    io.modelcontextprotocol.spec.McpSchema.ReadResourceResult.class,
    io.modelcontextprotocol.spec.McpSchema.SubscribeRequest.class,
    io.modelcontextprotocol.spec.McpSchema.UnsubscribeRequest.class,
    io.modelcontextprotocol.spec.McpSchema.TextResourceContents.class,
    io.modelcontextprotocol.spec.McpSchema.BlobResourceContents.class,
    io.modelcontextprotocol.spec.McpSchema.Prompt.class,
    io.modelcontextprotocol.spec.McpSchema.PromptArgument.class,
    io.modelcontextprotocol.spec.McpSchema.PromptMessage.class,
    io.modelcontextprotocol.spec.McpSchema.ListPromptsResult.class,
    io.modelcontextprotocol.spec.McpSchema.GetPromptRequest.class,
    io.modelcontextprotocol.spec.McpSchema.GetPromptResult.class,
    io.modelcontextprotocol.spec.McpSchema.ListToolsResult.class,
    io.modelcontextprotocol.spec.McpSchema.JsonSchema.class,
    io.modelcontextprotocol.spec.McpSchema.ToolAnnotations.class,
    io.modelcontextprotocol.spec.McpSchema.Tool.class,
    io.modelcontextprotocol.spec.McpSchema.CallToolRequest.class,
    io.modelcontextprotocol.spec.McpSchema.CallToolResult.class,
    io.modelcontextprotocol.spec.McpSchema.ModelPreferences.class,
    io.modelcontextprotocol.spec.McpSchema.ModelHint.class,
    io.modelcontextprotocol.spec.McpSchema.SamplingMessage.class,
    io.modelcontextprotocol.spec.McpSchema.CreateMessageRequest.class,
    io.modelcontextprotocol.spec.McpSchema.CreateMessageResult.class,
    io.modelcontextprotocol.spec.McpSchema.ElicitRequest.class,
    io.modelcontextprotocol.spec.McpSchema.ElicitResult.class,
    io.modelcontextprotocol.spec.McpSchema.PaginatedRequest.class,
    io.modelcontextprotocol.spec.McpSchema.PaginatedResult.class,
    io.modelcontextprotocol.spec.McpSchema.ProgressNotification.class,
    io.modelcontextprotocol.spec.McpSchema.ResourcesUpdatedNotification.class,
    io.modelcontextprotocol.spec.McpSchema.LoggingMessageNotification.class,
    io.modelcontextprotocol.spec.McpSchema.SetLevelRequest.class,
    io.modelcontextprotocol.spec.McpSchema.PromptReference.class,
    io.modelcontextprotocol.spec.McpSchema.ResourceReference.class,
    io.modelcontextprotocol.spec.McpSchema.CompleteRequest.class,
    io.modelcontextprotocol.spec.McpSchema.CompleteRequest.CompleteArgument.class,
    io.modelcontextprotocol.spec.McpSchema.CompleteRequest.CompleteContext.class,
    io.modelcontextprotocol.spec.McpSchema.CompleteResult.class,
    CompleteCompletion.class,
    io.modelcontextprotocol.spec.McpSchema.Content.class,
    io.modelcontextprotocol.spec.McpSchema.TextContent.class,
    io.modelcontextprotocol.spec.McpSchema.ImageContent.class,
    io.modelcontextprotocol.spec.McpSchema.AudioContent.class,
    io.modelcontextprotocol.spec.McpSchema.EmbeddedResource.class,
    io.modelcontextprotocol.spec.McpSchema.ResourceLink.class,
    io.modelcontextprotocol.spec.McpSchema.Root.class,
    io.modelcontextprotocol.spec.McpSchema.ListRootsResult.class,
    io.modelcontextprotocol.spec.McpSchema.ResourceContents.class
}, annotate = Serdeable.class)
@Internal
class McpSchemaSerdeImport {
}

@JsonInclude(JsonInclude.Include.ALWAYS)
@Mixin(CompleteCompletion.class)
class CompleteCompletionMixin {

}
