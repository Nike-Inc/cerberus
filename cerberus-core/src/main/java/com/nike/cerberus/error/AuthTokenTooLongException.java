package com.nike.cerberus.error;

public class AuthTokenTooLongException extends Exception {

  private AuthTokenTooLongException() {
    super();
  }

  public AuthTokenTooLongException(String message) {
    super(message);
  }
}
