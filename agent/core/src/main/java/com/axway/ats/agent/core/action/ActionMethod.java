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
package com.axway.ats.agent.core.action;

import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.agent.core.exceptions.ActionExecutionException;
import com.axway.ats.agent.core.exceptions.InternalComponentException;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.core.validation.ValidationType;
import com.axway.ats.core.validation.exceptions.InvalidInputArgumentsException;
import com.axway.ats.core.validation.exceptions.TypeException;
import com.axway.ats.core.validation.types.BaseType;
import com.axway.ats.core.validation.types.TypeFactory;

/**
 * This class represents a single action-implementing method relation
 * It can be used to invoke the method which implements the current action
 */
public class ActionMethod {

    private static final Logger log                                         = LogManager.getLogger(ActionMethod.class);

    protected String            componentName;
    protected String            actionName;                                                                        // as defined by the user in the name attribute of the Action annotation
    private boolean             registerActionExecution                     = true;
    private boolean             registerActionExecutionInQueueExecutionTime = true;
    private String              transferUnit;
    private Method              method;
    private Class<?>            actualClass;
    private List<String>        parameterNames;
    private boolean             isDeprecated;
    protected boolean           hasEnumParameter;

    /**
     * @param componentName name of the component
     * @param actionName name of the action
     * @param actionClassName the java class name implementing this action
     * @param actionMethodName the java method name implementing this action
     * @param method the java method
     * @param actualClass the class with the java method implementation
     */
    public ActionMethod( String componentName, String actionName, Method method, Class<?> actualClass ) {

        this.componentName = componentName;
        this.actionName = actionName;
        this.method = method;
        this.actualClass = actualClass;
        this.isDeprecated = false;
        this.hasEnumParameter = false;
        this.transferUnit = "";

        //get the annotation attributes
        Action actionAnnotation = method.getAnnotation(Action.class);
        if (actionAnnotation != null) {
            this.transferUnit = actionAnnotation.transferUnit();
            this.registerActionExecution = actionAnnotation.registerActionExecution();
            if (registerActionExecution) {
                this.registerActionExecutionInQueueExecutionTime = actionAnnotation.registerActionExecutionInQueueExecutionTime();
            }
        }

        //get the parameter names in their order
        //generate a map of the parameters
        this.parameterNames = new ArrayList<String>();
        Annotation[][] methodParameterAnnotations = method.getParameterAnnotations();
        for (int i = 0; i < methodParameterAnnotations.length; i++) {
            Annotation[] paramAnnotations = methodParameterAnnotations[i];
            for (Annotation paramAnnotation : paramAnnotations) {
                if (paramAnnotation instanceof Parameter) {
                    parameterNames.add( ((Parameter) paramAnnotation).name());
                }
            }
        }

        //check if this method is deprecated
        Annotation deprecatedAnnotation = method.getAnnotation(Deprecated.class);
        if (deprecatedAnnotation != null) {
            this.isDeprecated = true;
        }

        //check if this method has an Enumeration parameter
        for (Class<?> paramType : method.getParameterTypes()) {
            if (paramType.isEnum() || (paramType.isArray() && paramType.getComponentType().isEnum())) {
                this.hasEnumParameter = true;
                break;
            }
        }
    }

    /**
     * Invoke the given method
     *
     * @param instance an instance on which to invoke the method
     * @param args arguments
     * @return result of the method invocation
     * @throws ActionExecutionException if there was an error while executing the action
     * @throws InternalComponentException if an exception was thrown while executing the method
     */
    public Object invoke( Object instance, Object[] parameterValues,
                          boolean validateArguments ) throws ActionExecutionException,
                                                      InternalComponentException {

        try {
            if (isDeprecated()) {
                log.warn("Method '" + this.toString() + "' is deprecated");
            }

            //convert string to Enumerations if necessary
            if (hasEnumParameter) {
                parameterValues = convertToEnums(parameterValues);
            }

            //validate the arguments
            if (validateArguments) {
                validateArguments(parameterValues);
            }

            //invoke the action
            return doInvoke(instance, this.parameterNames, parameterValues);
        } catch (IllegalArgumentException ae) {
            throw new ActionExecutionException("Illegal arguments passed to action '" + actionName + "'",
                                               ae);
        } catch (IllegalAccessException iae) {
            throw new ActionExecutionException("Could not access action '" + actionName + "'", iae);
        } catch (InvocationTargetException ite) {
            throw new InternalComponentException(componentName, actionName, ite.getTargetException());
        }
    }

