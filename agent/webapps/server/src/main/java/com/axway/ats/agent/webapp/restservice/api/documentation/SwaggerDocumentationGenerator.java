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
package com.axway.ats.agent.webapp.restservice.api.documentation;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;

import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerClass;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethod;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinition;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodParameterDefinitions;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponse;
import com.axway.ats.agent.webapp.restservice.api.documentation.annotations.SwaggerMethodResponses;
import com.axway.ats.agent.webapp.restservice.api.documentation.exceptions.SwaggerDocumentationException;
import com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerDefinition;
import com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerDefinitionProperty;
import com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerDocument;
import com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerInfo;
import com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerInfoContact;
import com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerMethodParameter;
import com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerMethodPath;
import com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerMethodPathOperation;
import com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerTag;
import com.axway.ats.core.AtsVersion;
import com.axway.ats.core.utils.StringUtils;

public class SwaggerDocumentationGenerator {

    private static final String OUTPUT_FILE_NAME                              = "swagger.json";
    private static final String ATS_SWAGGER_DOCUMENTATION_PROPERTIES_FILENAME = "ats.swagger.documentation.properties";

    private String[]            classNames;
    private String              outputDirectory;

    public SwaggerDocumentationGenerator( String[] classNames, String outputDirectory ) {

        this.classNames = classNames;
        this.outputDirectory = outputDirectory;
    }

    public void generate() {

        try (PrintWriter pw = new PrintWriter(new File(this.outputDirectory + File.separator + OUTPUT_FILE_NAME))) {
            // load the properties file
            Properties documentationProperties = new Properties();
            documentationProperties.load(this.getClass()
                                             .getResourceAsStream(ATS_SWAGGER_DOCUMENTATION_PROPERTIES_FILENAME));
            // create the info object
            SwaggerInfo info = loadSwaggerInfo(documentationProperties);
            SwaggerDocument document = new SwaggerDocument();
            document.setInfo(info);
            document.setHost(documentationProperties.getProperty("host"));
            document.setBasePath(documentationProperties.getProperty("basePath"));
            // begin iteration over each class
            for (String className : classNames) {
                Class<?> clss = Class.forName(className);
                SwaggerClass swaggerClass = (SwaggerClass) clss.getAnnotation(SwaggerClass.class);
                if (swaggerClass == null) {
                    // the class has no proper Swagger documentation annotation, so skip it
                    continue;
                }
                System.out.println("\t Generating Swagger documentation for class '" + clss.getName() + "'");
                // add the current class's tag to the document
                document.addTag(new SwaggerTag(swaggerClass.value()));
                // iterate over each method
                for (Method method : clss.getMethods()) {
                    SwaggerMethod swaggerMethod = (SwaggerMethod) method.getAnnotation(SwaggerMethod.class);
                    if (swaggerMethod == null) {
                        // the method has no proper Swagger documentation annotation, so skip it
                        continue;
                    }
                    System.out.println("\t\t Generating Swagger documentation for method '" + method.getName() + "'");
                    // see if such path is already available
                    SwaggerMethodPath methodPath = null;
                    SwaggerMethodPathOperation methodPathOperation = new SwaggerMethodPathOperation();
                    if (document.hasPath(swaggerClass.value(), swaggerMethod.url())) {
                        // append new operation to an existing path
                        methodPath = document.getPath(swaggerClass.value(), swaggerMethod.url());
                    } else {
                        // create new path
                        methodPath = new SwaggerMethodPath();
                    }

                    methodPath.setClassUrl(swaggerClass.value());
                    methodPath.setUrl(swaggerMethod.url());
                    methodPathOperation.setHttpMethod(swaggerMethod.httpOperation().toLowerCase());
                    methodPathOperation.setTags(Arrays.asList(new String[]{ swaggerClass.value() }));
                    methodPathOperation.setSummary(swaggerMethod.summary());
                    methodPathOperation.setDescription(swaggerMethod.description());
                    methodPathOperation.setOperationId(swaggerMethod.url());
                    methodPathOperation.setConsumes(Arrays.asList(swaggerMethod.consumes()));
                    methodPathOperation.setProduces(Arrays.asList(swaggerMethod.produces()));

                    String parametersDefinition = swaggerMethod.parametersDefinition();
                    if (StringUtils.isNullOrEmpty(parametersDefinition)) {
                        // All of this method parameters are not part of a definition
                        // but instead must be added directly as query parameters to the methodPathOperation
                        // iterate over each method parameter
                        SwaggerMethodParameterDefinitions methodParameterDefinitions = method.getAnnotation(SwaggerMethodParameterDefinitions.class);
                        if (methodParameterDefinitions != null) {
                            for (SwaggerMethodParameterDefinition paramDef : methodParameterDefinitions.value()) {
                                SwaggerDefinitionProperty property = new SwaggerDefinitionProperty();
                                property.setName(paramDef.name());
                                property.setRequired(paramDef.required());
                                property.setType(paramDef.type());
                                property.setFormat(paramDef.format());
                                property.setExample(paramDef.example());
                                property.setDescription(paramDef.description());
                                methodPathOperation.addParameter(property.toQueryParameter());
                            }
                        }
                    } else {
                        // The parameters are provided via the Request's body
                        // They are part of a definition
                        SwaggerMethodParameter swaggerMethodParameter = new SwaggerMethodParameter();
                        swaggerMethodParameter.setDefinitionName(parametersDefinition);
                        swaggerMethodParameter.setDescription(parametersDefinition);
                        swaggerMethodParameter.setIn("body");
                        swaggerMethodParameter.setName("body");
                        methodPathOperation.addParameter(swaggerMethodParameter);

                        // create new definition, which will contains the JSON keys which must be passed in the request in order to call this method
                        SwaggerDefinition swaggerDefinition = new SwaggerDefinition();
                        swaggerDefinition.setName(parametersDefinition);

                        // iterate over each method parameter
                        SwaggerMethodParameterDefinitions methodParameterDefinitions = method.getAnnotation(SwaggerMethodParameterDefinitions.class);
                        if (methodParameterDefinitions != null) {
                            for (SwaggerMethodParameterDefinition paramDef : methodParameterDefinitions.value()) {
                                SwaggerDefinitionProperty property = new SwaggerDefinitionProperty();
                                property.setName(paramDef.name());
                                property.setRequired(paramDef.required());
                                property.setType(paramDef.type());
                                property.setFormat(paramDef.format());
                                property.setExample(paramDef.example());
                                property.setDescription(paramDef.description());
                                swaggerDefinition.addProperty(property);
                            }
                        }
                        document.addDefinition(swaggerDefinition);
                    }
                    methodPath.addOperation(methodPathOperation);
                    // add method responses definitions
                    SwaggerMethodResponses responses = method.getAnnotation(SwaggerMethodResponses.class);
                    if (responses != null) {
                        for (SwaggerMethodResponse response : responses.value()) {
                            // create SwaggerMethodResponse object
                            com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerMethodResponse methodResponse = new com.axway.ats.agent.webapp.restservice.api.documentation.model.SwaggerMethodResponse();
                            methodResponse.setCode(response.code());
                            methodResponse.setDescription(response.description());
                            methodResponse.setDefinitionLink(response.definition());
                            methodPathOperation.addResponse(methodResponse);

                            // create SwaggerDefinition for the response
                            SwaggerDefinition definition = new SwaggerDefinition();
                            definition.setName(response.definition());
                            // create the SwaggerDefinitionPproertie(s)
                            for (SwaggerMethodParameterDefinition responseParameterDef : response.parametersDefinitions()) {
                                SwaggerDefinitionProperty property = new SwaggerDefinitionProperty();
                                property.setDescription(responseParameterDef.description());
                                property.setExample(responseParameterDef.example());
                                property.setFormat(responseParameterDef.format());
                                property.setType(responseParameterDef.type());
                                property.setName(responseParameterDef.name());
                                definition.addProperty(property);
                            }
                            document.addDefinition(definition);
                        }
                    }
                    document.addPath(methodPath);
                }
            }
            pw.write(document.toJson());
            pw.flush();
        } catch (Exception e) {
            throw new SwaggerDocumentationException("Unable to generate Swagger documentation", e);
        }
    }

