package com.nike.cerberus.util;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Bytes;
import com.nike.cerberus.domain.DashboardResourceFile;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

/**
 * Cerberus Dashboard file objects including images, HTML, etc.
 */
public class DashboardResourceFileFactory {

    private static final Logger logger = LoggerFactory.getLogger(DashboardResourceFileFactory.class);

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
     * Creates a DashboardResourceFile object
     * @param file               File object of the dashboard resource to be created
     * @param rootDirectoryPath  Absolute path of the containing directory for the file
     * @return the DashboardResourceFile
     */
    public static DashboardResourceFile create(File file, String rootDirectoryPath) {
        return new DashboardResourceFile(
                file.getName(),
                getRelativePath(file.getAbsolutePath(), rootDirectoryPath),
                getMimeTypeForFileFromName(file.getName()),
                ImmutableList.<Byte>builder()
                        .addAll(getFileContents(file))
                        .build()
        );
    }

    /**
     * @return The contents of the given file in bytes
     */
    private static List<Byte> getFileContents(File file) {
        try {
            byte[] contents = Files.readAllBytes(file.toPath());
            return Bytes.asList(contents);
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Could not read contents of file: " + file.getName(), ioe);
        }
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
     * @param fullFilePath  The full path for the file which to get the relative path for
     * @param rootFolder    The full path of the root folder, which to remove from the file path
     * @return  The relative path of the file (excluding the given root folder)
     */
    private static String getRelativePath(String fullFilePath, String rootFolder) {
        return StringUtils.substringAfterLast(fullFilePath, rootFolder + "/");
    }
}
