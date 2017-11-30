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

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds methods and constructors that can be invoked reflectively.
 * Attempts to address some of the limitations of the JDK's
 * Class.getMethod() and Class.getConstructor(), and other JDK
 * reflective facilities.
 */

public final class MethodFinder {

    /**
     * The name of the object we search in - could be class, or list of methods
     */
    private final String                        searchedObjectName;

    /**
     * List of custom rules to apply when matching arguments with parameters
     */
    private final List<TypeComparisonRule>      customMatchingRules;

    /**
     * Mapping from method name to the Methods in the collection we search
     * with that name.
     */
    private final HashMap<String, List<Method>> methodMap    = new HashMap<String, List<Method>>();

    /**
     * List of all methods in the target collection
     */
    private final List<Method>                  methods      = new ArrayList<Method>();

    /**
     * List of the Constructors in the target collection.
     */
    private final List<Constructor<?>>          constructors = new ArrayList<Constructor<?>>();

    /**
     * Mapping from a Constructor or Method object to the Class
     * objects representing its formal parameters.
     */
    private final Map<Member, Class<?>[]>       paramMap     = new HashMap<Member, Class<?>[]>();

    /**
     * @param  targetClass  Class in which I will look for methods and
     * constructors
     * @exception  IllegalArgumentException  if targetClass is null, or
     * represents a primitive, or represents an array type
     */
    public MethodFinder( Class<?> targetClass ) {

        if (targetClass == null) {
            throw new IllegalArgumentException("null Class parameter");
        }

        if (targetClass.isPrimitive()) {
            throw new IllegalArgumentException("primitive Class parameter");
        }

        if (targetClass.isArray()) {
            throw new IllegalArgumentException("array Class parameter");
        }

        this.searchedObjectName = "class " + targetClass.getName();
        this.customMatchingRules = new ArrayList<TypeComparisonRule>();

        //load all constructors
        for (Constructor<?> constructor : targetClass.getDeclaredConstructors()) {
            constructors.add(constructor);
            paramMap.put(constructor, constructor.getParameterTypes());
        }

        this.methods.addAll(Arrays.asList(targetClass.getDeclaredMethods()));

        loadMethods(this.methods);
    }

    /**
     * @param searchedObjectName name of the object which represents the collection of methods
     * @param methods a collection of methods in which to search
     */
    public MethodFinder( String searchedObjectName,
                         List<Method> methods ) {

        this.methods.addAll(methods);
        this.searchedObjectName = searchedObjectName;
        this.customMatchingRules = new ArrayList<TypeComparisonRule>();

        loadMethods(this.methods);
    }

    /**
     * @param searchedObjectName name of the object which represents the collection of methods
     * @param methods a collection of methods in which to search
     * @param customMatchingRules rules for custom matching
     */
    public MethodFinder( String searchedObjectName,
                         List<Method> methods,
                         List<TypeComparisonRule> customMatchingRules ) {

        this.methods.addAll(methods);
        this.searchedObjectName = searchedObjectName;
        this.customMatchingRules = customMatchingRules;

        loadMethods(methods);
    }

    /**
     * Loads up the data structures for my target class's methods.
     */
    private void loadMethods(
                              List<Method> targetMethods ) {

        for (Method targetMethod : targetMethods) {

            String methodName = targetMethod.getName();
            Class<?>[] paramTypes = targetMethod.getParameterTypes();

            Class<?> targetClass = targetMethod.getDeclaringClass();

            List<Method> list = methodMap.get(methodName);

            if (list == null) {
                list = new ArrayList<Method>();
                methodMap.put(methodName, list);
            }

            if (!ClassUtilities.classIsAccessible(targetClass))
                targetMethod = ClassUtilities.getAccessibleMethodFrom(targetClass, methodName, paramTypes);

            if (targetMethod != null) {
                list.add(targetMethod);
                paramMap.put(targetMethod, paramTypes);
            }
        }
    }

    /**
     * Returns the most specific public constructor in my target class
     * that accepts the number and type of parameters in the given
     * Class array in a reflective invocation.
     * <p>
     * A null value or Void.TYPE in parameterTypes matches a
     * corresponding Object or array reference in a constructor's formal
     * parameter list, but not a primitive formal parameter.
     * 
     * @param  parameterTypes  array representing the number and
     * types of parameters to look for in the constructor's signature.  A
     * null array is treated as a zero-length array.
     * @return  Constructor object satisfying the conditions
     * @exception  NoSuchMethodException  if no constructors match
     * the criteria, or if the reflective call is ambiguous based on the
     * parameter types
     */
    public Constructor<?> findConstructor(
                                           Class<?>[] parameterTypes ) throws NoSuchMethodException,
                                                                       AmbiguousMethodException {

        if (parameterTypes == null)
            parameterTypes = new Class[0];

        return (Constructor<?>) findMemberIn(constructors, parameterTypes);
    }

