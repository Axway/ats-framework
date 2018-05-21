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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Set;
import java.util.TreeSet;

import org.apache.tools.ant.BuildException;

import com.axway.ats.agent.core.model.MemberClasses;

class ClassTemplateProcessor extends TemplateProcessor {

    public static final String DEFAULT_CLASS_TEMPLATE = "templates/class.template";
    public static final String CLEANUP_CLASS_TEMPLATE = "templates/cleanup_class.template";

    /**
     * Default template constructor
     * 
     * @param targetPackage
     * @param className
     * @param componentName
     * @param methodDefinitions
     * @param publicConstants
     * @throws IOException
     */
    public ClassTemplateProcessor( String originalSourcePackage,
                                   String originalTargetPackage,
                                   String targetPackage,
                                   Class<?> actionClass,
                                   String componentName,
                                   String methodDefinitions,
                                   String publicConstants ) throws IOException {

        super(ClassTemplateProcessor.class.getResourceAsStream(DEFAULT_CLASS_TEMPLATE),
              DEFAULT_CLASS_TEMPLATE);

        initializePlaceholders(originalSourcePackage,
                               originalTargetPackage,
                               targetPackage,
                               actionClass,
                               componentName,
                               "",
                               methodDefinitions,
                               publicConstants);
    }
    
    public ClassTemplateProcessor( String originalSourcePackage,
                                   String originalTargetPackage,
                                   String targetPackage,
                                   Class<?> actionClass,
                                   String initializeRequestUrl,
                                   String componentName,
                                   String methodDefinitions,
                                   String publicConstants ) throws IOException {

        super(ClassTemplateProcessor.class.getResourceAsStream(DEFAULT_CLASS_TEMPLATE),
              DEFAULT_CLASS_TEMPLATE);

        initializePlaceholders(originalSourcePackage,
                               originalTargetPackage,
                               targetPackage,
                               actionClass,
                               componentName,
                               initializeRequestUrl,
                               methodDefinitions,
                               publicConstants);
    }

    public ClassTemplateProcessor( String templateName,
                                   String originalSourcePackage,
                                   String originalTargetPackage,
                                   String targetPackage,
                                   Class<?> actionClass,
                                   String componentName,
                                   String methodDefinitions,
                                   String publicConstants ) throws IOException {

        super(ClassTemplateProcessor.class.getResourceAsStream(templateName), templateName);

        initializePlaceholders(originalSourcePackage,
                               originalTargetPackage,
                               targetPackage,
                               actionClass,
                               componentName,
                               "",
                               methodDefinitions,
                               publicConstants);
    }

    public ClassTemplateProcessor( File fileTemplate,
                                   String originalSourcePackage,
                                   String originalTargetPackage,
                                   String targetPackage,
                                   Class<?> actionClass,
                                   String componentName,
                                   String methodDefinitions,
                                   String publicConstants ) throws IOException {

        super(new FileInputStream(fileTemplate), fileTemplate.getAbsolutePath());

        initializePlaceholders(originalSourcePackage,
                               originalTargetPackage,
                               targetPackage,
                               actionClass,
                               componentName,
                               "",
                               methodDefinitions,
                               publicConstants);
    }
    
    public ClassTemplateProcessor( File fileTemplate,
                                   String originalSourcePackage,
                                   String originalTargetPackage,
                                   String targetPackage,
                                   Class<?> actionClass,
                                   String componentName,
                                   String initialRequestUrl,
                                   String methodDefinitions,
                                   String publicConstants ) throws IOException {

        super(new FileInputStream(fileTemplate), fileTemplate.getAbsolutePath());

        initializePlaceholders(originalSourcePackage,
                               originalTargetPackage,
                               targetPackage,
                               actionClass,
                               componentName,
                               initialRequestUrl,
                               methodDefinitions,
                               publicConstants);
    }

