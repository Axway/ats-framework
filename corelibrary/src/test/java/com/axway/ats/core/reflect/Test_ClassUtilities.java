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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.axway.ats.core.BaseTest;

public class Test_ClassUtilities extends BaseTest {

    @Test
    public void classForNameOrPrimitivePositiveNull() throws ClassNotFoundException {

        //test null
        assertEquals(Void.TYPE,
                     ClassUtilities.classForNameOrPrimitive(null,
                                                            Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Void.TYPE,
                     ClassUtilities.classForNameOrPrimitive("", Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Void.TYPE,
                     ClassUtilities.classForNameOrPrimitive("void",
                                                            Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Void.TYPE,
                     ClassUtilities.classForNameOrPrimitive("null",
                                                            Test_ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void classForNameOrPrimitivePositivePrimitives() throws ClassNotFoundException {

        //test with all primitive types
        assertEquals(Boolean.TYPE,
                     ClassUtilities.classForNameOrPrimitive("boolean",
                                                            Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Byte.TYPE,
                     ClassUtilities.classForNameOrPrimitive("byte",
                                                            Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Character.TYPE,
                     ClassUtilities.classForNameOrPrimitive("char",
                                                            Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Double.TYPE,
                     ClassUtilities.classForNameOrPrimitive("double",
                                                            Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Float.TYPE,
                     ClassUtilities.classForNameOrPrimitive("float",
                                                            Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Integer.TYPE,
                     ClassUtilities.classForNameOrPrimitive("int",
                                                            Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Long.TYPE,
                     ClassUtilities.classForNameOrPrimitive("long",
                                                            Test_ClassUtilities.class.getClassLoader()));
        assertEquals(Short.TYPE,
                     ClassUtilities.classForNameOrPrimitive("short",
                                                            Test_ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void classForNameOrPrimitivePositiveClass() throws ClassNotFoundException {

        //test with a real class
        assertEquals(Test_ClassUtilities.class,
                     ClassUtilities.classForNameOrPrimitive(Test_ClassUtilities.class.getName(),
                                                            Test_ClassUtilities.class.getClassLoader()));
    }

    @Test( expected = ClassNotFoundException.class)
    public void classForNameOrPrimitivNegative() throws ClassNotFoundException {

        //test with a non-existent class
        assertEquals(Test_ClassUtilities.class,
                     ClassUtilities.classForNameOrPrimitive("asd",
                                                            Test_ClassUtilities.class.getClassLoader()));
    }

    @Test
    public void classIsAccessible() {

        assertTrue(ClassUtilities.classIsAccessible(Test_ClassUtilities.class));
    }

    @Test
    public void compatibleClassesNoTypeComparisonRulePositive() {

        //no arguments
        Class<?>[] argTypes = new Class<?>[]{};
        Class<?>[] paramTypes = new Class<?>[]{};

        assertTrue(ClassUtilities.compatibleClasses(argTypes, paramTypes));

        //unboxing
        argTypes = new Class<?>[]{ String.class, Boolean.class, Long.TYPE };
        paramTypes = new Class<?>[]{ String.class, Boolean.TYPE, Long.class };

        assertTrue(ClassUtilities.compatibleClasses(argTypes, paramTypes));

        //widening conversions
        argTypes = new Class<?>[]{ String.class, Float.class, Byte.class };
        paramTypes = new Class<?>[]{ String.class, Double.TYPE, Long.class };

        assertTrue(ClassUtilities.compatibleClasses(argTypes, paramTypes));

        //inheritance
        argTypes = new Class<?>[]{ FileInputStream.class, String.class };
        paramTypes = new Class<?>[]{ InputStream.class, String.class };

        assertTrue(ClassUtilities.compatibleClasses(argTypes, paramTypes));

        //arrays and lists
        argTypes = new Class<?>[]{ (new byte[]{}).getClass(),
                                   (new ArrayList<FileInputStream>()).getClass() };
        paramTypes = new Class<?>[]{ (new byte[]{}).getClass(),
                                     (new ArrayList<FileInputStream>()).getClass() };

        assertTrue(ClassUtilities.compatibleClasses(argTypes, paramTypes));
    }

    @Test
    public void compatibleClassesNoTypeComparisonRuleNegative() {

        //null to primitive value
        Class<?>[] argTypes = new Class<?>[]{ null };
        Class<?>[] paramTypes = new Class<?>[]{ Byte.TYPE };

        assertFalse(ClassUtilities.compatibleClasses(argTypes, paramTypes));

        //wrong number of args and params
        argTypes = new Class<?>[]{ String.class, Boolean.TYPE, };
        paramTypes = new Class<?>[]{ String.class, Boolean.TYPE, Long.class };

        assertFalse(ClassUtilities.compatibleClasses(argTypes, paramTypes));

        //incompatible classes
        argTypes = new Class<?>[]{ String.class, Boolean.class, InputStream.class };
        paramTypes = new Class<?>[]{ String.class, Boolean.TYPE, Long.class };

        assertFalse(ClassUtilities.compatibleClasses(argTypes, paramTypes));

        //incompatible widening
        argTypes = new Class<?>[]{ String.class, Double.class };
        paramTypes = new Class<?>[]{ String.class, Float.class };

        assertFalse(ClassUtilities.compatibleClasses(argTypes, paramTypes));

        //one arg is primitive, other is not
        argTypes = new Class<?>[]{ String.class, Integer.TYPE, };
        paramTypes = new Class<?>[]{ String.class, String.class };

        assertFalse(ClassUtilities.compatibleClasses(argTypes, paramTypes));
    }

    @Test
    public void compatibleClassesCustomTypeComparisonRulePositive() {

        List<TypeComparisonRule> comparistonRules = new ArrayList<TypeComparisonRule>();
        comparistonRules.add(new CustomTypeComparisonRule());

        //no arguments
        Class<?>[] argTypes = new Class<?>[]{ FileInputStream.class, Byte.TYPE, Test_ClassUtilities.class };
        Class<?>[] paramTypes = new Class<?>[]{ InputStream.class, Long.class, String.class };

        assertTrue(ClassUtilities.compatibleClasses(argTypes, paramTypes, comparistonRules));

        //expected false
        argTypes = new Class<?>[]{ String.class };
        paramTypes = new Class<?>[]{ Test_ClassUtilities.class };

        assertFalse(ClassUtilities.compatibleClasses(argTypes, paramTypes, comparistonRules));
    }

    @Test
    public void primitiveEquivalentOfPositive() {

        //test with all primitive types
        assertEquals(Boolean.TYPE, ClassUtilities.primitiveEquivalentOf(Boolean.class));
        assertEquals(Byte.TYPE, ClassUtilities.primitiveEquivalentOf(Byte.class));
        assertEquals(Character.TYPE, ClassUtilities.primitiveEquivalentOf(Character.class));
        assertEquals(Double.TYPE, ClassUtilities.primitiveEquivalentOf(Double.class));
        assertEquals(Float.TYPE, ClassUtilities.primitiveEquivalentOf(Float.class));
        assertEquals(Integer.TYPE, ClassUtilities.primitiveEquivalentOf(Integer.class));
        assertEquals(Long.TYPE, ClassUtilities.primitiveEquivalentOf(Long.class));
        assertEquals(Short.TYPE, ClassUtilities.primitiveEquivalentOf(Short.class));

        //and when an actual primitive type is passed
        assertEquals(Byte.TYPE, ClassUtilities.primitiveEquivalentOf(Byte.TYPE));
    }

    @Test
    public void primitiveEquivalentOfNotAPrimitive() {

        //test with all primitive types
        assertEquals(null, ClassUtilities.primitiveEquivalentOf(Test_ClassUtilities.class));
    }

    @Test
    public void primitiveIsAssignableFromPositive() {

        assertTrue(ClassUtilities.primitiveIsAssignableFrom(Long.TYPE, Integer.TYPE));
        assertTrue(ClassUtilities.primitiveIsAssignableFrom(Double.TYPE, Float.TYPE));
    }

    @Test
    public void primitiveIsAssignableFromNegative() {

        //null
        assertFalse(ClassUtilities.primitiveIsAssignableFrom(Long.TYPE, null));
        assertFalse(ClassUtilities.primitiveIsAssignableFrom(null, Float.TYPE));

        //not a primitive type
        assertFalse(ClassUtilities.primitiveIsAssignableFrom(Long.TYPE, String.class));
        assertFalse(ClassUtilities.primitiveIsAssignableFrom(String.class, Float.TYPE));

        //widening not possible
        assertFalse(ClassUtilities.primitiveIsAssignableFrom(Integer.TYPE, Long.TYPE));
    }

    @Test
    public void getAccessibleMethodFromPositive() {

        //class
        assertNotNull(ClassUtilities.getAccessibleMethodFrom(FileInputStream.class,
                                                             "reset",
                                                             new Class<?>[]{}));

        //interface
        assertNotNull(ClassUtilities.getAccessibleMethodFrom(Thread.class, "run", new Class<?>[]{}));
    }

    private static class CustomTypeComparisonRule implements TypeComparisonRule {

        public boolean isCompatible(
                                     Class<?> argType,
                                     Class<?> parameterType ) {

            if (argType == Test_ClassUtilities.class && parameterType == String.class) {
                return true;
            }

            return false;
        }

    }
}