    protected Object doInvoke( Object instance, List<String> parameterNames,
                               Object[] parameterValues ) throws IllegalArgumentException,
                                                          IllegalAccessException, InvocationTargetException,
                                                          ActionExecutionException {

        /*
         * Here we log the action we are going to be execute.
         *
         * ATS has some actions for internal usage and users should not see them.
         * Currently we do not have some good way to distinguish these actions from the regular ones, for
         * example we could use a new attribute in the Action annotation.
         * For now we can filter these ATS internal actions by expecting their names match the next regular
         * expression.
         */
        if (log.isInfoEnabled()) {
            if (!actionName.matches("Internal.*Operations.*")
                && !actionName.startsWith("InternalProcessTalker")) {
                log.info("Executing '" + actionName + "' with arguments "
                         + StringUtils.methodInputArgumentsToString(parameterValues));
            } else {
                // internal action
                if (log.isDebugEnabled())
                    log.debug("Executing '" + actionName + "' with arguments "
                              + StringUtils.methodInputArgumentsToString(parameterValues));
            }
        }

        return method.invoke(instance, parameterValues);
    }

    /**
     * Has this action method been deprecated
     *
     * @return true if the method is deprecated, false if not
     */
    public boolean isDeprecated() {

        return isDeprecated;
    }

    public boolean isRegisterActionExecution() {

        return registerActionExecution;
    }

    public boolean isRegisterActionExecutionInQueueExecutionTime() {

        return registerActionExecutionInQueueExecutionTime;
    }

    /**
     * Get the transfer unit associated with this action
     *
     * @return the transfer unit, empty string if not set
     */
    public String getTransferUnit() {

        if (transferUnit.length() > 0) {
            return transferUnit + "/sec";
        } else {
            return transferUnit; // default value is empty string
        }
    }

    /**
     * @return the method which implements the action
     */
    public Method getMethod() {

        return method;
    }

    /**
     * In case of using abstract action methods, we need to make an
     * instance of the child class where the implementation is, not the
     * abstract parent class
     *
     * @return
     */
    public Class<?> getTheActualClass() {

        if (actualClass != null) {
            return actualClass;
        } else {
            return method.getDeclaringClass();
        }
    }

