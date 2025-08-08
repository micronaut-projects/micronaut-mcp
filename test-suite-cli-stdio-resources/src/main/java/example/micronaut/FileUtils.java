package example.micronaut;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileUtils {
    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);
    private FileUtils() {
    }

    public static String text(String uri) {
        try {
            URI fileUri = new URI(uri);
            Path filePath = Paths.get(fileUri);
            return Files.readString(filePath, StandardCharsets.UTF_8);

        } catch (URISyntaxException e) {
            LOG.error("Invalid URI format: {}", uri, e);
            // You might want to throw an exception or return an error result
            throw new IllegalArgumentException("Invalid URI format: " + uri, e);
        } catch (IOException e) {
            LOG.error("Failed to read file from URI: {}", uri, e);
            // You might want to throw an exception or return an error result
            throw new RuntimeException("Failed to read file: " + uri, e);
        } catch (IllegalArgumentException e) {
            // This can happen if the URI scheme is not 'file'
            LOG.error("URI is not a valid file path: {}", uri, e);
            throw new IllegalArgumentException("URI is not a valid file path: " + uri, e);
        }
    }
}
