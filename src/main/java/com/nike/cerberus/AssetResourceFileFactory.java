package com.nike.cerberus;

import com.google.common.collect.ImmutableMap;
import com.nike.cerberus.domain.AssetResourceFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Factory for creating {@link AssetResourceFile} objects
 */
public class AssetResourceFileFactory {
    private static final Logger logger = LoggerFactory.getLogger(AssetResourceFileFactory.class);

    private static final String DEFAULT_MIME_TYPE = "text/plain";

    public static final ImmutableMap<String, String> FILE_EXT_TO_MIME_TYPE_MAP = ImmutableMap.<String, String>builder()
            .put("svg", "image/svg+xml")
            .put("js", "text/javascript")
            .put("ico", "image/x-icon")
            .put("css", "text/css")
            .put("html", "text/html")
            .put("json", "application/json")
            .build();

    public static AssetResourceFile create(String filename, String filePath, String rootFolderPath) {
        return new AssetResourceFile(
                filename,
                getRelativePath(filePath, rootFolderPath),
                getMimeTypeForFileFromName(filename),
                getFileContents(filePath));
    }

    /**
     * @return The contents of the given file in bytes
     */
    private static byte[] getFileContents(String filePath) {
        try {
            ClassLoader loader = AssetResourceFileFactory.class.getClassLoader();
            return IOUtils.toByteArray(loader.getResourceAsStream(filePath));
        } catch (NullPointerException | IOException ioe) {
            logger.info("Could not read contents of file: " + filePath, ioe);
        }
        return new byte[]{};
    }

    /**
     * @return The appropriate MIME type for the given file
     */
    private static String getMimeTypeForFileFromName(String fileName) {
        String fileExtension = StringUtils.substringAfterLast(fileName, ".");

        if (FILE_EXT_TO_MIME_TYPE_MAP.containsKey(fileExtension)) {
            return FILE_EXT_TO_MIME_TYPE_MAP.get(fileExtension);
        } else {
            logger.error("Could not determine MIME type for file: {}, using type '{}'", fileName, DEFAULT_MIME_TYPE);
            return DEFAULT_MIME_TYPE;
        }
    }

    /**
     * Returns the relative path of a file from a given root folder context
     * @param filePath  The full path for the file which to get the relative path for
     * @param rootFolderPath    The full path of the root folder, which to remove from the file path
     * @return  The relative path of the file (excluding the given root folder)
     */
    private static String getRelativePath(String filePath, String rootFolderPath) {
        String formattedRootFolderPath = StringUtils.stripStart(rootFolderPath, "/");
        String relativePath = StringUtils.substringAfterLast(filePath, formattedRootFolderPath);
        return StringUtils.stripStart(relativePath, "/");
    }
}
