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
package com.axway.ats.agent.core.action;

import com.axway.ats.core.reflect.TypeComparisonRule;

/**
 * This rule is used when searching for methods - we support conversion from
 * String to Enumeration, so we need to be able to find implementing methods which accept
 * Enumeration as parameters
 */
class StringToEnumRule implements TypeComparisonRule {

    @Override
    public boolean isCompatible(
                                 Class<?> argType,
                                 Class<?> parameterType ) {

        boolean isArgumentArray = argType.isArray();
        boolean isParameterArray = parameterType.isArray();

        //throw an exception of either the argument is an array and the parameter is not
        //or vice versa
        if (isArgumentArray != isParameterArray) {
            return false;
        }

        if (isArgumentArray) {
            argType = argType.getComponentType();
            parameterType = parameterType.getComponentType();
        }

        if (argType == String.class && parameterType.isEnum()) {
            //we'll leave the actual argument to Enum conversion for the execution
            return true;
        }

        return false;
    }
}
