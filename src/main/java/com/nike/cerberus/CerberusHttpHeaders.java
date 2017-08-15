package com.nike.cerberus;

import com.nike.riposte.server.http.RequestInfo;
import io.netty.handler.codec.http.HttpHeaders;
import org.apache.commons.lang3.StringUtils;

public final class CerberusHttpHeaders {

    public static final String HEADER_X_CERBERUS_CLIENT = "X-Cerberus-Client";
    public static final String HEADER_X_REFRESH_TOKEN = "X-Refresh-Token";
    public static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String UNKNOWN = "Unknown";

    /**
     * Get the value of the X-Cerberus-Client header or "Unknown" if not found.
     */
    public static String getClientVersion(RequestInfo request) {
        final HttpHeaders headers = request.getHeaders();
        if (headers != null) {
            String value = headers.get(HEADER_X_CERBERUS_CLIENT);
            if (value != null) {
                return value;
            }
        }
        return UNKNOWN;
    }

    /**
     * Get the first IP address from the Http header "X-Forwarded-For"
     *
     * E.g. "X-Forwarded-For: ip1, ip2, ip3" would return "ip1"
     */
    public static String getXForwardedClientIp(RequestInfo request) {
        final HttpHeaders headers = request.getHeaders();
        if (headers != null) {
            String value = headers.get(HEADER_X_FORWARDED_FOR);
            if (value != null) {
                if(value.contains(",")) {
                    return StringUtils.substringBefore(value, ",").trim();
                }
                else {
                    return value.trim();
                }
            }
        }
        return null;
    }
}
