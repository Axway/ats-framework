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
package com.axway.ats.agent.core.ant;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import org.apache.tools.ant.BuildException;

import com.axway.ats.core.utils.StringUtils;

class MethodTemplateProcessor extends TemplateProcessor {

    private static final String DEFAULT_METHOD_TEMPLATE = "templates/method.template";

    public MethodTemplateProcessor( Method actionImplementation,
                                    String actionName,
                                    String[] paramNames,
                                    boolean registerAction,
                                    String[] paramTypes,
                                    String transferUnit,
                                    boolean isDeprecated,
                                    String requestUrl,
                                    String requestMethod ) throws IOException {

        super(ClassTemplateProcessor.class.getResourceAsStream(DEFAULT_METHOD_TEMPLATE),
              DEFAULT_METHOD_TEMPLATE);

        configureTemplate(actionImplementation, actionName, paramNames, registerAction, paramTypes, transferUnit,
                          isDeprecated, requestUrl, requestMethod, paramNames);
    }

    private void configureTemplate(
                                    Method actionImplementation,
                                    String actionName,
                                    String[] paramNames,
                                    boolean registerAction,
                                    String[] paramTypes,
                                    String transferUnit,
                                    boolean isDeprecated,
                                    String requestUrl,
                                    String requestMethod,
                                    String[] argumentsNames ) {

        try {
            placeHolderValues.put("$METHOD_NAME$",
                                  toCamelNotation(actionImplementation.getDeclaringClass().getSimpleName(),
                                                  actionName));
            placeHolderValues.put("$ACTION_NAME$", actionName);

            Class<?> returnType = actionImplementation.getReturnType();
            //construct the return type
            String returnTypeName = returnType.getSimpleName();
            StringBuilder execDefinition = new StringBuilder();
            if (!"void".equals(returnTypeName)) {
                execDefinition.append("return ( ");
                if (returnType.isPrimitive() && !returnType.isArray()) {
                    execDefinition.append(getObjectTypeForPrimitive(returnTypeName));
                } else {

                    Type genericReturnType = actionImplementation.getGenericReturnType();
                    if (genericReturnType instanceof ParameterizedType) {
                        StringBuilder typeArgsString = new StringBuilder();
                        ParameterizedType type = (ParameterizedType) genericReturnType;
                        Type[] typeArguments = type.getActualTypeArguments();
                        for (Type typeArg : typeArguments) {
                            Class<?> typeArgClass = (Class<?>) typeArg;
                            typeArgsString.append(typeArgClass.getSimpleName()).append(", ");
                        }
                        if (typeArgsString.length() > 0) {
                            returnTypeName += "<" + typeArgsString.substring(0, typeArgsString.length() - 2)
                                              + ">";
                        }
                    }
                    execDefinition.append(returnTypeName);
                }
                execDefinition.append(" ) ");
            }
            placeHolderValues.put("$RETURN_TYPE$", returnTypeName);
            placeHolderValues.put("$EXEC_RETURN_DEFINITION$", execDefinition.toString());
            if (registerAction) {
                placeHolderValues.put("$EXECUTE_ACTION$", "executeAction");
            } else {
                placeHolderValues.put("$EXECUTE_ACTION$", "executeActionWithoutRegister");
            }

            StringBuilder paramDefinition = new StringBuilder();
            StringBuilder argumentArray = new StringBuilder();

            for (int i = 0; i < paramNames.length; i++) {
                paramDefinition.append(paramTypes[i] + " " + paramNames[i] + ", ");
                argumentArray.append(paramNames[i] + ", ");
            }
            String paramDefinitionStr = paramDefinition.toString();
            String argumentArrayStr = argumentArray.toString();

            if (paramDefinition.length() > 2) {
                paramDefinitionStr = paramDefinitionStr.substring(0, paramDefinition.length() - 2);
                argumentArrayStr = argumentArrayStr.substring(0, argumentArrayStr.length() - 2);
            }
            placeHolderValues.put("$PARAMETERS$", paramDefinitionStr);
            placeHolderValues.put("$ARGUMENTS$", argumentArrayStr);
            if (!StringUtils.isNullOrEmpty(argumentArrayStr)) {
                placeHolderValues.put("$ARGUMENTS_NAMES$", "\"" + argumentArrayStr + "\".split(\",\")");
            } else {
                placeHolderValues.put("$ARGUMENTS_NAMES$", "new String[]{ }");
            }

            if (StringUtils.isNullOrEmpty(transferUnit)) {
                placeHolderValues.put("$META_KEYS$", "");
                placeHolderValues.put("$META_VALUES$", "");
            } else {
                placeHolderValues.put("$META_KEYS$", ", " + arrayToString(new String[]{ "transferUnit" }));
                placeHolderValues.put("$META_VALUES$", ", " + arrayToString(new String[]{ transferUnit }));
            }

            if (StringUtils.isNullOrEmpty(requestUrl)) {
                placeHolderValues.put("$REQUEST_URL$", ", \"\"");
            } else {
                placeHolderValues.put("$REQUEST_URL$", ", \"" + requestUrl + "\", ");
            }

            if (StringUtils.isNullOrEmpty(requestMethod)) {
                placeHolderValues.put("$REQUEST_METHOD$", ", \"\", ");
            } else {
                placeHolderValues.put("$REQUEST_METHOD$", "\"" + requestMethod + "\", ");
            }

            if (!returnTypeName.equalsIgnoreCase("void")) {
                placeHolderValues.put("$RETURN_TYPE_CLASS$", ", " + returnTypeName + ".class");
            } else {
                placeHolderValues.put("$RETURN_TYPE_CLASS$", ", null");
            }

            if (isDeprecated) {
                placeHolderValues.put("$DEPRECATED$", "    @Deprecated");
            } else {
                placeHolderValues.put("$DEPRECATED$", "");
            }

        } catch (Exception e) {
            throw new BuildException("Error building Agent action stub for action method "
                                     + actionImplementation.toString(), e);
        }
    }

