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
package com.axway.ats.action.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.axway.ats.action.BaseTest;
import org.powermock.core.classloader.annotations.PowerMockIgnore;

@PowerMockIgnore({"java.net.ssl", "javax.security.*", "com.sun.*", "javax.management.*", "org.apache.http.conn.ssl.*"})
public class Test_RestClient extends BaseTest {

    private String[]            supportedProtocols;
    private String              username;
    private String              password;
    private String              uri;

    private Map<String, String> propMap;
    private List<Class<?>>      providerClasses;
    private List<Object>        providers;
    private String              certFileName;
    private String              certPassword;

    @Before
    public void before() {

        supportedProtocols = new String[5];
        supportedProtocols[0] = "prt_1";
        supportedProtocols[1] = "prt_2";

        username = "username";
        password = "password";
        uri = "uri";

        providerClasses = new ArrayList<Class<?>>();
        providerClasses.add(Test_RestClient.class);

        providers = new ArrayList<Object>();
        providers.add("object");

        propMap = new HashMap<String, String>();
        propMap.put("propName", "propValue");

        certFileName = "certFileName";
        certPassword = "certPassword";
    }

    /**
     * Verify the copied instance of RESTClient, copy also all original instance properties 
     */
    @Test
    public void testCopy() {

        // create RESTClientConfigurator instance and set any custom properties to it
        RestClientConfigurator clientConfigurator = new RestClientConfigurator();
        clientConfigurator.setSSLCertificate(certFileName, certPassword);
        clientConfigurator.setProperty("propName", propMap.get("propName"));
        clientConfigurator.registerProviderClass(Test_RestClient.class);
        clientConfigurator.registerProvider("object");

        // create RESTClient instance and set any custom properties to it
        RestClient client = new RestClient("http://www.test.com");
        client.setSupportedProtocols(supportedProtocols);
        client.setURI(uri);
        client.setBasicAuthorization(username, password);
        client.setClientConfigurator(clientConfigurator);

        // create a copy of the original instance
        RestClient clientCopy = client.newCopy();

        // verify all properties are copied to the copied instance
        assertEquals(Arrays.toString(supportedProtocols),
                     Arrays.toString(clientCopy.getSupportedProtocols()));
        assertEquals(uri, clientCopy.getURI());

        // verify that clientConfigurator properties are also copied to the copied instance
        assertEquals(certFileName, clientCopy.getClientConfigurator().getCertificateFileName());
        assertEquals(certPassword, clientCopy.getClientConfigurator().getCertificateFilePassword());
        assertTrue(clientCopy.getClientConfigurator()
                             .getProperties()
                             .entrySet()
                             .containsAll(propMap.entrySet()));
        assertTrue(clientCopy.getClientConfigurator().getProviderClasses().containsAll(providerClasses));
        assertTrue(clientCopy.getClientConfigurator().getProviders().containsAll(providers));
    }
}
