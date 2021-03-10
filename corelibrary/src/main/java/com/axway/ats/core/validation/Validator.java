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

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.reflect.AmbiguousMethodException;
import com.axway.ats.core.reflect.MethodFinder;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.validation.exceptions.InvalidInputArgumentsException;
import com.axway.ats.core.validation.exceptions.TypeException;
import com.axway.ats.core.validation.types.BaseType;
import com.axway.ats.core.validation.types.TypeFactory;

/**
 * This class may be used both to validate method parameters and any object,
 * defined within the {@link ValidationType} enumeration.
 * <ul>
 * <li>In case it is used as a method parameters validator it should be
 * instantiated and then the parameters should be passed as an array of
 * {@link Object}s (reffer to the {@link Validator#validateMethodParameters(Object[])} and
 * {@link Validator#validateMethodParameters(Object[], int)} methods).</li>
 * <li>In any other case the methods {@link Validator#validate(ValidationType, Object)}
 * and {@link Validator#validate(ValidationType, Object, Object[])} should be used.
 * Please check the method's description for more information.</li>
 * </ul>
 * @see ValidationType
 * @see Validate
 */
public class Validator {

    private static final int      DEFAULT_STACK_TRACE_OFFSET = 3;

    protected static final String VALIDATION_ERROR_MESSAGE   = "Validation failed while validating argument ";

    private static Logger         log                        = LogManager.getLogger(Validator.class);

    private final List<BaseType>  typeValidators;

    private final Stack<String>   methodNames;

    // -------------------------------------------------------------------------
    protected String popMethodName() {

        return methodNames.pop();
    }

    // -------------------------------------------------------------------------
    protected void pushMethodName( String methodName ) {

        this.methodNames.push(methodName);
    }

    public void methodEnd( boolean result ) {

        String message = "END   - " + popMethodName();
        if (result) {
            message += ". SUCCESS!";
        } else {
            message += ". FAILURE!";
        }
        log.info(message);
    }

    /** Default constructor */
    public Validator() {

        this.typeValidators = new ArrayList<BaseType>();

        methodNames = new Stack<String>();
    }

    /**
     * Validates the value of an object of a certain {@link ValidationType}
     * Returns true if the object is properly validated.
     *
     * @param type the specific object's {@link ValidationType}
     * @param value the value of the object
     * @return true if the validation was properly validated
     * @see ValidationType
     */
    public boolean validate( ValidationType type, Object value ) {

        return validate(type, value, null);
    }

