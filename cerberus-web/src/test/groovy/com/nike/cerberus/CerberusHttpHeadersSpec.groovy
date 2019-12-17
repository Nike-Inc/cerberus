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
