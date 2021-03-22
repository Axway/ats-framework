/*
 * Copyright 2017-2020 Axway Software
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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.apache.tools.ant.BuildException;
import org.xml.sax.SAXException;

import com.axway.ats.agent.core.ConfigurationParser;
import com.axway.ats.agent.core.action.ActionMethod;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.agent.core.model.TemplateAction;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * Used to generate server and client stub classes for @Action methods.
 */
class ActionClassGenerator {

    static {

        // Currently invoked from Ant build task so needs to be configured
        PatternLayout layout = org.apache.logging.log4j.core.layout.PatternLayout
                                                                                 .newBuilder()
                                                                                 .withPattern("%-5p %d{HH:MM:ss} %c{2}: %m%n")
                                                                                 .build();
        ConsoleAppender appender = ConsoleAppender.newBuilder().setName("ConsoleAppender").setLayout(layout).build();

        //init log4j2
        final LoggerContext context = LoggerContext.getContext(false);
        final Configuration config = context.getConfiguration();
        appender.start();
        config.addAppender(appender);
        context.updateLoggers();

    }

    private static final Logger               log            = LogManager.getLogger(ActionClassGenerator.class);

    private final static String               LINE_SEPARATOR = AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

    private String                            descriptorFileName;
    private String                            sourceDir;
    private String                            destDir;
    // the "sourcePackage" of the acgen ant task
    private String                            originalSourcePackage;
    // the "package" of the acgen ant task
    private String                            originalTargetPackage;
    private Map<String, String>               customTemplates;

    //this map holds information on whether we've already
    //added enum constants definition to a particular stub
    private HashMap<Class<?>, List<Class<?>>> addedEnumConstants;

    /**
     *
     * @param descriptorFileName Agent descriptor file to use
     * @param sourceDir source folder for action classes
     * @param destDir target folder
     * @param sourcePackage source package name (or prefix) of action classes for modification
     * @param targetPackage target package name (or prefix) to use as replacement for sourcePackage in client-side generated files
     * @param customTemplates
     */
    public ActionClassGenerator( String descriptorFileName, String sourceDir, String destDir,
                                 String sourcePackage, String targetPackage,
                                 Map<String, String> customTemplates ) {

        this.descriptorFileName = descriptorFileName;
        this.sourceDir = sourceDir;
        this.destDir = destDir;
        this.originalSourcePackage = sourcePackage;
        this.originalTargetPackage = targetPackage;
        this.customTemplates = customTemplates;
        this.addedEnumConstants = new HashMap<Class<?>, List<Class<?>>>();
    }

    public static boolean isAnActionClass( Method method ) {

        if ( (method.getAnnotation(Action.class) == null)
             && (method.getAnnotation(TemplateAction.class) == null)) {
            return false;
        }

        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }

