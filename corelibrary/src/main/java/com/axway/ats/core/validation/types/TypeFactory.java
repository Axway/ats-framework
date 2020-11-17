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
package com.axway.ats.core.validation.types;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.validation.ValidationType;

/**
 * This class is a singleton, responsible for the creation of the
 * different validation types. It contains the mapping between
 * the {@link ValidationType} enumeration and the actual
 * validation classes.
 *
 * Created on : Oct 10, 2007
 */
public class TypeFactory {
    private static TypeFactory  factory            = null;
    private static Logger       log                = LogManager.getLogger(TypeFactory.class);
    private static final String UNSUPPORTED_TYPE   = "The factory has encountered an unsupported validation type. ";
    private static final String ARGUMENTS_REQUIRED = "This validation type requires arguments to be passed. ";

    private static final double LOWEST_PORT        = 0;
    private static final double HIGHEST_PORT       = 65535;

    /** Default constructor */
    private TypeFactory() {

        // Empty constructor
    }

    /**
     * Creates (if nessasary) and returns the current instance of the
     * validation type factory
     * @return the current instance of the {@link TypeFactory}
     */
    public static synchronized TypeFactory getInstance() {

        if (factory == null) {
            factory = new TypeFactory();
        }

        return factory;
    }

    /**
     * Creates a validate object given the type of the object and the
     * value to be validated. This can be used outside the validation
     * package to validate certain types of data (aside from the
     * validation of the method parameters)
     *
     * @param type a certain element from the {@link ValidationType}
     * @param value the value to validate
     * @return the resulting {@link BaseType}
     */
    public synchronized BaseType createValidationType(
                                                       ValidationType type,
                                                       Object value ) {

        return createValidationType(type, null, value, null);
    }

    /**
     * Creates a validate object given the type of the object and the
     * value to be validated. This can be used outside the validation
     * package to validate certain types of data (aside from the
     * validation of the method parameters).<BR>
     * <BR>
     * This method allows the passing of additional arguments (for
     * validation types who need them).
     *
     * @param type a certain element from the {@link ValidationType}
     * @param value the value to validate
     * @param args an {@link Object} array containing the arguments
     * @return the resulting {@link BaseType}
     */
    public synchronized BaseType createValidationType(
                                                       ValidationType type,
                                                       Object value,
                                                       Object[] args ) {

        return createValidationType(type, null, value, args);
    }

    /**
     * Creates a validate object given the type of the object and the
     * value to be validated. Used in the proccess of method parameters
     * validation.
     *
     * @param type a certain element from the {@link ValidationType}
     * @param paramName the name of the parameter that is being validated
     * @param value the value to validate
     * @return the resulting {@link BaseType}
     */
    public synchronized BaseType createValidationType(
                                                       ValidationType type,
                                                       String paramName,
                                                       Object value ) {

        return createValidationType(type, paramName, value, null);
    }

