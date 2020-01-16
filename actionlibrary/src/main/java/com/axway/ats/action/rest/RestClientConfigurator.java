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

import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.HttpClientConnectionManager;
import org.apache.http.conn.HttpConnectionFactory;
import org.glassfish.jersey.client.spi.ConnectorProvider;

import com.axway.ats.common.PublicAtsApi;

/**
 * A class used to configure the REST client
 */
@PublicAtsApi
public class RestClientConfigurator {

    private List<Object>                providers;
    private List<Class<?>>              providerClasses;
    private Map<String, Object>         properties;

    private String                      certFileName;
    private String                      certPassword;

    /**
     * Provide third-party {@link ConnectorProvider}, like ApacheConnector, etc
     * */
    private ConnectorProvider           connectorProvider;
    /**
     * Additional properties for configuring the  {@link ConnectorProvider} <br>
     * If there is no connector provider, those properties will not be applied.
     * */
    private Map<String, Object>         connectorProviderProperties;

    /*
     * TODO: httpclient dependency is needed for that class even if ApacheConnector will not be used.
     *       Make another class that extends this one and is only responsible for ApacheConnector configuration
     * */
    private HttpClientConnectionManager connectionManager = null;

    private HttpConnectionFactory       connectionFactory = null;

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
     * @param certFileName certificate file path. It must be a {@link KeyStore} file (.jks or .p12)
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
     * Register third-party {@link ConnectorProvider}, like org.glassfish.jersey.apache.connector.ApacheConnectorProvider, etc, along with (optional) configuration properties for the provider.<br>
     * If not specified, <code>org.glassfish.jersey.client.HttpUrlConnectorProvider</code> is used as a connection provider.<br>
     * <br>Note: Currently only Apache connector is expected to work properly. Other connector providers may or may not work if additional configuration is needed.
     * 
     * @param connectorProvider - the connection provider
     * @param properties - (optional) configuration properties for the connection provider
     **/
    @PublicAtsApi
    public void registerConnectorProvider( ConnectorProvider connectorProvider,
                                           Map<String, Object> properties ) {

        this.connectorProvider = connectorProvider;
        if (properties != null) {
            this.connectorProviderProperties.putAll(properties);
        }

    }

    /**
     * Use org.glassfish.jersey.apache.connector.ApacheConnectorProvider as a provider.<br>
     * Note that an additional dependency (jersey-apache-connector) must be specified before using this method.
     */
    @PublicAtsApi
    public void registerApacheConnectorProvider() {

        try {
            registerApacheConnectorProvider((ConnectorProvider) Class.forName(RestClient.APACHE_CONNECTOR_CLASSNAME)
                                                                     .newInstance(),
                                            null, null, null);
        } catch (Exception e) {
            throw new RuntimeException("Could not register connector provider '" + RestClient.APACHE_CONNECTOR_CLASSNAME
                                       + "'", e);
        }

    }

    /**
     * Use org.glassfish.jersey.apache.connector.ApacheConnectorProvider as a provider
     * @param properties - (optional) configuration properties for the connection provider
     */
    @PublicAtsApi
    public void registerApacheConnectorProvider( Map<String, Object> properties ) {

        try {
            registerApacheConnectorProvider((ConnectorProvider) Class.forName(RestClient.APACHE_CONNECTOR_CLASSNAME)
                                                                     .newInstance(),
                                            properties, null, null);
        } catch (Exception e) {
            throw new RuntimeException("Could not register connector provider '" + RestClient.APACHE_CONNECTOR_CLASSNAME
                                       + "'", e);
        }
    }

    /** <strong>Note</strong>: For internal usage only. Using this method may lead to undesired effects
     * Use org.glassfish.jersey.apache.connector.ApacheConnectorProvider as a provider<br>
     * @param connectorProvider - the connector provider's class
     * @param properties - (optional) configuration properties for the connection provider.
     * <strong>Note</strong>: If connections will be done over SSL (HTTPS), any of the needed configuration must be done by you.
     * ATS will NOT apply any of the logic, related to that functionality if connectionManager parameter is not null.
     * @param connectionManager - (optional) specify the connection manager to be used with the connector provider. If this parameter is not null, connection factory must also be provider
     * @param connectionFactory - (optional) specify the connection factory
     */
    public void registerApacheConnectorProvider( ConnectorProvider connectorProvider,
                                                 Map<String, Object> properties,
                                                 HttpClientConnectionManager connectionManager,
                                                 HttpConnectionFactory connectionFactory ) {

        try {
            Class<?> apacheClientProperties = Class.forName(RestClient.APACHE_CLIENT_PROPERTIES_CLASSNAME);

            if (properties != null) {
                if (properties.get((String) apacheClientProperties.getDeclaredField("REQUEST_CONFIG")
                                                                  .get(null)) != null) {
                    // do nothing the user has provided such property
                } else {
                    // add pool request timeout of 30 seconds
                    RequestConfig requestConfig = (RequestConfig) properties.get((String) apacheClientProperties.getDeclaredField("REQUEST_CONFIG")
                                                                                                                .get(null));
                    if (requestConfig != null) {
                        // Throw an org.apache.http.conn.ConnectionPoolTimeoutException exception if connection can not be leased/obtained from the pool after 30 sec
                        requestConfig = RequestConfig.copy(requestConfig)
                                                     .setConnectionRequestTimeout(30 * 1000)
                                                     .build();
                    } else {
                        // Throw an org.apache.http.conn.ConnectionPoolTimeoutException exception if connection can not be leased/obtained from the pool after 30 sec
                        requestConfig = RequestConfig.custom().setConnectionRequestTimeout(30 * 1000).build();
                    }

                }
            } else {
                // construct properties maps and add pool request timeout of 30 seconds
                properties = new HashMap<String, Object>();
                // Throw an org.apache.http.conn.ConnectionPoolTimeoutException exception if connection can not be leased/obtained from the pool after 30 sec
                properties.put((String) apacheClientProperties.getDeclaredField("REQUEST_CONFIG").get(null),
                               RequestConfig.custom().setConnectionRequestTimeout(30 * 1000).build());
            }
        } catch (Exception e) {
            throw new RuntimeException("Unable to register connector provider '" + RestClient.APACHE_CONNECTOR_CLASSNAME
                                       + "'", e);
        }

        registerConnectorProvider(connectorProvider, properties);
        this.connectionManager = connectionManager;
        this.connectionFactory = connectionFactory;
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

        // Only copy the connector. The Rest Client will handle the creation of a connection manager/factory
        newConfigurator.connectorProvider = this.connectorProvider;
        if (this.connectorProviderProperties != null) {
            newConfigurator.connectorProviderProperties.putAll(this.connectorProviderProperties);
        }
        newConfigurator.connectionManager = null;
        newConfigurator.connectionFactory = null;

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

    /**
     * Get the underlying connection manager that is used when ApacheConnectorProvider is used. Otherwise return null
     * */
    HttpClientConnectionManager getConnectionManager() {

        return connectionManager;

    }

    /**
     * Get the underlying connection factory that is used when ApacheConnectorProvider is used. Otherwise return null
     * */
    HttpConnectionFactory getConnectionFactory() {

        return connectionFactory;
    }

}
