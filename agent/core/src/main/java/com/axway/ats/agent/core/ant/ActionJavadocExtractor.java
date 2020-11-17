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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;

public class ActionJavadocExtractor {

    private static final Logger     log                     = LogManager.getLogger(ActionJavadocExtractor.class);

    private static final String     javadocOneLineRegex     = "\\s*/\\*\\*.*\\*/\\s*";
    private static final String     javadocStartRegex       = "\\s*/\\*\\*.*";
    private static final String     javadocEndRegex         = ".*\\*/.*";
    private static final String     actionAnnotation        = "\\s*@Action.*name\\s*=\\s*\"(.*)\".*";

    private static final String     LINE_SEPARATOR          = AtsSystemProperties.SYSTEM_LINE_SEPARATOR;

    private static final String     AGENT_EXCEPTION_JAVADOC = "     * @throws AgentException  if an error occurs during action execution";

    private String                  sourceFolder;
    private HashMap<String, String> actionJavadocs          = new HashMap<String, String>();

    public ActionJavadocExtractor( String sourceFolder ) {

        this.sourceFolder = sourceFolder;
    }

    /**
     * Get the javadoc comments for all actions
     *
     * @return              a hash map where the action name is the key and the javadoc comment is the value
     * @throws IOException
     */
    public Map<String, String> extractJavaDocs() throws IOException {

        actionJavadocs = new HashMap<String, String>();

        List<String> javaFiles = getJavaFiles(new File(sourceFolder));
        for (String javaFile : javaFiles) {
            extractAtionJavaDoc(new File(javaFile));
        }

        return actionJavadocs;
    }

    /**
     * Get all .java files in a given folder (scan recursively)
     *
     * @param startFolder   the start folder
     * @return
     */
    private List<String> getJavaFiles(
                                       File startFolder ) {

        if (!startFolder.isDirectory()) {
            throw new RuntimeException(startFolder.getName() + " is not a directory ");
        }

        List<String> javaFiles = new ArrayList<String>();

        File[] children = startFolder.listFiles();
        if (children != null) {
            for (File child : children) {
                if (child.isDirectory()) {
                    javaFiles.addAll(getJavaFiles(child));
                } else {
                    String childName = child.getPath();

                    if (childName.endsWith(".java")) {
                        javaFiles.add(childName);
                    }
                }
            }
        }

        return javaFiles;
    }

    /**
     * Extract the javadoc information from a Java action class
     *
     * @param javaFile      the source file for the action class
     * @throws IOException
     */
    private void extractAtionJavaDoc(
                                      File javaFile ) throws IOException {

        Pattern actionPattern = Pattern.compile(actionAnnotation);
        BufferedReader javaReader = null;

        try {
            javaReader = new BufferedReader(new FileReader(javaFile));

            boolean inJavadoc = false;

            StringBuilder actionJavaDoc = new StringBuilder();
            String line = javaReader.readLine();
            while (line != null) {

                //empty line
                if (line.matches("\\s*")) {

                    line = javaReader.readLine();
                    continue;
                }

                //we found a 1 line long javadoc
                if (line.matches(javadocOneLineRegex)) {
                    actionJavaDoc.delete(0, actionJavaDoc.length());
                    actionJavaDoc.append(line.replace("*/", LINE_SEPARATOR + AGENT_EXCEPTION_JAVADOC
                                                            + LINE_SEPARATOR + "     */" + LINE_SEPARATOR));
                    line = javaReader.readLine();
                    continue;
                }

                //we found the start of a javadoc
                if (line.matches(javadocStartRegex)) {
                    inJavadoc = true;
                    actionJavaDoc.delete(0, actionJavaDoc.length());
                }

                //append the javadoc content
                if (inJavadoc) {

                    if (line.matches(javadocEndRegex)) {
                        //this is the end of the javadoc
                        actionJavaDoc.append(AGENT_EXCEPTION_JAVADOC + LINE_SEPARATOR);
                        actionJavaDoc.append(line + LINE_SEPARATOR);
                        inJavadoc = false;
                    } else {
                        //remove the @throws javadoc, because only AgentException is thrown
                        //in the client stubs
                        if (!line.contains("@throws")) {
                            actionJavaDoc.append(line + LINE_SEPARATOR);
                        }
                    }
                }

                //the javadoc ends
                if (line.matches(javadocEndRegex) && actionJavaDoc.length() > 0) {
                    actionJavaDoc.delete(actionJavaDoc.length() - 1, actionJavaDoc.length());
                    inJavadoc = false;
                } else {

                    //is this an action annotation
                    Matcher actionMatcher = actionPattern.matcher(line);
                    if (actionMatcher.matches() && actionJavaDoc.length() > 0) {

                        try {
                            //extract the name of the action
                            String actionName = actionMatcher.group(1);

                            log.info("Adding javadoc for action '" + actionName + "'");

                            actionJavadocs.put(actionName, actionJavaDoc.toString());

                            //clear the javadoc
                            actionJavaDoc.delete(0, actionJavaDoc.length());

                        } catch (IndexOutOfBoundsException iobe) {
                            log.warn("Action annotation found, but could not extract action name");
                        }
                    } else {
                        if (!line.matches("\\s*@.*") && inJavadoc == false && actionJavaDoc.length() > 0) {
                            //we are not in javadoc anymore, but found a method which is
                            //not annotated with the Action annotation so we need to remove this javadoc
                            actionJavaDoc.delete(0, actionJavaDoc.length());
                        }
                    }
                }

                line = javaReader.readLine();
            }
        } finally {
            IoUtils.closeStream(javaReader);
        }
    }
}
