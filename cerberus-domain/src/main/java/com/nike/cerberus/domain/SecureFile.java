package com.nike.cerberus.domain;

public interface SecureFile {
  byte[] getData();
  String getName();
  int getSizeInBytes();
}
