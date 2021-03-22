/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.core.reflect;

import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.axway.ats.core.BaseTest;

public class Test_MethodFinder extends BaseTest {

    @Test( expected = IllegalArgumentException.class)
    public void classConstructorNegativeNullClass() throws NoSuchMethodException, AmbiguousMethodException {

        new MethodFinder(null);
    }

    @Test( expected = IllegalArgumentException.class)
    public void classConstructorNegativePrimitiveType() throws NoSuchMethodException,
                                                        AmbiguousMethodException {

        new MethodFinder(Integer.TYPE);
    }

    @Test( expected = IllegalArgumentException.class)
    public void classConstructorNegativeArrayType() throws NoSuchMethodException, AmbiguousMethodException {

        new MethodFinder( (new byte[]{}).getClass());
    }

    @Test
    public void findConstructorInClassPositive() throws NoSuchMethodException, AmbiguousMethodException {

        MethodFinder methodFinder = new MethodFinder(FileInputStream.class);

        //one argument
        assertNotNull(methodFinder.findConstructor(new Class<?>[]{ File.class }));

        //no arguments
        methodFinder = new MethodFinder(ArrayList.class);
        assertNotNull(methodFinder.findConstructor(null));
    }

    @Test( expected = NoSuchMethodException.class)
    public void findConstructorInClassNegative() throws NoSuchMethodException, AmbiguousMethodException {

        MethodFinder methodFinder = new MethodFinder(FileInputStream.class);

        //one argument
        methodFinder.findConstructor(new Class<?>[]{});
    }

    @Test
    public void findMethodWithNamePositive() throws NoSuchMethodException, AmbiguousMethodException {

        MethodFinder methodFinder = new MethodFinder(FileInputStream.class);

        //null argument
        assertNotNull(methodFinder.findMethod("close", null));

        //one argument
        assertNotNull(methodFinder.findMethod("read", new Class<?>[]{ (new byte[]{}).getClass() }));

        //several arguments
        assertNotNull(methodFinder.findMethod("read", new Class<?>[]{ (new byte[]{}).getClass(),
                                                                      Integer.TYPE,
                                                                      Integer.TYPE }));

        //unboxing
        assertNotNull(methodFinder.findMethod("read", new Class<?>[]{ (new byte[]{}).getClass(),
                                                                      Integer.class,
                                                                      Integer.class }));

        //widening conversion
        assertNotNull(methodFinder.findMethod("read", new Class<?>[]{ (new byte[]{}).getClass(),
                                                                      Byte.class,
                                                                      Short.TYPE }));
    }

    @Test( expected = NoSuchMethodException.class)
    public void findMethodWithNameNegative() throws NoSuchMethodException, AmbiguousMethodException {

        MethodFinder methodFinder = new MethodFinder(FileInputStream.class);

        //one argument
        methodFinder.findMethod("read123", new Class<?>[]{});

    }

    @Test
    public void findMethodPositive() throws NoSuchMethodException, AmbiguousMethodException {

        MethodFinder methodFinder = new MethodFinder(FileInputStream.class);

        //one argument
        assertNotNull(methodFinder.findMethod(new Class<?>[]{ (new byte[]{}).getClass() }));

        //several arguments
        assertNotNull(methodFinder.findMethod(new Class<?>[]{ (new byte[]{}).getClass(),
                                                              Integer.TYPE,
                                                              Integer.TYPE }));

        //unboxing
        assertNotNull(methodFinder.findMethod(new Class<?>[]{ (new byte[]{}).getClass(),
                                                              Integer.class,
                                                              Integer.class }));

        //widening conversion
        assertNotNull(methodFinder.findMethod(new Class<?>[]{ (new byte[]{}).getClass(),
                                                              Byte.class,
                                                              Short.TYPE }));
    }

    @Test( expected = NoSuchMethodException.class)
    public void findMethodNegative() throws NoSuchMethodException, AmbiguousMethodException {

        MethodFinder methodFinder = new MethodFinder(FileInputStream.class);

        //one argument
        methodFinder.findMethod(new Class<?>[]{ Integer.class, Integer.class, Integer.class });

    }

