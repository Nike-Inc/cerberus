package com.nike.cerberus.domain;

import com.google.common.collect.ImmutableList;

/**
 * Cerberus Dashboard file object (e.g. image file, HTML file, JSON, etc.)
 */
public class DashboardResourceFile {

    private String fileName;

    private String relativePath;

    private String mimeType;

    private ImmutableList<Byte> fileContents;

    public DashboardResourceFile(String fileName,
                                 String relativePath,
                                 String mimeType,
                                 ImmutableList<Byte> fileContents) {
        this.fileName = fileName;
        this.relativePath = relativePath;
        this.mimeType = mimeType;
        this.fileContents = fileContents;
    }

    public String getFileName() {
        return fileName;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getMimeType() {
        return mimeType;
    }

    public ImmutableList<Byte> getFileContents() {
        return fileContents;
    }
}
