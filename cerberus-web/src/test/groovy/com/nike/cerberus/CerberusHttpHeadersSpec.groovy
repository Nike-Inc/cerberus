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

package com.nike.cerberus

import spock.lang.Specification

import javax.servlet.http.HttpServletRequest

import static com.nike.cerberus.CerberusHttpHeaders.*

class CerberusHttpHeadersSpec extends Specification {

  void "test that getClientVersion can parse the client"() {
    given:
    def fakeVersion = 'fake/1.2.3'
    HttpServletRequest request = Mock()
    request.getHeader(HEADER_X_CERBERUS_CLIENT) >> fakeVersion

    when:
    def actual = getClientVersion(request)

    then:
    actual == fakeVersion
  }

  void "test that getClientVersion returns unknown when the header value is null"() {
    given:
    HttpServletRequest request = Mock()
    request.getHeader(HEADER_X_CERBERUS_CLIENT) >> null

    when:
    def actual = getClientVersion(request)

    then:
    actual == UNKNOWN
  }

  void "test that when 3 ip addresses are passed to getXForwardedClientIp the first is returned"() {
    given:
    HttpServletRequest request = Mock()
    request.getHeader(HEADER_X_FORWARDED_FOR) >> "ip1, ip2, ip3"

    when:
    def actual = getXForwardedClientIp(request)

    then:
    actual == "ip1"
  }

  void "test that when the x forwarded for header is missing getXForwardedClientIp returns unknown"() {
    given:
    HttpServletRequest request = Mock()
    request.getHeader(HEADER_X_FORWARDED_FOR) >> null

    when:
    def actual = getXForwardedClientIp(request)

    then:
    actual == UNKNOWN
  }
}
