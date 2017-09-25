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
