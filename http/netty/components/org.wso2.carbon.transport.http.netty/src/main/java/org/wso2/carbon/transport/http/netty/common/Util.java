/*
 * Copyright (c) 2015, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.wso2.carbon.transport.http.netty.common;

import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpVersion;
import org.wso2.carbon.messaging.CarbonMessage;
import org.wso2.carbon.messaging.Header;
import org.wso2.carbon.messaging.Headers;
import org.wso2.carbon.transport.http.netty.common.ssl.SSLConfig;
import org.wso2.carbon.transport.http.netty.config.Parameter;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Includes utility methods for creating http requests and responses and their related properties.
 */
public class Util {

    private static final String DEFAULT_HTTP_METHOD_POST = "POST";
    private static final String DEFAULT_VERSION_HTTP_1_1 = "HTTP/1.1";

    public static Headers getHeaders(HttpMessage message) {
        List<Header> headers = new LinkedList<>();
        if (message.headers() != null) {
            for (Map.Entry<String, String> k : message.headers().entries()) {
                headers.add(new Header(k.getKey(), k.getValue()));
            }
        }
        return new Headers(headers);
    }

    public static void setHeaders(HttpMessage message, Headers headers) {
        HttpHeaders httpHeaders = message.headers();
        for (Header header : headers.getAll()) {
            httpHeaders.add(header.getName(), header.getValue());
        }
    }