    /**
     * Returns the most specific public method in my target class that
     * has the given name and accepts the number and type of
     * parameters in the given Class array in a reflective invocation.
     * <p>
     * A null value or Void.TYPE in parameterTypes will match a
     * corresponding Object or array reference in a method's formal
     * parameter list, but not a primitive formal parameter.
     * 
     * @param  methodName  name of the method to search for
     * @param  parameterTypes  array representing the number and
     * types of parameters to look for in the method's signature.  A
     * null array is treated as a zero-length array.
     * @return  Method object satisfying the conditions
     * @exception  NoSuchMethodException  if no methods match the
     * criteria, or if the reflective call is ambiguous based on the
     * parameter types, or if methodName is null
     */
    public Method findMethod(
                              String methodName,
                              Class<?>[] parameterTypes ) throws NoSuchMethodException,
                                                          AmbiguousMethodException {

        //get only the methods with the specified name
        List<Method> methodList = methodMap.get(methodName);

        if (methodList == null)
            throw new NoSuchMethodException("no method named " + methodName + " found in "
                                            + searchedObjectName);

        return findMethod(methodList, parameterTypes);
    }

    /**
     * Returns the most specific public method in my target class that
     * has the given name and accepts the number and type of
     * parameters in the given Class array in a reflective invocation.
     * <p>
     * A null value or Void.TYPE in parameterTypes will match a
     * corresponding Object or array reference in a method's formal
     * parameter list, but not a primitive formal parameter.
     * 
     * @param  methodName  name of the method to search for
     * @param  parameterTypes  array representing the number and
     * types of parameters to look for in the method's signature.  A
     * null array is treated as a zero-length array.
     * @return  Method object satisfying the conditions
     * @exception  NoSuchMethodException  if no methods match the
     * criteria, or if the reflective call is ambiguous based on the
     * parameter types, or if methodName is null
     */
    public Method findMethod(
                              Class<?>[] parameterTypes ) throws NoSuchMethodException,
                                                          AmbiguousMethodException {

        return (Method) findMemberIn(methods, parameterTypes);
    }

    /**
     * @param methodList
     * @param parameterTypes
     * @return
     * @throws NoSuchMethodException
     * @throws AmbiguousMethodException
     */
    private Method findMethod(
                               List<Method> methodList,
                               Class<?>[] parameterTypes ) throws NoSuchMethodException,
                                                           AmbiguousMethodException {

        if (parameterTypes == null)
            parameterTypes = new Class[0];

        return (Method) findMemberIn(methodList, parameterTypes);
    }

    /**
     * Basis of findConstructor() and findMethod().  The member list
     * fed to this method will be either all Constructor objects or all
     * Method objects.
     */
    private Member findMemberIn(
                                 List<? extends Member> memberList,
                                 Class<?>[] parameterTypes ) throws NoSuchMethodException,
                                                             AmbiguousMethodException {

        List<Member> matchingMembers = new ArrayList<Member>();

        for (Member member : memberList) {
            Class<?>[] methodParamTypes = paramMap.get(member);

            if (Arrays.equals(methodParamTypes, parameterTypes))
                return member;

            if (ClassUtilities.compatibleClasses(parameterTypes, methodParamTypes, customMatchingRules))
                matchingMembers.add(member);
        }

        if (matchingMembers.isEmpty())
            throw new NoSuchMethodException("No member matching given args found in " + searchedObjectName);

        if (matchingMembers.size() == 1)
            return matchingMembers.get(0);

        return findMostSpecificMemberIn(matchingMembers);
    }

