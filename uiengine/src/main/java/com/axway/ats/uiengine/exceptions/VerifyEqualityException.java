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
package com.axway.ats.uiengine.exceptions;

import com.axway.ats.uiengine.elements.UiElement;

public class VerifyEqualityException extends UiElementException {

    private static final long serialVersionUID = 1L;

    public VerifyEqualityException( String expectedValue,
                                    String actualValue,
                                    UiElement uiElement ) {

        super( "The expected value '" + expectedValue + "' is not equal to the actual value '" + actualValue
               + "'", uiElement );
    }

    public VerifyEqualityException( String expectedValue,
                                    UiElement uiElement ) {

        super( "The element's value is not the expected '" + expectedValue + "'", uiElement );
    }
}