    /**
     * @param originalSourcePackage the "sourcePackage" of the acgen ant task 
     * @param originalTargetPackage the "package" of the acgen ant task
     * @param targetPackage the package of the currently processed Action class
     * @param actionClass the currently processed Action class
     * @param componentName the currently processed component
     * @param initializeRequestUrl the request URL that will be used to create instance of this Action class on the ATS agent.
     *        It is intended for internal usage only, so third-party/custom actions should pass null or empty string, because
     *        for that kind of actions passing any value for that String will have zero effect.
     * @param methodDefinitions this Action class' methods
     * @param publicConstants this Action class' public constants
     */
    private void initializePlaceholders(
                                         String originalSourcePackage,
                                         String originalTargetPackage,
                                         String targetPackage,
                                         Class<?> actionClass,
                                         String componentName,
                                         String initializeRequestUrl,
                                         String methodDefinitions,
                                         String publicConstants ) {

        try {
            MemberClasses memberClassesAnnotation = actionClass.getAnnotation(MemberClasses.class);
            StringBuilder memberClassesImport = new StringBuilder();
            StringBuilder memberClassesDeclaration = new StringBuilder();
            StringBuilder memberClassesNoArgsInitialization = new StringBuilder();
            StringBuilder memberClassesOneArgInitialization = new StringBuilder();
            StringBuilder classAnnotations = new StringBuilder();

            if (memberClassesAnnotation != null) {

                StringBuilder memberClassNames = new StringBuilder();
                StringBuilder memberClassInstances = new StringBuilder();

                Class<?>[] memberClasseses = memberClassesAnnotation.classes();
                String[] namesOfInstances = memberClassesAnnotation.namesOfInstances();

                if (memberClasseses.length != namesOfInstances.length) {
                    throw new BuildException("The number of classes and instance names do not match - @MemberClasses annotation in class "
                                             + actionClass.getName());
                }

                for (int i = 0; i < memberClasseses.length; i++) {

                    String memberClassPackageName = memberClasseses[i].getPackage().getName();
                    String memberClassSimpleName = memberClasseses[i].getSimpleName();
                    String memberClassInstanceName = namesOfInstances[i];

                    memberClassNames.append(memberClassSimpleName);
                    memberClassNames.append(".class, ");

                    memberClassInstances.append("\"");
                    memberClassInstances.append(memberClassInstanceName);
                    memberClassInstances.append("\", ");

                    // if the member class is from a packaged different than the
                    // currently processed action class we need to insert an import declaration 
                    if (!memberClassPackageName.equals(actionClass.getPackage().getName())) {
                        memberClassesImport.append("import ");
                        // we need to apply changes to the packages when user specify 
                        // originalSourcePackage different than originalTargetPackage
                        memberClassesImport.append(memberClassPackageName.replace(originalSourcePackage,
                                                                                  originalTargetPackage));
                        memberClassesImport.append(".");
                        memberClassesImport.append(memberClassSimpleName);
                        memberClassesImport.append(";" + LINE_SEPARATOR);
                    }

                    memberClassesDeclaration.append("    public ");
                    memberClassesDeclaration.append(memberClassSimpleName);
                    memberClassesDeclaration.append(" ");
                    memberClassesDeclaration.append(memberClassInstanceName);
                    memberClassesDeclaration.append(";" + LINE_SEPARATOR);

                    memberClassesNoArgsInitialization.append("        ");
                    memberClassesNoArgsInitialization.append(memberClassInstanceName);
                    memberClassesNoArgsInitialization.append(" = new ");
                    memberClassesNoArgsInitialization.append(memberClassSimpleName);
                    memberClassesNoArgsInitialization.append("();" + LINE_SEPARATOR);

                    memberClassesOneArgInitialization.append("        ");
                    memberClassesOneArgInitialization.append(memberClassInstanceName);
                    memberClassesOneArgInitialization.append(" = new ");
                    memberClassesOneArgInitialization.append(memberClassSimpleName);
                    memberClassesOneArgInitialization.append("( host );" + LINE_SEPARATOR);
                }

                //remove the trailing comma and space
                if (memberClasseses.length > 0) {
                    memberClassNames.delete(memberClassNames.length() - 2, memberClassNames.length());
                    memberClassInstances.delete(memberClassInstances.length() - 2,
                                                memberClassInstances.length());

                    classAnnotations.append("@MemberClasses( classes = { ");
                    classAnnotations.append(memberClassNames);
                    classAnnotations.append(" }, namesOfInstances = { ");
                    classAnnotations.append(memberClassInstances);
                    classAnnotations.append(" })");
                }

            }

            placeHolderValues.put("$RETURN_CLASS_IMPORTS$", getReturnClassImports(actionClass));
            placeHolderValues.put("$CLASS_ANNOTATIONS$", classAnnotations.toString());
            placeHolderValues.put("$MEMBER_CLASS_IMPORT$", memberClassesImport.toString());
            placeHolderValues.put("$MEMBER_CLASS_DECLARATION$", memberClassesDeclaration.toString());
            placeHolderValues.put("$MEMBER_CLASS_NO_ARG_INIT$", memberClassesNoArgsInitialization.toString());
            placeHolderValues.put("$MEMBER_CLASS_ONE_ARG_INIT$",
                                  memberClassesOneArgInitialization.toString());
            placeHolderValues.put("$PACKAGE$", targetPackage);
            placeHolderValues.put("$CLASS_NAME$", actionClass.getSimpleName());
            placeHolderValues.put("$INITIALIZE_REQUEST_URL$", initializeRequestUrl);
            placeHolderValues.put("$COMPONENT_NAME$", componentName);
            placeHolderValues.put("$METHOD_DEFINITIONS$", methodDefinitions);
            placeHolderValues.put("$PUBLIC_CONSTANTS$", publicConstants);

        } catch (Exception e) {
            throw new BuildException("Error building Agent action stub for action class '"
                                     + actionClass.getName() + "', component name '" + componentName
                                     + "'",
                                     e);
        }
    }