    /**
     * Validates the value of an object of a certain {@link ValidationType}
     * Returns true if the object is properly validated.<BR>
     * <BR>
     * This method allows the passing of additional arguments (for
     * validation types who need them).
     *
     * @param type the specific object's {@link ValidationType}
     * @param value the value of the object
     * @param args an {@link Object} array containing the arguments
     * @return true if the validation was properly validated
     * @see ValidationType
     */
    public boolean validate(
                             ValidationType type,
                             Object value,
                             Object args[] ) {

        TypeFactory factory = TypeFactory.getInstance();
        BaseType baseType;
        if (value != null) {
            if (value.getClass().isArray()) {
                for (int i = 0; i < Array.getLength(value); i++) {
                    baseType = factory.createValidationType(type, Array.get(value, i), args);
                    if (!validate(baseType)) {
                        return false;
                    }
                }
            } else {
                baseType = factory.createValidationType(type, value, args);
                if (!validate(baseType)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    private boolean validate(
                              BaseType baseType ) {

        if (baseType != null) {
            try {
                baseType.validate();
            } catch (TypeException e) {
                log.error(VALIDATION_ERROR_MESSAGE + "." + e.getMessage());
                return false;
            }
            return true;
        }
        return false;
    }

    /**
     * Validates the method arguments, passed as an object array.
     *
     * @param errMessagePrefix top level prefix for the error message
     * @param argumentsValues the method's arguments
     */
    public void validateMethodParameters( String errMessagePrefix, Object[] argumentsValues ) {

        validateMethodParameters(errMessagePrefix, argumentsValues, DEFAULT_STACK_TRACE_OFFSET);
    }

    /**
     * Validates the method arguments, passed as an object array.
     *
     * @param argumentsValues the method's arguments
     */
    public void validateMethodParameters( Object[] argumentsValues ) {

        validateMethodParameters(null, argumentsValues, DEFAULT_STACK_TRACE_OFFSET);
    }

    /**
     * Validates the method arguments, passed as an object array. Allows
     * for specific offset settings.
     *
     * @param errMessagePrefix top level prefix for the error message
     * @param argumentsValues the method's arguments
     * @param offset the specific offset
     */
    public void validateMethodParameters( String errMessagePrefix, Object[] argumentsValues, int offset ) {

        if (StringUtils.isNullOrEmpty(errMessagePrefix)) {
            // there will be no change in the actual error message
            errMessagePrefix = "";
        } else {
            errMessagePrefix = errMessagePrefix + ": ";
        }

        init(errMessagePrefix, argumentsValues, offset);

        validate(errMessagePrefix);
    }

    /**
     * Cycles throughout the contents of the typeValidators array
     * to invoke every validation type's validate method
     */
    private void validate( String errMessagePrefix ) {

        try {
            for (BaseType type : this.typeValidators) {
                if (type != null) {
                    try {
                        type.validate();
                    } catch (TypeException e) {
                        String param = e.getParameterName() == null
                                                                    ? ": "
                                                                    : "\"" + e.getParameterName() + "\" : ";
                        throw new InvalidInputArgumentsException(errMessagePrefix + VALIDATION_ERROR_MESSAGE
                                                                 + param + e.getMessage());
                    }
                }
            }
        } finally {
            this.typeValidators.clear();
        }
    }

    // -------------------------------------------------------------------------

    /**
     * Validates the array of {@link Object} elements
     *
     * @param errMessagePrefix top level prefix for the error message
     * @param argumentsValues the {@link Object}s to validate
     * @param offset
     */
    private void init( String errMessagePrefix, Object[] argumentsValues, int offset ) {

        try {
            throw new Exception("Stack Trace");
        } catch (Exception e) {

            // get the stack-element before the last (i.e. us)
            if ( (offset < e.getStackTrace().length) && (offset >= 0)) {

                // Sometimes, using IBM JRE/JDK, the expected stack trace is changed and at the top
                // of the StackTrace stack is added one more Class - Throwable. So we have to skip it
                if (Throwable.class.getName().equals(e.getStackTrace()[0].getClassName())) {
                    offset++;
                }

                StackTraceElement stack = e.getStackTrace()[offset];
                String sClassName = stack.getClassName();
                pushMethodName(stack.getMethodName());

                // log the method name and arguments
                logMethodSignature(methodNames, argumentsValues);

                // find the method
                Annotation[][] parameterAnnotations;
                Method matchedMethod;
                Constructor<?> matchedConstructor;
                Class<?> targetClass;
                try {
                    targetClass = Class.forName(sClassName);
                    MethodFinder methodFinder = new MethodFinder(targetClass);

                    Class<?>[] parameterTypes = MethodFinder.getParameterTypesFrom(argumentsValues);

                    if ("<init>".equals(methodNames.peek())) {
                        matchedConstructor = methodFinder.findConstructor(parameterTypes);
                        parameterAnnotations = matchedConstructor.getParameterAnnotations();
                    } else {
                        matchedMethod = methodFinder.findMethod(methodNames.peek(), parameterTypes);
                        parameterAnnotations = matchedMethod.getParameterAnnotations();
                    }
                } catch (ClassNotFoundException cnfe) {
                    // throw runtime exception, as this is a fatal error
                    throw new RuntimeException(errMessagePrefix + "Could not find class '" + sClassName
                                               + "'", cnfe);
                } catch (NoSuchMethodException nsme) {
                    // throw runtime exception, as this is a fatal error
                    throw new RuntimeException(errMessagePrefix + "Could not find method '"
                                               + methodNames.peek() + "'", nsme);
                } catch (AmbiguousMethodException ame) {
                    // throw runtime exception, as this is a fatal error
                    throw new RuntimeException(errMessagePrefix + "Found ambiguous method '"
                                               + methodNames.peek() + "'", ame);
                }

                for (int i = 0; i < parameterAnnotations.length; i++) {
                    Annotation[] currentParamAnnotations = parameterAnnotations[i];

                    for (Annotation currentParamAnnotation : currentParamAnnotations) {
                        if (currentParamAnnotation instanceof Validate) {
                            // perform validation
                            Validate validateAnnotation = (Validate) currentParamAnnotation;

                            ValidationType validationType = validateAnnotation.type();
                            // if we are checking for valid constants, then the
                            // args array should contain
                            // the name of the array holding the valid constants
                            if (validationType == ValidationType.STRING_CONSTANT
                                || validationType == ValidationType.NUMBER_CONSTANT) {
                                try {
                                    String arrayName = validateAnnotation.args()[0];

                                    // get the field and set access level if
                                    // necessary
                                    Field arrayField = targetClass.getDeclaredField(arrayName);
                                    if (!arrayField.isAccessible()) {
                                        arrayField.setAccessible(true);
                                    }
                                    Object arrayValidConstants = arrayField.get(null);

                                    // convert the object array to string array
                                    String[] arrayValidConstatnsStr = new String[Array.getLength(arrayValidConstants)];
                                    for (int j = 0; j < Array.getLength(arrayValidConstants); j++) {
                                        arrayValidConstatnsStr[j] = Array.get(arrayValidConstants, j)
                                                                         .toString();
                                    }
                                    createBaseTypes(validationType, validateAnnotation.name(),
                                                    argumentsValues[i], arrayValidConstatnsStr);

                                } catch (IndexOutOfBoundsException iobe) {
                                    // throw runtime exception, as this is a
                                    // fatal error
                                    throw new RuntimeException(errMessagePrefix
                                                               + "You need to specify the name of the array with valid constants in the 'args' field of the Validate annotation");
                                } catch (Exception e1) {
                                    // throw runtime exception, as this is a
                                    // fatal error
                                    throw new RuntimeException(errMessagePrefix
                                                               + "Could not get array with valid constants");
                                }
                            } else {
                                createBaseTypes(validationType, validateAnnotation.name(),
                                                argumentsValues[i], validateAnnotation.args());
                            }
                        }
                    }
                }
            }
        }
    }

    /** Logs the signature of the method used for debuging purposes */
    private void logMethodSignature( Stack<String> methodNames, Object[] argumentsValues ) {

        StringBuffer buffer = new StringBuffer();
        buffer.append(methodNames.peek()).append("( ");

        for (int i = 0; i < argumentsValues.length; i++) {
            if (argumentsValues[i] == null) {
                buffer.append("null");
            } else {
                if (argumentsValues[i].getClass().isArray()) {
                    // we have an array, so get all elements
                    buffer.append("{ ");

                    for (int j = 0; j < Array.getLength(argumentsValues[i]); j++) {
                        if (Array.get(argumentsValues[i], j) == null) {
                            buffer.append("null");
                        } else {
                            buffer.append(Array.get(argumentsValues[i], j).toString());
                        }
                        if ( (j + 1) != Array.getLength(argumentsValues[i])) {
                            buffer.append(" , ");
                        }
                    }

                    buffer.append(" }");

                } else {
                    buffer.append(argumentsValues[i].toString());
                }
            }
            if ( (i + 1) != argumentsValues.length) {
                buffer.append(" , ");
            }
        }
        buffer.append(" )");

        log.debug("START - " + buffer);
    }

    /** Creates as much validation types as needed to validate the input data */
    private void createBaseTypes( ValidationType type, String paramName, Object values, Object[] args ) {

        // if this is an array of types to be validated, then add each
        // of them separatly to the list
        if ( (values != null) && values.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(values); i++) {
                Object value = Array.get(values, i);
                TypeFactory factory = TypeFactory.getInstance();
                BaseType baseType = factory.createValidationType(type, paramName, value, args);
                this.typeValidators.add(baseType);
            }
            // otherwise just add the single validation type
        } else {
            TypeFactory factory = TypeFactory.getInstance();

            String message = new StringBuilder().append("Validating if parameter with the name of [")
                                                .append(paramName)
                                                .append("] and value [")
                                                .append(values)
                                                .append("] is by the type of [")
                                                .append(type)
                                                .append("]")
                                                .toString();
            log.debug(message);

            BaseType baseType = factory.createValidationType(type, paramName, values, args);
            this.typeValidators.add(baseType);
        }
    }
}
