/*
 * Copyright (c) 2017 Nike, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.nike.cerberus.service;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.nike.cerberus.AssetResourceFileFactory;
import com.nike.cerberus.domain.AssetResourceFile;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Manager for loading and returning static asset file content.
 */
public class StaticAssetManager {
    private static final String JAR_URI_SCHEME = "jar";

    private final String resourceFolderPath;

    private final int maxDepth;

    private final ImmutableMap<String, AssetResourceFile> assetFileCache;

    public StaticAssetManager(String resourceFolderPath, int maxFileTraversalDepth) {
        this.resourceFolderPath = resourceFolderPath;
        this.maxDepth = maxFileTraversalDepth;
        assetFileCache = initCache();
    }

    public Optional<AssetResourceFile> get(String resourcePath) {
        return assetFileCache.containsKey(resourcePath) ?
                Optional.of(assetFileCache.get(resourcePath)) :
                Optional.empty();
    }

    /**
     * Load all files into memory that are located in the manager's resourceFolderPath
     * @return An immutable cache of files
     */
    private ImmutableMap<String, AssetResourceFile> initCache() {
        return ImmutableMap.<String, AssetResourceFile>builder()
                .putAll(loadFiles(resourceFolderPath))
                .build();
    }

    /**
     * Load all files into memory that are located in the given resource folder
     * @param resourceFolderPath  Path to the resource folder from which to load the files
     * @return A map of resource files
     */
    private Map<String, AssetResourceFile> loadFiles(String resourceFolderPath) {
        final Map<String, AssetResourceFile> assetFiles = Maps.newHashMap();
        Path resourceFolder = getPathToResourceFolder(resourceFolderPath);
        Stream<Path> resources = listResources(resourceFolder, maxDepth);
        resources.forEach(resourceFile -> {
            boolean isRootDirectory = resourceFolder.relativize(resourceFile).toString().isEmpty();
            if (!isRootDirectory) {
                AssetResourceFile assetResourceFile = AssetResourceFileFactory.create(
                        resourceFile.getFileName().toString(),
                        resourceFolder.getParent().relativize(resourceFile).toString(),
                        resourceFolderPath);
                assetFiles.put(assetResourceFile.getRelativePath(), assetResourceFile);
            }
        });
        return assetFiles;
    }

    /**
     * Ensure that the appropriate path to the resource folder is used (e.g. when running from within a JAR, or locally)
     * @param resourceFolderPath  Path to the resource folder from which to load the files
     * @return The path to the given resource folder
     */
    private Path getPathToResourceFolder(String resourceFolderPath) {
        try {
            URI resourceFolderUri = getClass().getResource(resourceFolderPath).toURI();
            Path folderPath;
            if (resourceFolderUri.getScheme().equals(JAR_URI_SCHEME)) {
                FileSystem fileSystem = FileSystems.newFileSystem(resourceFolderUri, Collections.emptyMap());
                folderPath = fileSystem.getPath(resourceFolderPath);
            } else {
                folderPath = Paths.get(resourceFolderUri);
            }
            return folderPath;
        } catch (URISyntaxException use) {
            throw new IllegalArgumentException("Could not get resource folder: " + resourceFolderPath, use);
        } catch (IOException ioe) {
            throw new IllegalArgumentException("Could not create file system resource folder: " + resourceFolderPath, ioe);
        }
    }

    /**
     * List the resource paths in the given folder
     * @param resourceFolderPath  Path to the resource folder from which to load the files
     * @param maxDepth            Max depth to recurse through the resource folder
     * @return A stream of paths
     */
    private Stream<Path> listResources(Path resourceFolderPath, int maxDepth) {
        try {
            return Files.walk(resourceFolderPath, maxDepth);
        } catch (IOException ioe) {
            throw new IllegalStateException("Could not walk file path: " + resourceFolderPath.toString());
        }
    }
}