    /**
     * Creates a validate object given the type of the object and the
     * value to be validated. Used in the proccess of method parameters
     * validation.<BR>
     * <BR>
     * This method allows the passing of additional arguments (for
     * validation types who need them).
     *
     * @param type a certain element from the {@link ValidationType}
     * @param paramName the name of the parameter that is being validated
     * @param value the value to validate
     * @param args an {@link Object} array containing the arguments
     * @return the resulting {@link BaseType}
     */
    public synchronized BaseType createValidationType(
                                                       ValidationType type,
                                                       String paramName,
                                                       Object value,
                                                       Object[] args ) {

        if (!this.checkRequirements(type, args)) {
            log.error(ARGUMENTS_REQUIRED);
            return null;
        }

        switch (type) {
            case NONE:
                return new TypeNone(paramName, value, args);

            case NOT_NULL:
                return new TypeObject(paramName, value, args);

            // --- STRING TYPES ---

            case STRING_NOT_EMPTY:
                return new TypeString(paramName, value, args);
            case STRING_EMAIL_ADDRESS:
                return new TypeEmailAddress(paramName, value, args);
            case STRING_CONSTANT:
                return new TypeStringConstant(paramName, value, args);
            case STRING_CONSTANT_IGNORE_CASE:
                return new TypeStringConstant(paramName, value, args, true);
            case STRING_CIDR:
                return new TypeStringCIDR(paramName, value, args);
            // validation throughout Regular expressions
            case STRING_NO_WHITESPACES:
                return new TypeRegex(paramName, value, args, TypeRegex.RegexType.WHITESPACE);
            case STRING_NO_DOUBLEQUOTED_WHITESPACE:
                return new TypeRegex(paramName, value, args, TypeRegex.RegexType.DOUBLE_QUOTED_WHITESPACE);
            case STRING_IP:
                return new TypeRegex(paramName, value, args, TypeRegex.RegexType.IPv4_ADDRESS);
            case STRING_HOST_NAME:
                return new TypeRegex(paramName, value, args, TypeRegex.RegexType.HOSTNAME);
            case STRING_DOMAIN_NAME:
                return new TypeRegex(paramName, value, args, TypeRegex.RegexType.DOMAIN);
            case STRING_DOMAIN_OR_SUBDOMAIN:
                return new TypeRegex(paramName, value, args, TypeRegex.RegexType.DOMAIN_OR_SUBDOMAIN);
            case STRING_DATE:
                return new TypeRegex(paramName, value, args, TypeRegex.RegexType.DATE);
            // multiple validations required
            case STRING_EXISTING_FILE:
                return new TypeStringFilename(paramName, value, args);
            case STRING_SERVER_ADDRESS:
                return new TypeServerName(paramName, value, args, false, false);
            case STRING_SERVER_WITH_PORT:
                return new TypeServerName(paramName, value, args, true, false);
            case STRING_SERVER_OR_CIDR:
                return new TypeServerName(paramName, value, args, false, true);

            // --- NUMBER TYPES ---

            case NUMBER_WITHIN_RANGE:
                return new TypeRange(paramName, value, args);
            case NUMBER_GREATER_THAN:
                return new TypeRange(paramName, value, new Object[]{ args[0], Double.MAX_VALUE });
            case NUMBER_PORT_NUMBER:
                return new TypeRange(paramName, value, new Object[]{ LOWEST_PORT, HIGHEST_PORT });
            case NUMBER_GREATER_THAN_ZERO:
                return new TypeRange(paramName, value, new Object[]{ 1, Double.MAX_VALUE });
            case NUMBER_POSITIVE:
                return new TypeRange(paramName, value, new Object[]{ 0, Double.MAX_VALUE });
            case NUMBER_TIMESTAMP_FULL_DAY:
                return new TypeTimestamp(paramName, value, args, true);
            case NUMBER_TIMESTAMP_FULL_HOUR:
                return new TypeTimestamp(paramName, value, args);
            case NUMBER_CONSTANT:
                return new TypeNumberConstant(paramName, value, args);

            // --- RESOURCE TYPES ---

            case FILE_EXISTING:
                return new TypeFile(paramName, value, args);
            case INPUT_STREAM_NOT_EMPTY:
                return new TypeInputStream(paramName, value, args);

            default:
                log.fatal(UNSUPPORTED_TYPE);
        }

        return null;
    }

    /** Check if the input given to the factory meets the requirements */
    private boolean checkRequirements(
                                       ValidationType type,
                                       Object[] args ) {

        int numberOfArguments = args != null
                                             ? args.length
                                             : 0;
        if (numberOfArguments < 2) {
            if (numberOfArguments < 1) {
                if (type == ValidationType.NUMBER_GREATER_THAN || type == ValidationType.STRING_CONSTANT
                    || type == ValidationType.NUMBER_CONSTANT) {
                    return false;
                }
            }
            if (type == ValidationType.NUMBER_WITHIN_RANGE) {
                return false;
            }

        }

        return true;
    }
}
