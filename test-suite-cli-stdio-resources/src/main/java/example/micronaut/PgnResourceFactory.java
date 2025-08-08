package example.micronaut;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.exceptions.ConfigurationException;
import jakarta.inject.Inject;
import io.micronaut.mcp.resources.Resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.List;

@Context
class PgnResourceFactory {
    public static final String PGN_MIME_TYPE = "application/x-chess-pgn";
    private static final String PGN_EXTENSION = ".pgn";

    PgnResourceFactory(BeanContext beanContext, PgnResourcesConfiguration configuration) {
        try {
            for (Resource resource : buildResourcesRecursive(configuration.getFolder())) {
                beanContext.registerSingleton(resource);
            }
        } catch (IOException e) {
            throw new ConfigurationException("Error reading PGN files from folder: " + configuration.getFolder(), e);
        }
    }


    /**
     * Recursive version using Files.walk() for all subdirectories.
     *
     * @param folderPath The path to the folder containing PGN files
     * @return List of Resource instances for each PGN file found
     * @throws IOException if there's an error reading the directory
     */
    public static List<Resource> buildResourcesRecursive(String folderPath) throws IOException {
        Path folder = Paths.get(folderPath);

        if (!Files.exists(folder)) {
            throw new ConfigurationException("Folder does not exist: " + folderPath);
        }

        if (!Files.isDirectory(folder)) {
            throw new ConfigurationException("Path is not a directory: " + folderPath);
        }

        try (Stream<Path> paths = Files.walk(folder)) {
            return paths
                .filter(Files::isRegularFile)
                .filter(PgnResourceFactory::isPgnFile)
                .map(PgnResourceFactory::createResourceFromFile)
                .collect(Collectors.toList());
        }
    }

    /**
     * Creates a Resource instance from a PGN file.
     *
     * @param file The path to the PGN file
     * @return Resource instance representing the file
     */
    private static Resource createResourceFromFile(Path file) {
        try {
            String fileName = file.getFileName().toString();
            String uri = file.toUri().toString();
            Long fileSize = Files.size(file);

            // Remove extension for a cleaner title
            String title = fileName.substring(0, fileName.lastIndexOf('.'));

            // Build the Resource with all available information
            return new Resource(
                uri,                                    // uri
                "PGN chess game file: " + fileName,     // name
                title,                                  // title (human-readable)
                "PGN chess game file: " + fileName,    // description
                PGN_MIME_TYPE,                         // mimeType
                fileSize                               // size in bytes
            );
        } catch (IOException e) {
            // If we can't get file size, create resource without it
            String fileName = file.getFileName().toString();
            String uri = file.toUri().toString();
            String title = fileName.substring(0, fileName.lastIndexOf('.'));

            return new Resource(
                uri,
                fileName,
                title,
                "PGN chess game file: " + fileName,
                PGN_MIME_TYPE,
                null  // size unknown
            );
        }
    }

    /**
     * Checks if a file is a PGN file based on its extension.
     *
     * @param file The path to check
     * @return true if the file has a .pgn extension (case-insensitive)
     */
    private static boolean isPgnFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(PGN_EXTENSION);
    }

}
