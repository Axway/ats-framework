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
package com.axway.ats.core.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Defines a set of rules by with annotation elements can be
 * used with the validation framework defined in this package.
 * The {@link Validate#name()} field represents the method's
 * field that needs validation. The {@link Validate#type()}
 * field indicates the type of validation needed. The
 * {@link Validate#args()} contains any arguments, that may
 * be needed while validating the parameter - for instance
 * the upper and lower limit when checking if a certain integer
 * is within a certain range.
 *
 * Created on : Oct 11, 2007
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( ElementType.PARAMETER)
public @interface Validate {

    /** The {@link String} name representation of the method's parameter */
    String name();

    /** The {@link ValidationType} of the parameter */
    ValidationType type() default ValidationType.NOT_NULL;

    /** The array of {@link String} arguments to be used while validating */
    String[] args() default {};
}
