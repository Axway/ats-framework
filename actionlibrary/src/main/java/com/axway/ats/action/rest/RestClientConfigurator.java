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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.glassfish.jersey.client.spi.ConnectorProvider;

import com.axway.ats.common.PublicAtsApi;

/**
 * A class used to configure the REST client
 */
@PublicAtsApi
public class RestClientConfigurator {

    private List<Object>        providers;
    private List<Class<?>>      providerClasses;
    private Map<String, Object> properties;

    private String              certFileName;
    private String              certPassword;

    /**
     * Provide third-party {@link ConnectorProvider}, like ApacheConnector, etc
     * */
    private ConnectorProvider   connectorProvider;
    /**
     * Additional properties for configuring the  {@link ConnectorProvider} </br>
     * If there is no connector provider, those properties will not be applied.
     * */
    private Map<String, Object> connectorProviderProperties;

    /**
     * Basic constructor
     */
    @PublicAtsApi
    public RestClientConfigurator() {

        providers = new ArrayList<Object>();
        providerClasses = new ArrayList<Class<?>>();
        properties = new HashMap<String, Object>();
        connectorProviderProperties = new HashMap<String, Object>();
    }

    /**
     * Set certificate file
     * 
     * @param certFileName certificate file path
     * @param certPassword certificate file password
     */
    @PublicAtsApi
    public void setSSLCertificate(
                                   String certFileName,
                                   String certPassword ) {

        this.certFileName = certFileName;
        this.certPassword = certPassword;
    }

    String getCertificateFileName() {

        return this.certFileName;
    }

    String getCertificateFilePassword() {

        return this.certPassword;
    }

    /**
     * Add instances of providers, filters or interceptors which will be
     * registered to the REST client prior to executing HTTP method.
     * 
     * The exact instances are specific for the internal implementation.
     * 
     * @param provider the provider instance
     */
    @PublicAtsApi
    public void registerProvider(
                                  Object provider ) {

        providers.add(provider);
    }

    /**
     * Add classes of providers, filters or interceptors which will be
     * registered to the REST client prior to executing HTTP method.
     * 
     * The exact classes are specific for the internal implementation.
     * 
     * @param providerClass the provider class or instance
     */
    @PublicAtsApi
    public void registerProviderClass(
                                       Class<?> providerClass ) {

        providerClasses.add(providerClass);
    }

    /**
     * Set a client property. 
     * This is specific for the internal implementation.
     * 
     * @param name property name
     * @param value property value
     */
    @PublicAtsApi
    public void setProperty(
                             String name,
                             Object value ) {

        properties.put(name, value);
    }

    /**
     * Register third-party {@link ConnectorProvider}, like ApacheConnector, etc, along with (optional) configuration properties for the provider
     * @param connectorProvider - the connection provider
     * @param properties - optional configuration properties for the connection provider
     **/
    @PublicAtsApi
    public void registerConnectorProvider( ConnectorProvider connectorProvider,
                                           Map<String, Object> properties ) {

        this.connectorProvider = connectorProvider;
        if (properties != null) {
            this.connectorProviderProperties.putAll(properties);
        }

    }

    RestClientConfigurator newCopy() {

        RestClientConfigurator newConfigurator = new RestClientConfigurator();

        newConfigurator.certFileName = this.certFileName;

        newConfigurator.certPassword = this.certPassword;

        newConfigurator.providers = new ArrayList<Object>();
        for (Object provider : this.providers) {
            newConfigurator.providers.add(provider);
        }

        newConfigurator.providerClasses = new ArrayList<Class<?>>();
        for (Class<?> providerClass : this.providerClasses) {
            newConfigurator.providerClasses.add(providerClass);
        }

        newConfigurator.properties = new HashMap<String, Object>();
        for (Entry<String, Object> propEntry : this.properties.entrySet()) {
            newConfigurator.properties.put(propEntry.getKey(), propEntry.getValue());
        }

        newConfigurator.connectorProvider = this.connectorProvider;
        if (this.connectorProviderProperties != null) {
            newConfigurator.connectorProviderProperties.putAll(this.connectorProviderProperties);
        }

        return newConfigurator;
    }

    List<Object> getProviders() {

        return providers;
    }

    List<Class<?>> getProviderClasses() {

        return providerClasses;
    }

    Map<String, Object> getProperties() {

        return properties;
    }

    Map<String, Object> getConnectorProviderProperties() {

        return connectorProviderProperties;
    }

    ConnectorProvider getConnectorProvider() {

        return connectorProvider;
    }
}