    private SwaggerInfo loadSwaggerInfo( Properties documentationProperties ) throws IOException {

        SwaggerInfo info = new SwaggerInfo();
        info.setVersion(AtsVersion.getAtsVersion());
        info.setDescription(documentationProperties.getProperty("info.description"));
        info.setTitle(documentationProperties.getProperty("info.title"));
        info.setContact(new SwaggerInfoContact(documentationProperties.getProperty("info.contact.name"),
                                               documentationProperties.getProperty("info.contact.url"),
                                               documentationProperties.getProperty("info.contact.email")));
        return info;
    }

    public static void main( String[] args ) {

        String[] classNames = new String[]{ "com.axway.ats.agent.webapp.restservice.api.actions.ActionsRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.agent.AgentPropertiesRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.agent.AgentConfigurationsRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.processes.executor.ProcessExecutorRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.processes.talker.ProcessTalkerRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.queues.QueuesRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.testcases.TestcasesRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.environments.EnvironmentsRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.filesystem.FileSystemRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.filesystem.snapshot.FileSystemSnapshotRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.system.SystemRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.system.input.SystemInputRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.registry.RegistryRestEntryPoint",
                                            "com.axway.ats.agent.webapp.restservice.api.machine.MachineDescriptionRestEntryPoint"
        };

        String swaggerDocLocation = System.getProperty("user.dir") + File.separator
                                    + "agent" + File.separator + "agentapp" + File.separator + "systemoperations"
                                    + File.separator + "target";
        new SwaggerDocumentationGenerator(classNames, swaggerDocLocation).generate();
        if (swaggerDocLocation.endsWith(File.separator)) {
            System.out.println("Swagger documentation saved as " + swaggerDocLocation + OUTPUT_FILE_NAME);
        } else {
            System.out.println("Swagger documentation saved as " + swaggerDocLocation + File.separator
                               + OUTPUT_FILE_NAME);
        }
    }

}
