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

package com.axway.ats.harness.testng;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.axway.ats.common.PublicAtsApi;

/**
 * Used for specifying some automation test options. <br>
 * This annotation can be applied on test methods and classes but some arguments are applicable only methods. See the
 * arguments documentation.
 */
@Retention( RetentionPolicy.RUNTIME)
@Target( { METHOD, TYPE })
@PublicAtsApi
public @interface TestOptions {

    /**
     * @return the folder holding files with input test data
     */
    @PublicAtsApi
    public String dataFileFolder() default "";

    /**
     * @return a file with input test data. It holds data sheets.
     */
    @PublicAtsApi
    public String dataFile() default "";

    /**
     * Applicable only on methods
     *
     * @return the data sheet. It holds the input test data for the method it is applied on.
     */
    @PublicAtsApi
    public String dataSheet() default "";

    /**
     * Specifies the max number of times a test can be run. It can be applied on test method
     * or it containing class or some of the parent classes<br>
     * 
     * Meaningful values are numbers above 1<br>
     * 
     * <b>Note:</b> It takes effect if a Retry Analyzer is specified for a test.
     * @return
     */
    public int maxRuns() default -1;
}