    public static String getStringValue(CarbonMessage msg, String key, String defaultValue) {
        String value = (String) msg.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    public static int getIntValue(CarbonMessage msg, String key, int defaultValue) {
        Integer value = (Integer) msg.getProperty(key);
        if (value == null) {
            return defaultValue;
        }

        return value;
    }

    @SuppressWarnings("unchecked")
    public static HttpResponse createHttpResponse(CarbonMessage msg) {
        HttpVersion httpVersion = new HttpVersion(Util.getStringValue(msg, Constants.HTTP_VERSION, HTTP_1_1.text()),
                true);

        int statusCode = Util.getIntValue(msg, Constants.HTTP_STATUS_CODE, 200);

        HttpResponseStatus httpResponseStatus = new HttpResponseStatus(statusCode,
                HttpResponseStatus.valueOf(statusCode).reasonPhrase());

        DefaultHttpResponse outgoingResponse = new DefaultHttpResponse(httpVersion, httpResponseStatus, false);

        Headers headers = msg.getHeaders();

        Util.setHeaders(outgoingResponse, headers);

        return outgoingResponse;
    }

    @SuppressWarnings("unchecked")
    public static HttpRequest createHttpRequest(CarbonMessage msg) {
        HttpMethod httpMethod;
        if (null != msg.getProperty(Constants.HTTP_METHOD)) {
            httpMethod = new HttpMethod((String) msg.getProperty(Constants.HTTP_METHOD));
        } else {
            httpMethod = new HttpMethod(DEFAULT_HTTP_METHOD_POST);
        }
        HttpVersion httpVersion;
        if (null != msg.getProperty(Constants.HTTP_VERSION)) {
            httpVersion = new HttpVersion((String) msg.getProperty(Constants.HTTP_VERSION), true);
        } else {
            httpVersion = new HttpVersion(DEFAULT_VERSION_HTTP_1_1, true);
        }
        if ((String) msg.getProperty(Constants.TO) == null) {
            msg.setProperty(Constants.TO, "/");
        }
        HttpRequest outgoingRequest = new DefaultHttpRequest(httpVersion, httpMethod,
                (String) msg.getProperty(Constants.TO), false);
        Headers headers = msg.getHeaders();
        Util.setHeaders(outgoingRequest, headers);
        return outgoingRequest;
    }

    public static SSLConfig getSSLConfigForListener(String certPass, String keyStorePass, String keyStoreFile,
            String trustStoreFile, String trustStorePass, List<Parameter> parametersList) {
        if (certPass == null) {
            certPass = keyStorePass;
        }
        if (keyStoreFile == null || keyStorePass == null) {
            throw new IllegalArgumentException("keyStoreFile or keyStorePass not defined for " + "HTTPS scheme");
        }
        File keyStore = new File(keyStoreFile);
        if (!keyStore.exists()) {
            throw new IllegalArgumentException("KeyStore File " + keyStoreFile + " not found");
        }
        SSLConfig sslConfig = new SSLConfig(keyStore, keyStorePass).setCertPass(certPass);
        for (Parameter parameter : parametersList) {
            if (parameter.getName()
                    .equals(Constants.SERVER_SUPPORT_CIPHERS)) {
                sslConfig.setCipherSuites(parameter.getValue());

            } else if (parameter.getName()
                    .equals(Constants.SERVER_SUPPORT_HTTPS_PROTOCOLS)) {
                sslConfig.setEnableProtocols(parameter.getValue());
            } else if (parameter.getName()
                    .equals(Constants.SERVER_SUPPORTED_SNIMATCHERS)) {
                sslConfig.setSniMatchers(parameter.getValue());
            } else if (parameter.getName()
                    .equals(Constants.SERVER_SUPPORTED_SERVER_NAMES)) {
                sslConfig.setServerNames(parameter.getValue());
            } else if (parameter.getName()
                    .equals(Constants.SERVER_ENABLE_SESSION_CREATION)) {
                sslConfig.setEnableSessionCreation(Boolean.parseBoolean(parameter.getValue()));
            } else if (parameter.getName()
                    .equals(Constants.SSL_VERIFY_CLIENT)) {
                sslConfig.setNeedClientAuth(Boolean.parseBoolean(parameter.getValue()));
            }
        }
        if (trustStoreFile != null) {
            File trustStore = new File(trustStoreFile);
            if (!trustStore.exists()) {
                throw new IllegalArgumentException("trustStore File " + trustStoreFile + " not found");
            }
            if (trustStorePass == null) {
                throw new IllegalArgumentException("trustStorePass is not defined for HTTPS scheme");
            }
            sslConfig.setTrustStore(trustStore).setTrustStorePass(trustStorePass);
        }
        return sslConfig;
    }

    public static SSLConfig getSSLConfigForSender(String certPass, String keyStorePass, String keyStoreFile,
            String trustStoreFile, String trustStorePass, List<Parameter> parametersList) {

        if (certPass == null) {
            certPass = keyStorePass;
        }
        if (trustStoreFile == null || trustStorePass == null) {
            throw new IllegalArgumentException("TrusstoreFile or trustStorePass not defined for " + "HTTPS scheme");
        }
        SSLConfig sslConfig = new SSLConfig(null, null).setCertPass(null);
        if (keyStoreFile != null) {
            File keyStore = new File(keyStoreFile);
            if (!keyStore.exists()) {
                throw new IllegalArgumentException("KeyStore File " + trustStoreFile + " not found");
            }
            sslConfig = new SSLConfig(keyStore, keyStorePass).setCertPass(certPass);
        }
        File trustStore = new File(trustStoreFile);

        sslConfig.setTrustStore(trustStore).setTrustStorePass(trustStorePass);
        sslConfig.setClientMode(true);
        for (Parameter parameter : parametersList) {
            if (parameter.getName()
                    .equals(Constants.CLIENT_SUPPORT_CIPHERS)) {
                sslConfig.setCipherSuites(parameter.getValue());

            } else if (parameter.getName()
                    .equals(Constants.CLIENT_SUPPORT_HTTPS_PROTOCOLS)) {
                sslConfig.setEnableProtocols(parameter.getValue());
            } else if (parameter.getName()
                    .equals(Constants.CLIENT_ENABLE_SESSION_CREATION)) {
                sslConfig.setEnableSessionCreation(Boolean.parseBoolean(parameter.getValue()));
            }
        }
        return sslConfig;
    }
}
