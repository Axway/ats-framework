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
package com.axway.ats.agent.core.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.axway.ats.core.validation.ValidationType;

/**
 * 
 * This annotation is used for getting the name of
 * the annotated parameter for the action.
 * Parameter name information is not available at
 * run time, that's why we need this annotation for
 * every parameter of the action method. Thus the client
 * can query the server and get the parameter types and
 * names for a specific action 
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.PARAMETER)
public @interface Parameter {

    /**
     * The name of the parameter - should be the same
     * as the actual parameter name
     */
    public String name();

    /** The {@link ValidationType} of the parameter, by
     * default no validation will be performed
     */
    public ValidationType validation() default ValidationType.NONE;

    /** The array of {@link String} arguments to be used while validating 
     * This is used by only a few of the validation types, see javadoc
     * of the validation package
     */
    public String[] args() default {};
}
