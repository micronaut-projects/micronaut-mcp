package example.micronaut;

import io.micronaut.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public final class ResourceLoaderUtils {
    private ResourceLoaderUtils() {
    }

    public static Optional<String> readResource(ResourceLoader resourceLoader, String resource) throws IOException {
        Optional<InputStream> inputStreamOptional = resourceLoader.getResourceAsStream(resource);
        if (inputStreamOptional.isPresent()) {
            try (InputStream inputStream = inputStreamOptional.get()) {
                return Optional.of(new String(inputStream.readAllBytes(), StandardCharsets.UTF_8));
            }
        }
        return Optional.empty();
    }
}
