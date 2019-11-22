/*
 * Copyright (c) 2017 Nike Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nike.cerberus.api

import com.fieldju.commons.PropUtils
import org.apache.http.conn.ssl.SSLSocketFactory
import org.apache.http.conn.ssl.X509HostnameVerifier
import org.apache.http.params.HttpParams

import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLParameters
import javax.net.ssl.SSLSocket

/**
 * https://sookocheff.com/post/api/rest-assured-tests-against-api-gateway/
 */
class GatewaySslSocketFactory extends SSLSocketFactory {

    GatewaySslSocketFactory(SSLContext sslContext, X509HostnameVerifier hostnameVerifier) {
        super(sslContext, hostnameVerifier)
    }

    @Override
    Socket createSocket(HttpParams params) throws IOException {
        SSLSocket sslSocket = (SSLSocket) super.createSocket(params)

        // Set the encryption protocol
        String[] protocols = ["TLSv1.2"].toArray()
        sslSocket.setEnabledProtocols(protocols)

        // Configure SNI
        URL url = new URL(PropUtils.getRequiredProperty("CERBERUS_API_URL", "The Cerberus API URL to Test"))
        SNIHostName serverName = new SNIHostName(url.getHost())
        SSLParameters sslParams = sslSocket.getSSLParameters()
        sslParams.setServerNames(Collections.singletonList(serverName))
        sslSocket.setSSLParameters(sslParams)

        return sslSocket
    }
}
