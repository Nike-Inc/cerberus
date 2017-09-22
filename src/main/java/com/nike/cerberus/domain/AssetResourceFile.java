package com.nike.cerberus.domain;

import com.google.common.collect.ImmutableList;
import com.google.common.primitives.Bytes;

/**
 * Resource file object (e.g. image file, HTML file, JSON, etc.)
 */
public class AssetResourceFile {

    private String fileName;

    private String relativePath;

    private String mimeType;

    private ImmutableList<Byte> fileContents;

    public AssetResourceFile(String fileName,
                             String relativePath,
                             String mimeType,
                             byte[] fileContents) {
        this.fileName = fileName;
        this.relativePath = relativePath;
        this.mimeType = mimeType;
        this.fileContents = ImmutableList.<Byte>builder().addAll(Bytes.asList(fileContents)).build();
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

    public byte[] getFileContents() {
        return Bytes.toArray(fileContents);
    }
}