    private String getReturnClassImports(
                                          Class<?> actionClass ) {

        Method[] actionClassMethods = actionClass.getDeclaredMethods();
        //using TreeSet to prevent duplication of imports and have sorted list
        Set<String> imports = new TreeSet<String>();
        for (Method m : actionClassMethods) {
            if (ActionClassGenerator.isAnActionClass(m)) {

                // import method return type if needed
                Class<?> returnType = m.getReturnType();
                //check if the return type is an array
                if (returnType.getComponentType() != null) {
                    returnType = returnType.getComponentType();
                }
                if (needsToImportMethodReturnType(returnType)) {
                    addImport(imports, returnType, m.getGenericReturnType());
                }

                // import method parameter types if needed
                Class<?> methodParameterTypes[] = m.getParameterTypes();
                Type methodGenericParameterTypes[] = m.getGenericParameterTypes();
                for (int i = 0; i < methodParameterTypes.length; i++) {
                    Class<?> methodParameterType = methodParameterTypes[i];
                    if (needsToImportMethodParameterType(methodParameterType)) {
                        addImport(imports, methodParameterType, methodGenericParameterTypes[i]);
                    }
                }
            }
        }
        if (imports.size() > 0) {
            StringBuilder sb = new StringBuilder();
            for (String s : imports) {
                sb.append(s);
            }
            return sb.toString();
        } else {
            return "";
        }
    }

    private void addImport(
                            Set<String> imports,
                            Class<?> actualType,
                            Type genericType ) {

        if (genericType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) genericType;
            Type[] typeArguments = type.getActualTypeArguments();
            for (Type typeArg : typeArguments) {
                Class<?> typeArgClass = (Class<?>) typeArg;
                if (needsToImportMethodReturnType(typeArgClass)) {
                    imports.add(LINE_SEPARATOR + "import " + typeArgClass.getCanonicalName() + ";");
                }
            }
        }
        imports.add(LINE_SEPARATOR + "import " + actualType.getCanonicalName() + ";");
    }

    private boolean needsToImportMethodReturnType(
                                                   Class<?> clazz ) {

        return !"void".equals(clazz.getSimpleName()) && !clazz.isPrimitive() && clazz.getPackage() != null
               && !"java.lang".equals(clazz.getPackage().getName());
    }

    private boolean needsToImportMethodParameterType(
                                                      Class<?> clazz ) {

        return !clazz.isPrimitive() && clazz.getPackage() != null
               && !"java.lang".equals(clazz.getPackage().getName());
    }
}
