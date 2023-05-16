package com.nike.cerberus.error;

import com.nike.backstopper.apierror.ApiError;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;

public class InvalidEncryptionContextApiError implements ApiError {

  private final String message;

  public InvalidEncryptionContextApiError(String message) {
    this.message = message;
  }

  @Override
  public String getName() {
    return "InvalidEncryptionContextException";
  }

  @Override
  public String getErrorCode() {
    return DefaultApiError.ENTITY_NOT_FOUND.getErrorCode();
  }

  @Override
  public String getMessage() {
    return this.message;
  }

  @Override
  public Map<String, Object> getMetadata() {
    return null;
  }

  @Override
  public int getHttpStatusCode() {
    return HttpServletResponse.SC_NOT_FOUND;
  }
}