    /**
     * @param  a List of Members (either all Constructors or all
     * Methods)
     * @return  the most specific of all Members in the list
     * @exception  NoSuchMethodException  if there is an ambiguity
     * as to which is most specific
     */
    private Member findMostSpecificMemberIn(
                                             List<Member> memberList ) throws NoSuchMethodException,
                                                                       AmbiguousMethodException {

        List<Member> mostSpecificMembers = new ArrayList<Member>();

        for (Member member : memberList) {

            if (mostSpecificMembers.isEmpty()) {
                // First guy in is the most specific so far.
                mostSpecificMembers.add(member);
            } else {
                boolean moreSpecific = true;
                boolean lessSpecific = false;

                // Is member more specific than everyone in the most-specific set?
                for (Member moreSpecificMember : mostSpecificMembers) {
                    if (!memberIsMoreSpecific(member, moreSpecificMember)) {
                        /* Can't be more specific than the whole set.  Bail out, and
                           mark whether member is less specific than the member
                           under consideration.  If it is less specific, it need not be
                           added to the ambiguity set.  This is no guarantee of not
                           getting added to the ambiguity set...we're just not clever
                           enough yet to make that assessment. */

                        moreSpecific = false;
                        lessSpecific = memberIsMoreSpecific(moreSpecificMember, member);
                        break;
                    }
                }

                if (moreSpecific) {
                    // Member is the most specific now.
                    mostSpecificMembers.clear();
                    mostSpecificMembers.add(member);
                } else if (!lessSpecific) {
                    // Add to ambiguity set if mutually unspecific.
                    mostSpecificMembers.add(member);
                }
            }
        }

        if (mostSpecificMembers.size() > 1) {
            throw new AmbiguousMethodException("Ambiguous request for member matching given args in "
                                               + searchedObjectName);
        }

        return mostSpecificMembers.get(0);
    }

    /**
     * @param  first  a Member
     * @param  second  a Member
     * @return  true if the first Member is more specific than the second,
     * false otherwise.  Specificity is determined according to the
     * procedure in the Java Language Specification, section 15.12.2.
     */
    private boolean memberIsMoreSpecific(
                                          Member first,
                                          Member second ) {

        Class<?>[] firstParamTypes = paramMap.get(first);
        Class<?>[] secondParamTypes = paramMap.get(second);

        return ClassUtilities.compatibleClasses(firstParamTypes, secondParamTypes, customMatchingRules);
    }

    /**
     * @param  args  an Object array
     * @return  an array of Class objects representing the classes of the
     * objects in the given Object array.  If args is null, a zero-length
     * Class array is returned.  If an element in args is null, then
     * Void.TYPE is the corresponding Class in the return array.
     */
    public static Class<?>[] getParameterTypesFrom(
                                                    Object[] args ) {

        List<Class<?>> argTypes = new ArrayList<Class<?>>();

        if (args != null) {
            for (Object arg : args) {
                if (arg == null) {
                    argTypes.add(Void.TYPE);
                } else {
                    argTypes.add(arg.getClass());
                }
            }
        }

        return argTypes.toArray(new Class<?>[]{});
    }

    /**
     * @param  classNames  String array of fully qualified names
     * (FQNs) of classes or primitives.  Represent an array type by using
     * its JVM type descriptor, with dots instead of slashes (e.g.
     * represent the type int[] with "[I", and Object[][] with
     * "[[Ljava.lang.Object;").
     * @return  an array of Class objects representing the classes or
     * primitives named by the FQNs in the given String array.  If the
     * String array is null, a zero-length Class array is returned.  If an
     * element in classNames is null, the empty string, "void", or "null",
     * then Void.TYPE is the corresponding Class in the return array.
     * If any classes require loading because of this operation, the
     * loading is done by the ClassLoader that loaded this class.  Such
     * classes are not initialized, however.
     * @exception  ClassNotFoundException  if any of the FQNs name
     * an unknown class
     */
    public static Class<?>[] getParameterTypesFrom(
                                                    String[] classNames ) throws ClassNotFoundException {

        return getParameterTypesFrom(classNames, MethodFinder.class.getClassLoader());
    }

    /**
     * @param  classNames  String array of fully qualified names
     * (FQNs) of classes or primitives.  Represent an array type by using
     * its JVM type descriptor, with dots instead of slashes (e.g.
     * represent the type int[] with "[I", and Object[][] with
     * "[[Ljava.lang.Object;").
     * @param  loader  a ClassLoader
     * @return  an array of Class objects representing the classes or
     * primitives named by the FQNs in the given String array.  If the
     * String array is null, a zero-length Class array is returned.  If an
     * element in classNames is null, the empty string, "void", or "null",
     * then Void.TYPE is the corresponding Class in the return array.
     * If any classes require loading because of this operation, the
     * loading is done by the given ClassLoader.  Such classes are not
     * initialized, however.
     * @exception  ClassNotFoundException  if any of the FQNs name
     * an unknown class
     */
    public static Class<?>[] getParameterTypesFrom(
                                                    String[] classNames,
                                                    ClassLoader loader ) throws ClassNotFoundException {

        List<Class<?>> types = new ArrayList<Class<?>>();

        if (classNames != null) {

            for (String className : classNames) {
                types.add(ClassUtilities.classForNameOrPrimitive(className, loader));
            }
        }

        return types.toArray(new Class<?>[]{});
    }
}
