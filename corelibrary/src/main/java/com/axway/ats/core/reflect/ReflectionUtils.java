/*
 * Copyright 2019 Axway Software
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.axway.ats.core.utils.ExceptionUtils;

public class ReflectionUtils {

    /**
     * Set field value from instance.</br>
     * Note that private static final fields could not handled by this method. 
     * @param instance - the object over which the search for this field will be invoked
     * @param fieldName - the field name (case-sensitive)
     * @param fieldValue - the new field value
     * @param deepSearch - whether to try and search for the field in the class and any of its super classes
     * @throws SecurityException 
     * @throws NoSuchFieldException 
     * */
    public static void setFieldValue( Object instance, String fieldName, Object fieldValue,
                                      boolean deepSearch ) throws IllegalArgumentException, IllegalAccessException,
                                                           NoSuchFieldException, SecurityException {

        Field f = getField(instance, fieldName, deepSearch);
        f.set(instance, fieldValue);
    }

    /**
     * Get field value from instance via. First get<field> will be tried and then Class.getDeclaredFiled(<field>)
     * @param instance - the object over which the search for this field will be invoked
     * @param field - the field name (case-sensitive)
     * @param deepSearch - whether to try and search for the field in the class and any of its super classes
     * */
    public static Object getFieldValue( Object instance, String fieldName, boolean deepSearch ) {

        Object fieldValue = null;
        Exception getterException = null;
        Exception nonGetterException = null;
        try {
            fieldValue = obtainFieldValueViaGetter(instance, fieldName, deepSearch);
            return fieldValue;
        } catch (Exception e) {
            getterException = e;
        }

        try {
            fieldValue = obtainFieldValueViaNonGetter(instance, fieldName, deepSearch);
            return fieldValue;
        } catch (Exception e) {
            nonGetterException = e;
        }

        throw new RuntimeException("Could not obtain field '" + fieldName + "' from class '"
                                   + instance.getClass().getName() + "' " + ( (deepSearch)
                                                                                           ? " and any of its super classes "
                                                                                           : "")
                                   + " via neither "
                                   + ("get" + String.valueOf(fieldName.charAt(0)).toUpperCase()
                                      + fieldName.substring(1))
                                   + " nor via Class.getDeclaredField(). Exceptions are as follows:\n\tVia Getter: "
                                   + ExceptionUtils.getExceptionMsg(getterException) + "\n\tVia getDeclaredField: "
                                   + ExceptionUtils.getExceptionMsg(nonGetterException));

    }

    /**
     * Get Field from class
     * @param instance - the object over which the search for this field will be invoked
     * @param field - the field name (case-sensitive)
     * @param deepSearch - whether to try and search for the field in the class and any of its super classes
     * */
    public static Field getField( Object instance, String fieldName, boolean deepSearch ) {

        Class<?> currentClass = instance.getClass();
        do {
            try {
                // try to find it by declared field
                Field f = currentClass.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f;
            } catch (Exception e) {
                if (!deepSearch) {
                    throw new RuntimeException("Error getting field '" + fieldName + "' from class '"
                                               + currentClass.getName() + "'", e);
                }

            }

        } while ( (currentClass = currentClass.getSuperclass()) != null && deepSearch);

        throw new RuntimeException("Could not get field '" + fieldName + "' from neither class '"
                                   + currentClass.getName() + "' nor any of its super classes");

    }

    /**
     * Get method from class and make it accessible, so {@link Method#invoke(Object, Object...)} can be executed later over this object
     * @param clazz - the Class (Someclass.class or someObject.getClass())
     * @param methodName - the methodName
     * @param paramTypes - the method parameter class types or an empty Class array (new Class[]{}) if the method does not have any parameters
     * @return the {@link Method} object
     * @throws SecurityException 
     * @throws NoSuchMethodException 
     * */
    public static Method getMethod( Class<?> clazz, String methodName,
                                    Class<?>[] paramTypes ) throws NoSuchMethodException, SecurityException {

        if (paramTypes == null) {
            paramTypes = new Class<?>[]{};
        }
        Method m = clazz.getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m;
    }

    private static Object obtainFieldValueViaNonGetter( Object object, String fieldName, boolean deepSearch ) {

        Class<?> currentClass = object.getClass();
        do {
            try {
                // try to find it by declared field
                Field f = currentClass.getDeclaredField(fieldName);
                f.setAccessible(true);
                return f.get(object);
            } catch (Exception e) {
                if (!deepSearch) {
                    throw new RuntimeException("Error getting field '" + fieldName + "' from class '"
                                               + currentClass.getName() + "'", e);
                }

            }

        } while ( (currentClass = currentClass.getSuperclass()) != null && deepSearch);

        throw new RuntimeException("Could not obtain field '" + fieldName + "' from neither class '"
                                   + currentClass.getName() + "' nor any of its super classes");

    }

    private static Object obtainFieldValueViaGetter( Object object, String fieldName, boolean deepSearch ) {

        Class<?> currentClass = object.getClass();
        String getFieldMethodName = "get" + String.valueOf(fieldName.charAt(0)).toUpperCase() + fieldName.substring(1);
        do {
            // try to find it by get method

            Method m = null;
            try {
                m = getMethod(currentClass, getFieldMethodName, null);
                Object fieldValue = m.invoke(object, (Object[]) null);
                return fieldValue;
            } catch (Exception e) {
                if (!deepSearch) {
                    throw new RuntimeException("Error getting field '" + fieldName + "' via method '"
                                               + getFieldMethodName
                                               + "' from class '"
                                               + currentClass.getName() + "'", e);
                }

            }

            // try to find it by declared field
        } while ( (currentClass = currentClass.getSuperclass()) != null && deepSearch);

        throw new RuntimeException("Could not obtain field '" + fieldName + "' from neither class '"
                                   + currentClass.getName() + "' nor any of its super classes via " + getFieldMethodName
                                   + "()");

    }

}
