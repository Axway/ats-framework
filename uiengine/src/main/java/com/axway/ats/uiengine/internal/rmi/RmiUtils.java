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
package com.axway.ats.uiengine.internal.rmi;

import com.axway.ats.uiengine.exceptions.RmiException;

/**
 * Some utility methods for RMI operations
 */
public class RmiUtils {

    /**
     * Handle error connecting to RMI remote object
     * 
     * @param rmiElement
     * @param e
     */
    public static void handleRmiConnectionError(
                                                 RmiElement rmiElement,
                                                 Exception e ) {

        throw new RmiException( "Error connecting to RMI object of type: " + rmiElement.getRmiName(), e );
    }
}
