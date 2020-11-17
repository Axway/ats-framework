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
package com.axway.ats.uiengine.aspect;

import java.lang.reflect.Array;
import java.util.Arrays;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

import com.axway.ats.uiengine.elements.UiElement;
import com.axway.ats.uiengine.elements.UiElementProperties;

@Aspect
public class UiEngineAspect {

    @Before( "(execution(* com.axway.ats.uiengine.elements.html.realbrowser..*.*(..)) "
             + "|| execution(* com.axway.ats.uiengine.elements.html.hiddenbrowser..*.*(..)) "
             + "|| execution(* com.axway.ats.uiengine.elements.mobile..*.*(..)) "
             + "|| execution(* com.axway.ats.uiengine.elements.swing..*.*(..)) "
             + ") && @annotation(com.axway.ats.common.PublicAtsApi)")
    public void logAction(
                           JoinPoint point ) throws Throwable {

        // initialize logger
        Logger log = null;
        if (point.getThis() != null) {
            log = LogManager.getLogger(point.getThis().getClass()); // not a static method
        } else {
            // in case of MobileButton.click() this method will return MobileElement.class, but not MobileButton.class
            // that is why it is better to use point.getThis().getClass(), but if the method is static point.getThis() is null
            log = LogManager.getLogger(point.getSignature().getDeclaringType());
        }

        String methodName = point.getSignature().getName();
        if (log.isTraceEnabled()) {
            log.trace("--> Invoked aspect for method " + methodName);
        }

        String propertiesString = " ";
        if (point.getThis() != null) { // not a static method

            UiElementProperties properties = ((UiElement) point.getThis()).getElementProperties();
            if (properties != null) { // has element properties

                if (properties.containsInternalProperty(UiElementProperties.MAP_ID_INTERNAL_PARAM)) {
                    propertiesString = properties.getInternalProperty(UiElementProperties.MAP_ID_INTERNAL_PARAM);
                } else {
                    // get the properties in the form '{key1=value1, key2=value2}'
                    propertiesString = properties.toString();
                    // remove the leading and trailing brackets
                    propertiesString = propertiesString.substring(1);
                    propertiesString = propertiesString.substring(0, propertiesString.length() - 1);
                }
            }
        }

        StringBuilder message = new StringBuilder();
        Object[] args = point.getArgs();
        if (args.length > 0) {

            message.append(methodName + "( ");
            for (Object o : args) {
                if (o instanceof String) {
                    message.append("\"" + o + "\" ,");
                } else if (o != null && o.getClass().isArray()) {
                    if (o instanceof Object[]) {
                        message.append(Arrays.asList((Object[]) o) + " ,");
                    } else {
                        message.append("[");
                        int length = Array.getLength(o);
                        for (int i = 0; i < length; i++) {
                            message.append(Array.get(o, i) + " ,");
                        }
                        message.delete(message.length() - 2, message.length());
                        message.append("]  ");
                    }
                } else {
                    message.append(o + " ,");
                }
            }

            message.deleteCharAt(message.length() - 1);
            message.append(") ");

            if (point.getThis() != null) {
                if (propertiesString.trim().isEmpty()) {
                    message.insert(0, "[  ].");
                } else {
                    message.insert(0, "[ \"" + propertiesString + "\" ].");
                }
            }
        } else if (point.getThis() == null) { // a static method
            message.insert(message.length(), "()");
        } else if (propertiesString.trim().isEmpty()) {
            message.insert(0, "[  ]." + methodName);
        } else {
            message.insert(0, "[ \"" + propertiesString + "\" ]." + methodName);
            message.insert(message.length(), "()");
        }
        log.info(message);
    }
}
