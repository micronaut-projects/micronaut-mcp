package io.micronaut.mcp.server.stateless.sync.resources;

import io.micronaut.context.annotation.Requires;
import io.micronaut.mcp.annotations.Resource;
import io.modelcontextprotocol.spec.McpSchema;
import jakarta.inject.Singleton;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Requires(property = "spec.name", value = "StatelessSyncResourcesBinaryAnnotationsTest")
@Singleton
class ResourcesBinary {

    @Resource(
        uri = "example://zip",
        name = "zip",
        title = "Zip archive",
        description = "A ZIP containing hello.txt",
        mimeType = "application/zip"
    )
    McpSchema.ReadResourceResult zipResource() {
        byte[] zipBytes = createTestZip();
        String blob = Base64.getEncoder().encodeToString(zipBytes);
        McpSchema.BlobResourceContents contents =
            new McpSchema.BlobResourceContents("example://zip", "application/zip", blob);
        return new McpSchema.ReadResourceResult(List.of(contents));
    }

    private static byte[] createTestZip() {
        try {
            byte[] data = "Hello Zip".getBytes(StandardCharsets.UTF_8);

            CRC32 crc = new CRC32();
            crc.update(data);

            ZipEntry entry = new ZipEntry("hello.txt");
            entry.setMethod(ZipEntry.STORED);
            entry.setSize(data.length);
            entry.setCompressedSize(data.length);
            entry.setCrc(crc.getValue());
            entry.setTime(0L); // stabilize timestamp for deterministic output

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                zos.putNextEntry(entry);
                zos.write(data);
                zos.closeEntry();
                zos.finish();
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to build deterministic ZIP for test", e);
        }
    }
}
