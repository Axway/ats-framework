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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.axway.ats.core.validation.Validate;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.Validator;

public class ReflectionUtils {

    public static List<Field>
            getAllFields( @Validate( name = "instance", type = ValidationType.NOT_NULL) Object instance,
                          @Validate( name = "deepSearch", type = ValidationType.NOT_NULL) boolean deepSearch ) {

        new Validator().validateMethodParameters(new Object[]{ instance, deepSearch });

        Set<Field> fields = new HashSet<Field>(Arrays.asList(instance.getClass().getDeclaredFields()));

        if (deepSearch) {
            Class<?> clazz = instance.getClass().getSuperclass();
            while (clazz != null) {
                Field[] fieldsArr = clazz.getDeclaredFields();
                fields.addAll(Arrays.asList(fieldsArr));
                clazz = clazz.getSuperclass();
            }
        }

        return new ArrayList<Field>(fields);
    }

    /**
     * Set field value from instance.<br>
     * Note that private static final fields could not handled by this method. 
     * @param instance - the object over which the search for this field will be invoked
     * @param fieldName - the field name (case-sensitive)
     * @param fieldValue - the new field value
     * @param deepSearch - whether to try and search for the field in the class and any of its super classes
     * @throws SecurityException 
     * @throws NoSuchFieldException 
     * */
    public static void
            setFieldValue( @Validate( name = "instance", type = ValidationType.NOT_NULL) Object instance,
                           @Validate( name = "fieldName", type = ValidationType.STRING_NOT_EMPTY) String fieldName,
                           @Validate( name = "fieldValue", type = ValidationType.NOT_NULL) Object fieldValue,
                           @Validate(
                                   name = "deepSearch",
                                   type = ValidationType.NOT_NULL) boolean deepSearch ) throws IllegalArgumentException,
                                                                                        IllegalAccessException,
                                                                                        NoSuchFieldException,
                                                                                        SecurityException {

        new Validator().validateMethodParameters(new Object[]{ instance, fieldName, fieldValue, deepSearch });
        Field f = null;
        boolean isAccessible = false;
        try {
            f = getField(instance, fieldName, deepSearch);
            isAccessible = f.isAccessible();
            if (!isAccessible) {
                f.setAccessible(true);
            }
            f.set(instance, fieldValue);
        } finally {
            if (f != null) {
                if (f.isAccessible() != isAccessible) {
                    f.setAccessible(isAccessible);
                }
            }

        }

    }

    /**
     * Get field value
     * @param instance - the object over which the search for this field will be invoked
     * @param field - the field name (case-sensitive)
     * @param deepSearch - whether to try and search for the field in the class and any of its super classes
     * */
    public static Object
            getFieldValue( @Validate( name = "instance", type = ValidationType.NOT_NULL) Object instance,
                           @Validate( name = "fieldName", type = ValidationType.STRING_NOT_EMPTY) String fieldName,
                           @Validate( name = "deepSearch", type = ValidationType.NOT_NULL) boolean deepSearch ) {

        new Validator().validateMethodParameters(new Object[]{ instance, fieldName, deepSearch });
        Field f = null;
        Object fieldValue = null;
        boolean isAccessible = false;
        try {
            f = getField(instance, fieldName, deepSearch);
            isAccessible = f.isAccessible();
            if (!isAccessible) {
                f.setAccessible(true);
            }
            fieldValue = f.get(instance);
            return fieldValue;
        } catch (Exception e) {
            throw new RuntimeException("Could not obtain field '" + fieldName + "' from class '"
                                       + instance.getClass().getName() + "' " + ( (deepSearch)
                                                                                               ? "or any of its super classes "
                                                                                               : ""),
                                       e);
        } finally {
            if (f != null) {
                if (f.isAccessible() != isAccessible) {
                    f.setAccessible(isAccessible);
                }
            }
        }

    }

