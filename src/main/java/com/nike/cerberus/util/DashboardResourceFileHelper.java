package com.nike.cerberus.util;

import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * Cerberus Dashboard file objects including images, HTML, etc.
 */
public class DashboardResourceFileHelper {

    private static final Logger logger = LoggerFactory.getLogger(DashboardResourceFileHelper.class);

    private static final String DEFAULT_MIME_TYPE = "text/plain";

    public static final ImmutableMap<String, String> FILE_EXT_TO_MIME_TYPE_MAP = ImmutableMap.<String, String>builder()
            .put("svg", "image/svg+xml")
            .put("js", "text/javascript")
            .put("ico", "image/x-icon")
            .put("css", "text/css")
            .put("html", "text/html")
            .put("json", "application/json")
            .build();

    /**
     * @return The contents of the given file in bytes
     */
    public static byte[] getFileContents(File file) {
        try {
            return Files.readAllBytes(file.toPath());
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Could not read contents of file: " + file.getName(), ioe);
        }
    }

    /**
     * @return The appropriate MIME type for the given file
     */
    public static String getMimeTypeForFileFromName(String fileName) {
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
     * @param fullFilePath   - The full path for the file which to get the relative path for
     * @param rootFolder - The full path of the root folder, which to remove from the file path
     * @return - The relative path of the file (excluding the given root folder)
     */
    public static String getRelativePath(String fullFilePath, String rootFolder) {
        return StringUtils.substringAfterLast(fullFilePath, rootFolder + "/");
    }
}
