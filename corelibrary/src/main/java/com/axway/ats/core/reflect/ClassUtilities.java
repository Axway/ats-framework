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
package com.axway.ats.core.reflect;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility routines for querying Class objects and methods.
 */

final class ClassUtilities {

    /**
     * Mapping from primitive wrapper Classes to their
     * corresponding primitive Classes.
     */
    private static final HashMap<Class<?>, Class<?>> objectToPrimitiveMap = new HashMap<Class<?>, Class<?>>(13);

    static {
        objectToPrimitiveMap.put(Boolean.class, Boolean.TYPE);
        objectToPrimitiveMap.put(Byte.class, Byte.TYPE);
        objectToPrimitiveMap.put(Character.class, Character.TYPE);
        objectToPrimitiveMap.put(Double.class, Double.TYPE);
        objectToPrimitiveMap.put(Float.class, Float.TYPE);
        objectToPrimitiveMap.put(Integer.class, Integer.TYPE);
        objectToPrimitiveMap.put(Long.class, Long.TYPE);
        objectToPrimitiveMap.put(Short.class, Short.TYPE);
    }

    /**
     * Mapping from primitive wrapper Classes to the sets of
     * primitive classes whose instances can be assigned an
     * instance of the first.
     */
    private static final HashMap<Class<?>, Set<Class<?>>> primitiveWideningsMap = new HashMap<Class<?>, Set<Class<?>>>(11);

    static {
        Set<Class<?>> set = new HashSet<Class<?>>();

        set.add(Short.TYPE);
        set.add(Integer.TYPE);
        set.add(Long.TYPE);
        set.add(Float.TYPE);
        set.add(Double.TYPE);
        primitiveWideningsMap.put(Byte.TYPE, set);

        set = new HashSet<Class<?>>();

        set.add(Integer.TYPE);
        set.add(Long.TYPE);
        set.add(Float.TYPE);
        set.add(Double.TYPE);
        primitiveWideningsMap.put(Short.TYPE, set);
        primitiveWideningsMap.put(Character.TYPE, set);

        set = new HashSet<Class<?>>();

        set.add(Long.TYPE);
        set.add(Float.TYPE);
        set.add(Double.TYPE);
        primitiveWideningsMap.put(Integer.TYPE, set);

        set = new HashSet<Class<?>>();

        set.add(Float.TYPE);
        set.add(Double.TYPE);
        primitiveWideningsMap.put(Long.TYPE, set);

        set = new HashSet<Class<?>>();

        set.add(Double.TYPE);
        primitiveWideningsMap.put(Float.TYPE, set);
    }

    /**
     * Do not instantiate.  Static methods only.
     */
    private ClassUtilities() {

    }

    /**
     * @param  name  FQN of a class, or the name of a primitive type
     * @param  loader  a ClassLoader
     * @return  the Class for the name given.  Primitive types are
     * converted to their particular Class object.  null, the empty string,
     * "null", and "void" yield Void.TYPE.  If any classes require
     * loading because of this operation, the loading is done by the
     * given class loader.  Such classes are not initialized, however.
     * @exception  ClassNotFoundException  if name names an
     * unknown class or primitive
     */
    static Class<?> classForNameOrPrimitive(
                                             String name,
                                             ClassLoader loader ) throws ClassNotFoundException {

        if (name == null || "".equals(name) || "null".equals(name) || "void".equals(name))
            return Void.TYPE;

        if ("boolean".equals(name))
            return Boolean.TYPE;

        if ("byte".equals(name))
            return Byte.TYPE;

        if ("char".equals(name))
            return Character.TYPE;

        if ("double".equals(name))
            return Double.TYPE;

        if ("float".equals(name))
            return Float.TYPE;

        if ("int".equals(name))
            return Integer.TYPE;

        if ("long".equals(name))
            return Long.TYPE;

        if ("short".equals(name))
            return Short.TYPE;

        return Class.forName(name, false, loader);
    }

    /**
     * @param  aClass  a Class
     * @return  true if the class is accessible, false otherwise.
     * Presently returns true if the class is declared public.
     */
    static boolean classIsAccessible(
                                      Class<?> aClass ) {

        return Modifier.isPublic(aClass.getModifiers());
    }

    /**
     * Tells whether instances of the classes in the 'argTypes' array
     * could be used as parameters to a reflective method
     * invocation whose parameter list has types denoted by the
     * 'paramTypes' array.
     * 
     * @param  argTypes  Class array representing the types of the
     * actual parameters of a method.  A null value or
     * Void.TYPE is considered to match a corresponding
     * @param  paramTypes  Class array representing the types of the
     * formal parameters of a method
     * Object or array class in lhs, but not a primitive.
     * @return  true if compatible, false otherwise
     */
    static boolean compatibleClasses(
                                      Class<?>[] argTypes,
                                      Class<?>[] paramTypes ) {

        return compatibleClasses(argTypes, paramTypes, new ArrayList<TypeComparisonRule>());
    }

