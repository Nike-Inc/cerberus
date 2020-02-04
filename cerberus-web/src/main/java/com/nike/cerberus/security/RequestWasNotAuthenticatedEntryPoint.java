package com.nike.cerberus.security;

import static com.nike.cerberus.error.DefaultApiError.AUTH_TOKEN_INVALID;

import com.nike.backstopper.exception.ApiException;
import com.nike.backstopper.handler.spring.SpringApiExceptionHandler;
import java.io.IOException;
import java.util.Optional;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

public class RequestWasNotAuthenticatedEntryPoint implements AuthenticationEntryPoint {

  private final SpringApiExceptionHandler springApiExceptionHandler;

  @Autowired
  public RequestWasNotAuthenticatedEntryPoint(SpringApiExceptionHandler springApiExceptionHandler) {
    this.springApiExceptionHandler = springApiExceptionHandler;
  }

  @Override
  public void commence(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException)
      throws IOException, ServletException {
    returnBadCredentialsResponse(request, response, authException);
  }

  protected void returnBadCredentialsResponse(
      HttpServletRequest request,
      HttpServletResponse response,
      AuthenticationException authException) {
    SecurityContextHolder.clearContext();

    var nullableResponse =
        springApiExceptionHandler.resolveException(
            request, response, null, new ApiException(AUTH_TOKEN_INVALID));

    Optional.ofNullable(nullableResponse)
        .flatMap(res -> Optional.ofNullable(res.getView()))
        .ifPresent(
            view -> {
              try {
                view.render(nullableResponse.getModel(), request, response);
              } catch (Exception e) {
                throw new RuntimeException("Failed to render Backstopper error");
              }
            });
  }
}