    /**
     * Get Field from class
     * @param instance - the object over which the search for this field will be invoked
     * @param field - the field name (case-sensitive)
     * @param deepSearch - whether to try and search for the field in the class and any of its super classes
     * */
    public static Field
            getField( @Validate( name = "instance", type = ValidationType.NOT_NULL) Object instance,
                      @Validate( name = "fieldName", type = ValidationType.STRING_NOT_EMPTY) String fieldName,
                      @Validate( name = "deepSearch", type = ValidationType.NOT_NULL) boolean deepSearch ) {

        new Validator().validateMethodParameters(new Object[]{ instance, fieldName, deepSearch });
        Class<?> currentClass = instance.getClass();
        do {
            try {
                Field f = currentClass.getDeclaredField(fieldName);
                return f;
            } catch (Exception e) {
                if (!deepSearch) {
                    throw new RuntimeException("Error getting field '" + fieldName + "' from class '"
                                               + currentClass.getName() + "'", e);
                }

            }

        } while ( (currentClass = currentClass.getSuperclass()) != null && deepSearch);

        throw new RuntimeException("Could not get field '" + fieldName + "' from neither class '"
                                   + instance.getClass().getName() + "' nor any of its super classes");

    }

    /**
     * Invoke method
     * @param method - the Method instance
     * @param instance - the instance over which this method will be invoked
     * @param arguments - the method arguments. If the method has not arguments, pass new Object[]{}
     * @return the method result as Object
     * */
    public static Object
            invokeMethod( @Validate( name = "method", type = ValidationType.NOT_NULL) Method method,
                          @Validate( name = "instance", type = ValidationType.NOT_NULL) Object instance,
                          @Validate(
                                  name = "arguments",
                                  type = ValidationType.NOT_NULL) Object[] arguments ) throws IllegalAccessException,
                                                                                       IllegalArgumentException,
                                                                                       InvocationTargetException {

        new Validator().validateMethodParameters(new Object[]{ method, instance, arguments });
        boolean isAccessible = method.isAccessible();
        try {
            if (!isAccessible) {
                method.setAccessible(true);
            }
            return method.invoke(instance, arguments);
        } finally {
            if (method.isAccessible() != isAccessible) {
                method.setAccessible(true);
            }
        }
    }

    /**
     * Get method from class and make it accessible, so {@link Method#invoke(Object, Object...)} can be executed later over this object<br>
     * Note that the method may not be accessible, so before invoking it, you may have to call Method.setAccessible(true) and then after you are done using the method, revert that accessible flag to the original one
     * @param clazz - the Class (Someclass.class or someObject.getClass())
     * @param methodName - the methodName
     * @param paramTypes - the method parameter class types or an empty Class array (new Class[]{}) if the method does not have any parameters
     * @param deepSearch - whether to try and search for the field in the class and any of its super classes
     * @return the {@link Method} object
     * @throws SecurityException 
     * @throws NoSuchMethodException 
     * */
    public static Method
            getMethod( @Validate( name = "clazz", type = ValidationType.NOT_NULL) Class<?> clazz,
                       @Validate( name = "paramTypes", type = ValidationType.STRING_NOT_EMPTY) String methodName,
                       @Validate( name = "paramTypes", type = ValidationType.NOT_NULL) Class<?>[] paramTypes,
                       @Validate(
                               name = "deepSearch",
                               type = ValidationType.NOT_NULL) boolean deepSearch ) throws NoSuchMethodException,
                                                                                    SecurityException {

        new Validator().validateMethodParameters(new Object[]{ clazz, methodName, paramTypes, deepSearch });
        Class<?> currentClass = clazz;
        do {
            try {
                Method m = currentClass.getDeclaredMethod(methodName, paramTypes);
                return m;
            } catch (Exception e) {
                if (!deepSearch) {
                    throw new RuntimeException("Error getting method '" + methodName + "' from class '"
                                               + currentClass.getName() + "'", e);
                }
            }

        } while ( (currentClass = currentClass.getSuperclass()) != null && deepSearch);

        throw new RuntimeException("Could not get method '" + methodName + "' from neither class '"
                                   + clazz.getName() + "' nor any of its super classes");
    }

}
