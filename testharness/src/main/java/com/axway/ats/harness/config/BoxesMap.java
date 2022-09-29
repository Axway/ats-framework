/*
 * Copyright 2017-2022 Axway Software
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.axway.ats.config.exceptions.ConfigurationException;

@SuppressWarnings( "serial")
class BoxesMap<T extends Box> extends HashMap<String, T> {

    BoxesMap() {

    }

    /**
     * Constructor
     * 
     * @param boxesProperties the properties for all boxes
     * @param prefix the prefix of these properties
     * @param boxClass the class of the box to use for creating instances
     */
    BoxesMap( Map<String, String> boxesProperties,
              String prefix,
              Class<T> boxClass ) {

        for (Map.Entry<String, String> boxesProperty : boxesProperties.entrySet()) {

            //extract the test box name and the key value
            String key = boxesProperty.getKey();
            String value = boxesProperty.getValue();

            String[] tokens = key.split(prefix.replace(".", "\\."))[1].split("\\.");

            String boxName = tokens[0];
            StringBuilder sbBoxPropKey = new StringBuilder();
            // support properties with arbitrary number of nesting/dots
            for (int i = 1; i < tokens.length; i++) { // we start from index 1, since index = 0 is the box's name
                sbBoxPropKey.append(tokens[i] + ".");
            }

            sbBoxPropKey.setLength(sbBoxPropKey.length() - 1); // remove trailing dot
            String boxPropertyKey = sbBoxPropKey.toString();

            T box = get(boxName);
            if (box == null) {
                try {
                    box = boxClass.newInstance();
                } catch (InstantiationException ie) {
                    throw new ConfigurationException("Cannot create a new box", ie);
                } catch (IllegalAccessException iae) {
                    throw new ConfigurationException("Cannot create a new box", iae);
                }

                put(boxName, box);
            }

            //try to find a suitable setter
            boolean foundSetter = setPropertyUsingSetter(boxName, box, boxPropertyKey, value);

            //if a suitable setter was not found, just set the property
            //as a generic one
            if (!foundSetter) {
                box.setProperty(boxPropertyKey, value);
            }
        }
    }

    /**
     * Try to find a setter for this property and set it
     * 
     * @param box the box instance
     * @param key the property key
     * @param value the property value
     * @return boolean if setter was found and successfully invoked, false if not suitable
     * setter was found
     */
    private boolean setPropertyUsingSetter(
                                            String boxName,
                                            Box box,
                                            String key,
                                            String value ) {

        Method[] boxClassMethods = box.getClass().getDeclaredMethods();
        for (Method boxClassMethod : boxClassMethods) {

            String methodName = boxClassMethod.getName();
            String setterName = "set" + key;

            //look if the current method is the setter we are looking for,
            //skip otherwise. The comparison is case insensitive as to forgive 
            //user errors in configuration files
            if (!methodName.equalsIgnoreCase(setterName)) {
                continue;
            }

            //check if the setter has the right number of arguments
            Class<?>[] parameterTypes = boxClassMethod.getParameterTypes();
            if (parameterTypes.length != 1 || parameterTypes[0] != String.class) {
                continue;
            }

            String methodDescription = boxName + "." + boxClassMethod.getName() + "( '" + value + "' )";

            try {
                boxClassMethod.invoke(box, new Object[]{ value });
                return true;
            } catch (IllegalArgumentException e) {
                throw new ConfigurationException("Could not invoke setter " + methodDescription, e);
            } catch (IllegalAccessException e) {
                throw new ConfigurationException("Could not invoke setter " + methodDescription, e);
            } catch (InvocationTargetException e) {
                throw new ConfigurationException("Could not invoke setter " + methodDescription, e);
            }
        }

        return false;
    }
}
