package com.nike.cerberus;

import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

public final class CerberusHttpHeaders {

    public static final String HEADER_X_CERBERUS_CLIENT = "X-Cerberus-Client";
    public static final String HEADER_X_REFRESH_TOKEN = "X-Refresh-Token";
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String UNKNOWN = "_unknown";

    /**
     * Get the value of the X-Cerberus-Client header or "Unknown" if not found.
     */
    public static String getClientVersion(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HEADER_X_CERBERUS_CLIENT)).orElse(UNKNOWN);
    }

    /**
     * Get the first IP address from the Http header "X-Forwarded-For"
     *
     * E.g. "X-Forwarded-For: ip1, ip2, ip3" would return "ip1"
     */
    public static String getXForwardedClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HEADER_X_FORWARDED_FOR))
          .map(value -> StringUtils.substringBefore(value, ",").trim())
          .orElse(UNKNOWN);
    }

    /**
     * Get the complete Http header "X-Forwarded-For"
     */
    public static String getXForwardedCompleteHeader(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader(HEADER_X_FORWARDED_FOR)).orElse(UNKNOWN);
    }
}
