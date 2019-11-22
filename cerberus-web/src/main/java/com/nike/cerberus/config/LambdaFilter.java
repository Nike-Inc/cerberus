package com.nike.cerberus.config;

import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.function.BiConsumer;

public class LambdaFilter extends OncePerRequestFilter {

  private final BiConsumer<HttpServletRequest, HttpServletResponse> function;
  private final boolean shouldDoChainBefore;

  public LambdaFilter(boolean shouldDoChainBefore, BiConsumer<HttpServletRequest, HttpServletResponse> function) {
    this.function = function;
    this.shouldDoChainBefore = shouldDoChainBefore;
  }

  public LambdaFilter(BiConsumer<HttpServletRequest, HttpServletResponse> function) {
    this.function = function;
    shouldDoChainBefore = false;
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    if (shouldDoChainBefore) {
      doChainBefore(request, response, filterChain);
    } else {
      doChainAfter(request, response, filterChain);
    }
  }

  private void doChainBefore(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    filterChain.doFilter(request, response);
    function.accept(request, response);
  }

  private void doChainAfter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
    function.accept(request, response);
    filterChain.doFilter(request, response);
  }
}