    private String getObjectTypeForPrimitive(
                                              String primitiveTypeName ) {

        String result;

        if (primitiveTypeName.startsWith("int")) {
            result = primitiveTypeName.replace("int", "Integer");
        } else if (primitiveTypeName.startsWith("long")) {
            result = primitiveTypeName.replace("long", "Long");
        } else if (primitiveTypeName.startsWith("double")) {
            result = primitiveTypeName.replace("double", "Double");
        } else if (primitiveTypeName.startsWith("float")) {
            result = primitiveTypeName.replace("float", "Float");
        } else if (primitiveTypeName.startsWith("boolean")) {
            result = primitiveTypeName.replace("boolean", "Boolean");
        } else if (primitiveTypeName.startsWith("short")) {
            result = primitiveTypeName.replace("short", "Short");
        } else if (primitiveTypeName.startsWith("byte")) {
            result = primitiveTypeName.replace("byte", "Byte");
        } else {
            result = primitiveTypeName;
        }

        return result;
    }

    private String toCamelNotation(
                                    String actionClassName,
                                    String actionName ) {

        String[] words = actionName.split(" ");
        StringBuilder actionCamel = new StringBuilder();

        for (int i = 0; i < words.length; i++) {
            String word;

            if (i == 0) {
                word = words[i].toLowerCase();
            } else {
                word = words[i].substring(0, 1).toUpperCase();

                if (words[i].length() > 1) {
                    word += words[i].substring(1).toLowerCase();
                }
            }

            actionCamel.append(word);
        }

        //if the method name starts with the name of the class, remove it
        //this way we prevent duplication - e.g instead of AvPolicy.avPolicyEnable()
        //we will generate AvPolicy.enable()

        if (actionCamel.toString().toLowerCase().indexOf(actionClassName.toLowerCase()) == 0) {
            actionCamel.delete(0, actionClassName.length());
            actionCamel.setCharAt(0, Character.toLowerCase(actionCamel.charAt(0)));
        }

        return actionCamel.toString();
    }

    private String arrayToString( String[] tokens ) {

        StringBuilder sb = new StringBuilder();
        sb.append("new String[]{");
        for (String token : tokens) {
            sb.append(" \"");
            sb.append(token);
            sb.append("\",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append(" }");

        return sb.toString();
    }
}
