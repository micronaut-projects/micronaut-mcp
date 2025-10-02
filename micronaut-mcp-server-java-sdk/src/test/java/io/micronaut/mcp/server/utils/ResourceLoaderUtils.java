package io.micronaut.mcp.server.utils;

import io.micronaut.core.io.ResourceLoader;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

public final class ResourceLoaderUtils {

    public static Optional<Long> size(ResourceLoader resourceLoader, String path) throws IOException {
        Optional<InputStream> inputStreamOptional = resourceLoader.getResourceAsStream(path);
        if (inputStreamOptional.isPresent()) {
            try (InputStream inputStream = inputStreamOptional.get()) {
                return Optional.of((long) inputStream.readAllBytes().length);
            }
        }
        return Optional.empty();
    }

}