    /**
     * @return get the names of all parameters that this method accepts
     */
    public List<String> getParameterNames() {

        return parameterNames;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {

        return method.toString();
    }

    /**
     * Convert any String arguments to proper Enumerations if
     * necessary
     *
     * @param args the arguments
     * @return arguments with Strings converted to Enums
     * @throws ActionExecutionException if a given String cannot be converted to the proper Enum
     */
    @SuppressWarnings( { "rawtypes", "unchecked" })
    protected Object[] convertToEnums( Object[] args ) throws ActionExecutionException {

        Object[] processedArgs = new Object[args.length];

        //try to convert all strings to enums
        Class<?>[] parameterTypes = method.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {

            if (args[i] == null) {
                processedArgs[i] = null;
                continue;
            }

            boolean isParamArray = parameterTypes[i].isArray();
            Class<?> paramType;
            Class<?> argType;
            if (isParamArray) {
                paramType = parameterTypes[i].getComponentType();
                argType = args[i].getClass().getComponentType();
            } else {
                paramType = parameterTypes[i];
                argType = args[i].getClass();
            }

            if (argType == String.class && paramType.isEnum()) {
                try {
                    if (isParamArray) {
                        Object convertedEnums = Array.newInstance(paramType, Array.getLength(args[i]));

                        //convert all array elements to enums
                        for (int j = 0; j < Array.getLength(args[i]); j++) {
                            String currentValue = (String) Array.get(args[i], j);
                            if (currentValue != null) {
                                Array.set(convertedEnums, j,
                                          Enum.valueOf((Class<? extends Enum>) paramType,
                                                       currentValue));
                            }
                        }

                        processedArgs[i] = convertedEnums;
                    } else {
                        processedArgs[i] = Enum.valueOf((Class<? extends Enum>) paramType,
                                                        (String) args[i]);
                    }
                } catch (IllegalArgumentException iae) {
                    throw new ActionExecutionException("Could not convert string " + args[i]
                                                       + " to enumeration of type " + paramType.getName());
                }
            } else {
                processedArgs[i] = args[i];
            }
        }

        return processedArgs;
    }

    /**
     * Validate the arguments according to the rules specified in the action
     * using the Parameter annotations
     *
     * @param actionMethod      the implementation of the action
     * @param args              the arguments to validate
     * @throws ActionExecutionException     if exception occurs during arguments validation
     */
    protected void validateArguments( Object[] args ) throws ActionExecutionException {

        Annotation[][] annotations = method.getParameterAnnotations();
        for (int i = 0; i < annotations.length; i++) {

            Annotation[] paramAnnotations = annotations[i];

            for (Annotation paramAnnotation : paramAnnotations) {
                if (paramAnnotation instanceof Parameter) {
                    Parameter paramDescriptionAnnotation = (Parameter) paramAnnotation;
                    ValidationType validationType = paramDescriptionAnnotation.validation();

                    String[] validationArgs;

                    // if we are checking for valid constants, then the
                    // args array should contain
                    // the name of the array holding the valid constants
                    if (validationType == ValidationType.STRING_CONSTANT
                        || validationType == ValidationType.NUMBER_CONSTANT) {
                        try {
                            String arrayName = paramDescriptionAnnotation.args()[0];

                            // get the field and set access level if
                            // necessary
                            Field arrayField = method.getDeclaringClass().getDeclaredField(arrayName);
                            if (!arrayField.isAccessible()) {
                                arrayField.setAccessible(true);
                            }
                            Object arrayValidConstants = arrayField.get(null);

                            // convert the object array to string array
                            String[] arrayValidConstatnsStr = new String[Array.getLength(arrayValidConstants)];
                            for (int j = 0; j < Array.getLength(arrayValidConstants); j++) {
                                arrayValidConstatnsStr[j] = Array.get(arrayValidConstants, j).toString();
                            }

                            validationArgs = arrayValidConstatnsStr;

                        } catch (IndexOutOfBoundsException iobe) {
                            // this is a fatal error
                            throw new ActionExecutionException("You need to specify the name of the array with valid constants in the 'args' field of the Parameter annotation");
                        } catch (Exception e) {
                            // this is a fatal error
                            throw new ActionExecutionException("Could not get array with valid constants - action annotations are incorrect");
                        }
                    } else {
                        validationArgs = paramDescriptionAnnotation.args();
                    }

                    List<BaseType> typeValidators = createBaseTypes(paramDescriptionAnnotation.validation(),
                                                                    paramDescriptionAnnotation.name(),
                                                                    args[i], validationArgs);
                    //perform validation
                    for (BaseType baseType : typeValidators) {
                        if (baseType != null) {
                            try {
                                baseType.validate();
                            } catch (TypeException e) {
                                throw new InvalidInputArgumentsException("Validation failed while validating argument "
                                                                         + paramDescriptionAnnotation.name()
                                                                         + e.getMessage());
                            }
                        } else {
                            log.warn("Could not perform validation on argument "
                                     + paramDescriptionAnnotation.name());
                        }
                    }
                }
            }
        }
    }

    /** Creates as much validation types as needed to validate the input data */
    private List<BaseType> createBaseTypes( ValidationType type, String paramName, Object values,
                                            Object[] args ) {

        List<BaseType> typeValidators = new ArrayList<BaseType>();

        // if this is an array of types to be validated, then add each
        // of them separately to the list
        if ( (values != null) && values.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(values); i++) {
                Object value = Array.get(values, i);
                TypeFactory factory = TypeFactory.getInstance();
                BaseType baseType = factory.createValidationType(type, paramName, value, args);
                typeValidators.add(baseType);
            }
            // otherwise just add the single validation type
        } else {
            TypeFactory factory = TypeFactory.getInstance();
            BaseType baseType = factory.createValidationType(type, paramName, values, args);
            typeValidators.add(baseType);
        }

        return typeValidators;
    }

    public ActionMethod getNewCopy() {

        return new ActionMethod(componentName, actionName, method, actualClass);
    }

    /**
     * Builds the action method name. It is the method class name + method name split by Camel-Case words<br>
     * For example: getDescription -> MethodClassName get Description; getSMSCount -> MethodClassName get S M S Count
     *
     * @param actionMethod action method
     * @return action method name
     */
    public static String buildActionMethodName( Method actionMethod ) {

        String methodName = actionMethod.getName();
        StringBuilder actionMethodName = new StringBuilder();
        int charIndex;
        for (charIndex = 0; charIndex < methodName.length(); charIndex++) {
            char ch = methodName.charAt(charIndex);
            if (Character.isUpperCase(ch)) {
                actionMethodName.append(' ');
            }
            actionMethodName.append(ch);
        }

        return actionMethod.getDeclaringClass().getSimpleName() + " " + actionMethodName.toString().trim();
    }
}