        return true;
    }

    /**
     * Generate the action class client stub
     *
     * @throws ParserConfigurationException
     * @throws IOException
     * @throws SAXException
     * @throws ClassNotFoundException
     * @throws AgentException
     */
    public void generate() throws ParserConfigurationException, IOException, SAXException,
                           ClassNotFoundException, AgentException {

        File descriptorFile = new File(descriptorFileName);
        if (!descriptorFile.exists()) {
            throw new BuildException("Descriptor file " + descriptorFile
                                     + " does not exist. Absolute path of searched file: "
                                     + descriptorFile.getAbsolutePath());
        }

        log.info("Parsing configuration file " + descriptorFile.getAbsoluteFile());

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse(new FileInputStream(descriptorFile), descriptorFile.getAbsolutePath());

        //first generate the cleanup client class
        generateCleanupHandlerStub(configParser);

        //get the javadocs for all actions in the action class
        ActionJavadocExtractor javadocExtractor = new ActionJavadocExtractor(sourceDir);
        Map<String, String> actionJavadocMap = javadocExtractor.extractJavaDocs();

        boolean errorsWhileProcessing = false;

        //next we need to generate the action classes
        Set<String> actionClassNames = configParser.getActionClassNames();
        for (String actionClassName : actionClassNames) {

            Class<?> actionClass;

            try {
                actionClass = Class.forName(actionClassName);
            } catch (ClassNotFoundException cnfe) {
                log.error("Could not find class '" + actionClassName + "' or a referenced class", cnfe);
                errorsWhileProcessing = true;
                continue;
            } catch (ExceptionInInitializerError eiie) {
                log.error("Could not initialize a static constant or execute a static section in '"
                          + actionClassName + "' or a referenced class", eiie);
                errorsWhileProcessing = true;
                continue;
            }

            //check if stub generation should be skipped
            if (!needStubGeneration(actionClass)) {
                log.info("Skipping client stub generation for action class '" + actionClass.getName()
                         + "'");
                continue;
            }

            String targetActionClassPackage = actionClass.getPackage().getName();

            log.info("Source package: " + originalSourcePackage);

            //if the source package attribute has been initialized, then we should
            //replace it with the target package, otherwise use only the target package
            if (originalSourcePackage != null && actionClass.getName().contains(originalSourcePackage)) {
                targetActionClassPackage = actionClass.getPackage()
                                                      .getName()
                                                      .replace(originalSourcePackage,
                                                               originalTargetPackage);
            }

            //create the package folder structure
            String targetPath = createPackageIfDoesNotExist(targetActionClassPackage);
            String destFileName = targetPath + "/" + actionClass.getSimpleName() + ".java";

            log.info("Writing generated stub to '" + destFileName + "'");

            PrintWriter fileWriter = null;
            try {
                fileWriter = new PrintWriter(new FileOutputStream(new File(destFileName)));

                fileWriter.write(generateStub(configParser.getComponentName(), actionClass,
                                              targetActionClassPackage, originalTargetPackage,
                                              actionJavadocMap));
                fileWriter.flush();
            } finally {
                IoUtils.closeStream(fileWriter);
            }
        }

        if (errorsWhileProcessing) {
            throw new RuntimeException("There were some errors while creating the Agent action client and server jars. Please review the console output above.");
        }
    }

    private String createPackageIfDoesNotExist( String packageName ) {

        //replace the package separator with file separator
        String targetPath = destDir + "/" + packageName.replace(".", "/");
        File targetPathDir = new File(targetPath);
        if (!targetPathDir.exists()) {
            log.info("Creating package '" + packageName + "'");
            if (!targetPathDir.mkdirs()) {
                throw new BuildException("Could not create target package '" + targetPath + "'");
            }
        }

        return targetPath;
    }

    private String generateStub( String componentName, Class<?> actionClass, String targetActionClassPackage,
                                 String originalTargetPackage, Map<String, String> actionJavadocMap ) {

        try {
            log.info("Generating stub for action class '" + actionClass.getCanonicalName() + "'");

            //first we need to generate the method definitions
            StringBuilder methodsDefinition = new StringBuilder();
            StringBuilder publicConstants = new StringBuilder();

            Method[] actionClassMethods = actionClass.getMethods();
            for (Method actionClassMethod : actionClassMethods) {
                if (isAnActionClass(actionClassMethod)) {
                    Action actionAnnotation = actionClassMethod.getAnnotation(Action.class);
                    TemplateAction templateActionAnnotation = actionClassMethod.getAnnotation(TemplateAction.class);

                    String actionName;
                    if (actionAnnotation != null) {
                        actionName = actionAnnotation.name();
                    } else {
                        actionName = templateActionAnnotation.name();
                    }

                    // if the 'name' attribute is empty, generate an action method name
                    if (StringUtils.isNullOrEmpty(actionName)) {

                        actionName = ActionMethod.buildActionMethodName(actionClassMethod);
                    }

                    // check if this is a transfer action and it has the necessary return type
                    String transferUnit = "";
                    if (actionAnnotation != null) {
                        transferUnit = actionAnnotation.transferUnit();
                    }
                    if (actionAnnotation != null && transferUnit.length() > 0
                        && actionClassMethod.getReturnType() != Long.class) {
                        throw new BuildException("Action '" + actionName
                                                 + "' has a declared transfer unit, but the return type is not Long");
                    }

                    String actionJavaDoc = actionJavadocMap.get(actionName);

                    //first append the action javadoc (if any)
                    if (actionJavaDoc != null) {
                        methodsDefinition.append(actionJavaDoc);
                    }

                    //then process the method body and append it
                    boolean registerActionExecution = true;
                    if (actionAnnotation != null) {
                        registerActionExecution = actionAnnotation.registerActionExecution();
                    }
                    String actionDefinition = generateActionDefinition(actionName, actionClassMethod, transferUnit,
                                                                       registerActionExecution);
                    methodsDefinition.append(actionDefinition);

                    //get any enum constants
                    publicConstants.append(generateEnumConstants(actionClassMethod));
                }
            }

            //generate the public constants
            publicConstants.append(generateConstantsDefinition(actionClass));

            ClassTemplateProcessor classProcessor;
            if (customTemplates.containsKey(actionClass.getName())) {
                //use the custom template supplied
                classProcessor = new ClassTemplateProcessor(new File(customTemplates.get(actionClass.getName())),
                                                            originalSourcePackage, originalTargetPackage,
                                                            targetActionClassPackage, actionClass,
                                                            componentName, methodsDefinition.toString(),
                                                            publicConstants.toString());
            } else {
                //use default template
                classProcessor = new ClassTemplateProcessor(originalSourcePackage, originalTargetPackage,
                                                            targetActionClassPackage, actionClass,
                                                            componentName, methodsDefinition.toString(),
                                                            publicConstants.toString());
            }

            return classProcessor.processTemplate();
        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private void generateCleanupHandlerStub( ConfigurationParser configParser ) {

        String cleanupHandlerClassName = configParser.getCleanupHandler();
        log.info("Generating stub for cleanup handler class '" + cleanupHandlerClassName + "'");

        Class<?> cleanupHandlerClass;

        try {
            cleanupHandlerClass = Class.forName(cleanupHandlerClassName);
        } catch (ClassNotFoundException cnfe) {
            log.warn("Could not find class '" + cleanupHandlerClassName + "'");
            return;
        } catch (ExceptionInInitializerError eiie) {
            log.warn("Could not initialize a static constant or execute a static section in '"
                     + cleanupHandlerClassName + "' or a referenced class", eiie);
            return;
        }

        try {
            String targetActionClassPackage = originalTargetPackage;

            // if the source package attribute has been initialized, then we should
            // replace it with the target package, otherwise use only the target package
            if (originalSourcePackage != null
                && cleanupHandlerClass.getName().contains(originalSourcePackage)) {
                targetActionClassPackage = cleanupHandlerClass.getPackage()
                                                              .getName()
                                                              .replace(originalSourcePackage,
                                                                       originalTargetPackage);
            }

            ClassTemplateProcessor classProcessor;
            //use default template
            classProcessor = new ClassTemplateProcessor(ClassTemplateProcessor.CLEANUP_CLASS_TEMPLATE, "",
                                                        "", targetActionClassPackage, cleanupHandlerClass,
                                                        configParser.getComponentName(), "",
                                                        generateConstantsDefinition(cleanupHandlerClass));

            String classBody = classProcessor.processTemplate();

            //create the package folder structure
            String targetPath = createPackageIfDoesNotExist(targetActionClassPackage);
            String destFileName = targetPath + "/" + cleanupHandlerClass.getSimpleName() + ".java";

            log.info("Writing generated cleanup stub to '" + destFileName + "'");

            PrintWriter fileWriter = new PrintWriter(new FileOutputStream(new File(destFileName)));
            fileWriter.write(classBody);
            fileWriter.flush();
            fileWriter.close();

        } catch (Exception e) {
            throw new BuildException(e);
        }
    }

    private String generateConstantsDefinition( Class<?> actionClass ) throws IllegalAccessException {

        StringBuilder constantsDeclaration = new StringBuilder();

        Field[] fields = actionClass.getFields();
        for (Field field : fields) {
            int fieldModifiers = field.getModifiers();

            //get the field only if it public static final
            if (Modifier.isPublic(fieldModifiers) && Modifier.isStatic(fieldModifiers)
                && Modifier.isFinal(fieldModifiers)) {

                String fieldName = field.getName();

                log.info("Generating declaration for public static constant " + fieldName);

                Object fieldValue = field.get(null);

                constantsDeclaration.append(LINE_SEPARATOR);
                constantsDeclaration.append("    public static final ");
                constantsDeclaration.append(field.getType().getSimpleName());
                constantsDeclaration.append(" " + fieldName + " = ");
                if (fieldValue.getClass().isArray()) {
                    constantsDeclaration.append("{ ");
                    boolean firstTime = true;
                    for (int i = 0; i < Array.getLength(fieldValue); i++) {
                        Object arrayFieldValue = Array.get(fieldValue, i);
                        if (firstTime) {
                            firstTime = false;
                        } else {
                            constantsDeclaration.append(",");
                            constantsDeclaration.append(LINE_SEPARATOR);
                        }
                        if (arrayFieldValue instanceof String) {
                            //we'll need extra quotes for strings
                            constantsDeclaration.append("\"");
                            constantsDeclaration.append(arrayFieldValue);
                            constantsDeclaration.append("\"");
                        } else {
                            constantsDeclaration.append(arrayFieldValue);
                        }
                    }
                    constantsDeclaration.append("};");
                    constantsDeclaration.append(LINE_SEPARATOR);
                } else {
                    if (fieldValue instanceof String) {
                        //we'll need extra quotes for strings
                        constantsDeclaration.append("\"");
                        constantsDeclaration.append(fieldValue);
                        constantsDeclaration.append("\";");
                    } else {
                        constantsDeclaration.append(fieldValue + ";");
                    }
                    constantsDeclaration.append(LINE_SEPARATOR);
                }
            }
        }

        return constantsDeclaration.toString();
    }

    private StringBuilder generateEnumConstants( Method method ) {

        StringBuilder enumConstantsBuilder = new StringBuilder();

        for (Class<?> paramType : method.getParameterTypes()) {

            //if this is an array parameter, get the type of the elements
            if (paramType.isArray()) {
                paramType = paramType.getComponentType();
            }

            if (paramType.isEnum()) {
                //check if this definition has already been added
                List<Class<?>> addedEnums = addedEnumConstants.get(method.getDeclaringClass());
                if (addedEnums != null && addedEnums.contains(paramType)) {
                    continue;
                }

                for (Object enumConstant : paramType.getEnumConstants()) {
                    enumConstantsBuilder.append("    public static final String ");
                    enumConstantsBuilder.append(paramType.getSimpleName().toUpperCase());
                    enumConstantsBuilder.append("_");
                    enumConstantsBuilder.append(enumConstant.toString().toUpperCase());
                    enumConstantsBuilder.append(" = \"");
                    enumConstantsBuilder.append(enumConstant.toString().toUpperCase());
                    enumConstantsBuilder.append("\";" + LINE_SEPARATOR);
                }

                //add the enum type to the list
                if (addedEnums == null) {
                    addedEnums = new ArrayList<Class<?>>();
                    addedEnumConstants.put(method.getDeclaringClass(), addedEnums);
                }

                addedEnums.add(paramType);
            }
        }

        return enumConstantsBuilder;
    }

    private String generateActionDefinition( String actionName,
                                             Method actionImplementation,
                                             String transferUnit,
                                             boolean registerAction ) {

        log.info("Generating method implementation for action '" + actionName + "'");

        String[] paramNames = new String[actionImplementation.getParameterTypes().length];
        String[] paramTypes = new String[actionImplementation.getParameterTypes().length];

        Annotation[][] parameterAnnotations = actionImplementation.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Class<?> paramType = actionImplementation.getParameterTypes()[i];

            Annotation[] currentParamAnnotations = parameterAnnotations[i];

            Parameter paramAnnotation = null;
            for (int j = 0; j < currentParamAnnotations.length; j++) {
                if (currentParamAnnotations[j] instanceof Parameter) {
                    paramAnnotation = (Parameter) currentParamAnnotations[j];
                    break;
                }
            }

            if (paramAnnotation == null) {
                throw new BuildException("No @Parameter annotation for one of the parameters of action method "
                                         + actionImplementation.toString());
            }

            paramNames[i] = paramAnnotation.name();

            if (paramType.isArray() && paramType.getComponentType().isEnum()) {
                //array of enums should be represented by array of String in the generated stub
                paramTypes[i] = "String[]";
            } else if (paramType.isEnum()) {
                //enums should be represented by Strings in the generated stub
                paramTypes[i] = "String";
            } else {
                paramTypes[i] = paramType.getSimpleName();
            }
        }

        //parameters and arguments
        if (paramNames.length != paramTypes.length) {
            throw new BuildException("Parameter names count different than parameter types count for action method "
                                     + actionImplementation.toString());
        }

        Annotation deprecatedAnnotation = actionImplementation.getAnnotation(Deprecated.class);
        boolean isDeprecated = (deprecatedAnnotation != null);

        try {
            return new MethodTemplateProcessor(actionImplementation, actionName, paramNames, registerAction,
                                               paramTypes, transferUnit, isDeprecated).processTemplate();
        } catch (IOException ioe) {
            throw new BuildException(ioe);
        }
    }

    private boolean needStubGeneration( Class<?> actionClass ) {

        Annotation[] annotations = actionClass.getAnnotations();
        for (Annotation annotation : annotations) {
            if (annotation instanceof ClientStubGeneration) {
                ClientStubGeneration clientStubAnnotation = (ClientStubGeneration) annotation;
                if (clientStubAnnotation.skip()) {
                    return false;
                }
            }
        }

        return true;
    }
}
