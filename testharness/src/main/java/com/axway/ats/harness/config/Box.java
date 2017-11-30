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
package com.axway.ats.harness.config;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.config.exceptions.NullOrEmptyConfigurationPropertyException;

/**
 * A class representing a single box
 */
@PublicAtsApi
abstract class Box implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * List of generic properties
     */
    protected Properties      properties;

    public Box() {

        properties = new Properties();
    }

    /**
     * Get a generic property associated with this box
     * 
     * @param name name of the property
     * @return value of the property
     */
    public String getProperty(
                               String name ) {

        return properties.getProperty(name);
    }

    /**
     * Get all properties associated with this box
     */
    @SuppressWarnings( { "unchecked", "rawtypes" })
    public Map<String, Object> getProperties() {

        return new HashMap<String, Object>((Map) properties);

    }

    /**
     * Associate a property with this box (not to be exposed to clients)
     * 
     * @param name name of the property
     * @param value value of the property     
     */
    public void setProperty(
                             String name,
                             String value ) {

        properties.put(name, value);

    }

    protected void verifyNotNullNorEmptyParameter(
                                                   String key,
                                                   String value ) {

        if (value == null || value.trim().length() == 0) {
            throw new NullOrEmptyConfigurationPropertyException(key);
        }
    }

    @Override
    public String toString() {

        return ", properties = " + this.properties.toString();
    }

    /**
     * @return a new copy of this Box
     */
    @PublicAtsApi
    public abstract Box newCopy();

    protected Properties getNewProperties() {

        Properties newProperties = new Properties();

        for (Enumeration<?> keys = this.properties.propertyNames(); keys.hasMoreElements();) {
            Object key = keys.nextElement();
            newProperties.put(key, this.properties.get(key));
        }

        return newProperties;
    }
}
