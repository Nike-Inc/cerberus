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

package com.nike.cerberus.controller;

import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Slf4j
@Controller
public class DashboardController {
  @GetMapping(value = {"/", "/dashboard", "/dashboard/"})
  public String root(HttpServletRequest request) {
    String uri = request.getRequestURI();
    log.info("*** Redirecting to index.html from " + uri + " ***");
    return "redirect:/dashboard/index.html";
  }

  @GetMapping(value = {"/dashboard/callback"})
  public String callback(HttpServletRequest request) {
    String uri = request.getRequestURI();
    log.info("*** Forwarding to index.html from " + uri + " ***");
    return "forward:/dashboard/index.html";
  }
}
