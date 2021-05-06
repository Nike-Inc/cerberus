package com.nike.cerberus.security;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;

import com.nike.backstopper.handler.spring.SpringApiExceptionHandler;
import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.servlet.ModelAndView;

public class RequestWasNotAuthenticatedEntryPointTest {

  @InjectMocks private RequestWasNotAuthenticatedEntryPoint requestWasNotAuthenticatedEntryPoint;
  @Mock private AuthenticationException authenticationException;
  @Mock private SpringApiExceptionHandler springApiExceptionHandler;
  private HttpServletRequest request;
  private HttpServletResponse response;
  private ModelAndView modelAndView;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    request = mock(HttpServletRequest.class);
    response = mock(HttpServletResponse.class);
    modelAndView = mock(ModelAndView.class);
  }

  @Test
  public void testCommence() throws IOException, ServletException {
    Mockito.when(
            springApiExceptionHandler.resolveException(
                anyObject(), anyObject(), anyObject(), anyObject()))
        .thenReturn(modelAndView);
    requestWasNotAuthenticatedEntryPoint.commence(request, response, authenticationException);
  }
}
