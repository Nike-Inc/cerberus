/*
 * Copyright (c) 2020 Nike, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.config;

import java.io.IOException;
import java.util.function.BiConsumer;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

public class LambdaFilter extends OncePerRequestFilter {

  private final BiConsumer<HttpServletRequest, HttpServletResponse> function;
  private final boolean shouldDoChainBefore;

  public LambdaFilter(
      boolean shouldDoChainBefore, BiConsumer<HttpServletRequest, HttpServletResponse> function) {
    this.function = function;
    this.shouldDoChainBefore = shouldDoChainBefore;
  }

  public LambdaFilter(BiConsumer<HttpServletRequest, HttpServletResponse> function) {
    this.function = function;
    shouldDoChainBefore = false;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    if (shouldDoChainBefore) {
      doChainBefore(request, response, filterChain);
    } else {
      doChainAfter(request, response, filterChain);
    }
  }

  private void doChainBefore(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    filterChain.doFilter(request, response);
    function.accept(request, response);
  }

  private void doChainAfter(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    function.accept(request, response);
    filterChain.doFilter(request, response);
  }
}