    /**
     * Tells whether instances of the classes in the 'argTypes' array
     * could be used as parameters to a reflective method
     * invocation whose parameter list has types denoted by the
     * 'paramTypes' array.
     * 
     * @param  argTypes  Class array representing the types of the
     * actual parameters of a method.  A null value or
     * Void.TYPE is considered to match a corresponding
     * @param  paramTypes  Class array representing the types of the
     * formal parameters of a method
     * Object or array class in lhs, but not a primitive.
     * @param  typeComparisonRules  Custom rules for type comparison
     * @return  true if compatible, false otherwise
     */
    static boolean compatibleClasses(
                                      Class<?>[] argTypes,
                                      Class<?>[] paramTypes,
                                      List<TypeComparisonRule> typeComparisonRules ) {

        if (argTypes.length != paramTypes.length)
            return false;

        for (int i = 0; i < argTypes.length; ++i) {
            Class<?> argType = argTypes[i];
            Class<?> paramType = paramTypes[i];

            if (argType == null || argType.equals(Void.TYPE)) {
                if (paramType.isPrimitive()) {
                    //we can't pass null as argument to a parameter of primitive type
                    return false;
                } else {
                    //everything else is ok
                    continue;
                }
            }

            if (!paramType.isAssignableFrom(argType)) {

                //get the primitive equivalent of the argument and parameter
                //if such exists
                Class<?> lhsPrimEquiv = primitiveEquivalentOf(paramType);
                Class<?> rhsPrimEquiv = primitiveEquivalentOf(argType);

                if (!primitiveIsAssignableFrom(lhsPrimEquiv, rhsPrimEquiv)) {

                    //everything else failed, so execute the custom rules
                    if (areTypesCompatible(typeComparisonRules, argType, paramType)) {
                        continue;
                    }

                    //nothing matched, args and parameters are not compatible
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * @param  aClass  a Class
     * @param  methodName  name of a method
     * @param  parameterTypes  Class array representing the types of a method's formal parameters
     * @return  the Method with the given name and formal
     * parameter types that is in the nearest accessible class in the
     * class hierarchy, starting with aClass's superclass.  The
     * superclass and implemented interfaces of aClass are
     * searched, then their superclasses, etc. until a method is
     * found.  Returns null if there is no such method.
     */
    static Method getAccessibleMethodFrom(
                                           Class<?> aClass,
                                           String methodName,
                                           Class<?>[] parameterTypes ) {

        // Look for overridden method in the superclass.
        Class<?> superclass = aClass.getSuperclass();
        Method overriddenMethod = null;

        if (superclass != null && classIsAccessible(superclass)) {
            try {
                overriddenMethod = superclass.getMethod(methodName, parameterTypes);
            } catch (NoSuchMethodException exc) {}

            if (overriddenMethod != null)
                return overriddenMethod;
        }

        // If here, then aClass represents Object, or an interface, or
        // the superclass did not have an override.  Check
        // implemented interfaces.

        Class<?>[] interfaces = aClass.getInterfaces();

        for (int i = 0; i < interfaces.length; ++i) {
            overriddenMethod = null;

            if (classIsAccessible(interfaces[i])) {
                try {
                    overriddenMethod = interfaces[i].getMethod(methodName, parameterTypes);
                } catch (NoSuchMethodException exc) {}

                if (overriddenMethod != null)
                    return overriddenMethod;
            }
        }

        overriddenMethod = null;

        // Try superclass's superclass and implemented interfaces.
        if (superclass != null) {
            overriddenMethod = getAccessibleMethodFrom(superclass, methodName, parameterTypes);

            if (overriddenMethod != null)
                return overriddenMethod;
        }

        // Try implemented interfaces' extended interfaces...
        for (int i = 0; i < interfaces.length; ++i) {
            overriddenMethod = getAccessibleMethodFrom(interfaces[i], methodName, parameterTypes);

            if (overriddenMethod != null)
                return overriddenMethod;
        }

        // Give up.
        return null;
    }

    /**
     * @param  aClass  a Class
     * @return  the class's primitive equivalent, if aClass is a
     * primitive wrapper.  If aClass is primitive, returns aClass.
     * Otherwise, returns null.
     */
    static Class<?> primitiveEquivalentOf(
                                           Class<?> aClass ) {

        return aClass.isPrimitive()
                                    ? aClass
                                    : (Class<?>) objectToPrimitiveMap.get(aClass);
    }

    /**
     * Tells whether an instance of the primitive class
     * represented by 'rhs' can be assigned to an instance of the
     * primitive class represented by 'lhs'.
     * 
     * @param  lhs  assignee class
     * @param  rhs  assigned class
     * @return  true if compatible, false otherwise.  If either
     * argument is <code>null</code>, or one of the parameters
     * does not represent a primitive (e.g. Byte.TYPE), returns
     * false.
     */
    static boolean primitiveIsAssignableFrom(
                                              Class<?> lhs,
                                              Class<?> rhs ) {

        if (lhs == null || rhs == null)
            return false;

        if (! (lhs.isPrimitive() && rhs.isPrimitive()))
            return false;

        if (lhs.equals(rhs))
            return true;

        Set<Class<?>> wideningSet = primitiveWideningsMap.get(rhs);

        if (wideningSet == null)
            return false;

        return wideningSet.contains(lhs);
    }

    /**
     * Execute several custom type comparison rules and
     * evaluate whether the types are compatible
     * 
     * @param comparisonRules list of rules
     * @param argType the actual parameter type
     * @param paramType the formal parameter type
     * @return true if the types are compatible according to each rule
     */
    private static boolean areTypesCompatible(
                                               List<TypeComparisonRule> comparisonRules,
                                               Class<?> argType,
                                               Class<?> paramType ) {

        for (TypeComparisonRule comparisonRule : comparisonRules) {
            if (comparisonRule.isCompatible(argType, paramType)) {
                return true;
            }
        }

        return false;
    }
}
