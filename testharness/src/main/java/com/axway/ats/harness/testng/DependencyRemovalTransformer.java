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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import org.testng.annotations.ITestAnnotation;
import org.testng.internal.annotations.IAnnotationTransformer;

/**
 * This transformer will remove all dependencies of a given test.
 * This is particularly useful when a test depends on a lot of other tests, but
 * you want to run only this particular test
 */
public class DependencyRemovalTransformer implements IAnnotationTransformer {

    /* (non-Javadoc)
     * @see org.testng.internal.annotations.IAnnotationTransformer#transform(org.testng.internal.annotations.ITest, java.lang.Class, java.lang.reflect.Constructor, java.lang.reflect.Method)
     */
    public void transform(
                           ITestAnnotation annotation,
                           Class testClass,
                           Constructor testConstructor,
                           Method testMethod ) {

        annotation.setDependsOnMethods(null);
        annotation.setDependsOnGroups(null);
    }

}
