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

package com.nike.cerberus.controller.authentication;

import static org.springframework.web.bind.annotation.RequestMethod.DELETE;

import com.nike.cerberus.security.CerberusPrincipal;
import com.nike.cerberus.service.AuthenticationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/auth")
public class RevokeAuthenticationController {
  private final AuthenticationService authenticationService;

  @Autowired
  public RevokeAuthenticationController(AuthenticationService authenticationService) {
    this.authenticationService = authenticationService;
  }

  @RequestMapping(method = DELETE)
  public void revokeAuthentication(Authentication authentication) {
    var cerberusPrincipal = (CerberusPrincipal) authentication;
    authenticationService.revoke(cerberusPrincipal.getToken());
  }
}
