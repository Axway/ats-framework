/*
 * Copyright 2017 Axway Software
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
package com.axway.ats.action.mail.model;

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * A dummy Trust Manager which does not perform validation of certificates if they come from trusted CA, 
 * it just trusts everything
 */
public class BasicTrustManager implements X509TrustManager {

    public void checkClientTrusted(
                                    X509Certificate[] cert,
                                    String authType ) {

    // no exception is thrown, so everything is trusted
    }

    public void checkServerTrusted(
                                    X509Certificate[] cert,
                                    String authType ) {

    // no exception is thrown, so everything is trusted

    }

    public X509Certificate[] getAcceptedIssuers() {

        return new X509Certificate[0];
    }
}