    @Test
    public void findMethodConstructorMethodListPositive() throws NoSuchMethodException,
                                                          AmbiguousMethodException {

        MethodFinder methodFinder = new MethodFinder("methods",
                                                     Arrays.asList(FileInputStream.class.getDeclaredMethods()));

        //one argument
        assertNotNull(methodFinder.findMethod(new Class<?>[]{ (new byte[]{}).getClass() }));

        //several arguments
        assertNotNull(methodFinder.findMethod(new Class<?>[]{ (new byte[]{}).getClass(),
                                                              Integer.TYPE,
                                                              Integer.TYPE }));

        //unboxing
        assertNotNull(methodFinder.findMethod(new Class<?>[]{ (new byte[]{}).getClass(),
                                                              Integer.class,
                                                              Integer.class }));

        //widening conversion
        assertNotNull(methodFinder.findMethod(new Class<?>[]{ (new byte[]{}).getClass(),
                                                              Byte.class,
                                                              Short.TYPE }));
    }

    @Test( expected = NoSuchMethodException.class)
    public void findMethodConstructorMethodListNegative() throws NoSuchMethodException,
                                                          AmbiguousMethodException {

        MethodFinder methodFinder = new MethodFinder("methods",
                                                     Arrays.asList(FileInputStream.class.getDeclaredMethods()));

        //one argument
        methodFinder.findMethod(new Class<?>[]{ Integer.class, Integer.class, Integer.class });

    }

    @Test( expected = AmbiguousMethodException.class)
    public void findMethodConstructorMethodListNegativeAmbigous() throws NoSuchMethodException,
                                                                  AmbiguousMethodException {

        MethodFinder methodFinder = new MethodFinder("ambiguous",
                                                     Arrays.asList(MethodFinderTester.class.getDeclaredMethods()));

        //no arguments
        methodFinder.findMethod("ambiguousMethod", new Class<?>[]{ Void.TYPE });
    }

    @Test
    public void findMethodConstructorMethodListCustomRulesPositive() throws NoSuchMethodException,
                                                                     AmbiguousMethodException {

        List<TypeComparisonRule> typeComparisonRules = new ArrayList<TypeComparisonRule>();
        typeComparisonRules.add(new CustomTypeComparisonRule());

        MethodFinder methodFinder = new MethodFinder("methods",
                                                     Arrays.asList(MethodFinderTester.class.getDeclaredMethods()),
                                                     typeComparisonRules);

        //one argument - this argument will be evaulated as String in our custom rule
        assertNotNull(methodFinder.findMethod(new Class<?>[]{ Test_MethodFinder.class }));
    }

    @Test
    public void getParameterTypesFromPositive() {

        //no arguments
        Assert.assertArrayEquals(new Class<?>[]{}, MethodFinder.getParameterTypesFrom(new Object[]{}));

        //two parameters and null
        Assert.assertArrayEquals(new Class<?>[]{ String.class, Void.TYPE, Integer.class },
                                 MethodFinder.getParameterTypesFrom(new Object[]{ new String("adf"), null, 3 }));
    }

    @Test
    public void getParameterTypesFromByClassNamePositive() throws ClassNotFoundException {

        //two parameters
        Assert.assertArrayEquals(new Class<?>[]{ Byte.TYPE, String.class },
                                 MethodFinder.getParameterTypesFrom(new String[]{ "byte", "java.lang.String" }));
    }

    @Test
    public void getParameterTypesFromByClassNameClassLoaderPositive() throws ClassNotFoundException {

        //two parameters
        Assert.assertArrayEquals(new Class<?>[]{ Byte.TYPE, Test_MethodFinder.class },
                                 MethodFinder.getParameterTypesFrom(new String[]{ "byte",
                                                                                  Test_MethodFinder.class.getName() },
                                                                    Test_MethodFinder.class.getClassLoader()));
    }

    private static class CustomTypeComparisonRule implements TypeComparisonRule {

        public boolean isCompatible(
                                     Class<?> argType,
                                     Class<?> parameterType ) {

            if (argType == Test_MethodFinder.class && parameterType == String.class) {
                return true;
            }

            return false;
        }

    }
}
