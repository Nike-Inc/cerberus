package com.nike.cerberus.domain;

import java.io.File;

/**
 * Cerberus Dashboard file object (e.g. image file, HTML file, JSON, etc.)
 */
public class DashboardResourceFile {

    private final File file;

    private final String mimeType;

    private final byte[] fileContents;

    public DashboardResourceFile(File file, String mimeType, byte[] fileContents) {
        this.file = file;
        this.mimeType = mimeType;
        this.fileContents = fileContents;
    }

    public String getFileName() {
        return file.getName();
    }

    public String getMimeType() {
        return mimeType;
    }

    public byte[] getFileContents() {
        return fileContents;
    }
}
