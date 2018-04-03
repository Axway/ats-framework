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
package com.axway.ats.core.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;
import java.util.jar.Manifest;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * Utility class for inspecting the classpath
 */
public class ClasspathUtils {

    private static final Logger       log              = Logger.getLogger(ClasspathUtils.class);

    /**
     * A map containing all jars found in the classpath:
     * <jar simple name, <list with all detected instances of this jar> >
     */
    private Map<String, List<String>> loadedJarsMap    = new HashMap<String, List<String>>();

    /**
     * List of jars that are not supposed to be added by user manually
     * as this may affect the work of the framework in an unpredictable way.
     */
    private static List<String>       PROBLEMATIC_JARS = new ArrayList<String>();
    static {
        PROBLEMATIC_JARS.add("jaxws-api");
        PROBLEMATIC_JARS.add("jaxb-api");
    }

    /**
     * Log all jars found in the classpath.
     * If a jar is found more than once, then we list these one after another
     */
    public void logClassPath() {

        log.info("Classpath of JVM on " + HostUtils.getLocalHostIP() + ": \n" + getClassPathDescription());
    }

    /**
     * Return all jars found in the classpath.
     * If a jar is found more than once, then we list these one after another
     * 
     * @return StringBuilder with all jars loaded in the ClassPath
     */
    public StringBuilder getClassPathDescription() {

        // load the jars from the classpath
        StringBuilder classPathLog = new StringBuilder();
        String[] classpathArray = getClassPathArray();

        if (classpathArray.length == 0) {
            return classPathLog;
        }

        // Make a map containing folders as keys and jar names as values
        Map<String, List<String>> classPathMap = new HashMap<String, List<String>>();
        for (int i = 0; i < classpathArray.length; i++) {
            String jarFullPath = classpathArray[i].replace("\\", "/");
            String absPath = jarFullPath.substring(0, jarFullPath.lastIndexOf('/') + 1);
            String simpleJarName = jarFullPath.substring(jarFullPath.lastIndexOf('/') + 1,
                                                         jarFullPath.length());
            if (classPathMap.containsKey(absPath)) {
                classPathMap.get(absPath).add(simpleJarName);
            } else {
                classPathMap.put(absPath, new ArrayList<String>(Arrays.asList(simpleJarName)));
            }
        }

        // append instances of same jar one after another
        for (String path : new TreeSet<String>(classPathMap.keySet())) {
            classPathLog.append("\n");
            classPathLog.append(path);
            List<String> jarList = classPathMap.get(path);
            Collections.sort(jarList);
            for (String lib : jarList) {
                classPathLog.append("\n\t");
                classPathLog.append(lib);
            }
        }
        return classPathLog;
    }

    /**
     * @return return a list with all jars found in the classpath
     */
    public String[] getClassPathArray() {

        loadJarsFromClasspath();

        List<String> classPath = new ArrayList<String>();
        for (List<String> elements : loadedJarsMap.values()) {
            classPath.addAll(elements);
        }

        if (!classPath.isEmpty()) {
            return classPath.toArray(new String[classPath.size()]);
        } else {
            return new String[0];
        }
    }

    /**
     * @return list of jars that are found more than once in the classpath
     */
    public String[] getDuplicatedJars() {

        loadJarsFromClasspath();

        List<String> duplicatedJars = new ArrayList<String>();
        for (Entry<String, List<String>> loadedJarEntry : loadedJarsMap.entrySet()) {
            if (loadedJarEntry.getValue().size() > 1) {
                duplicatedJars.addAll(loadedJarEntry.getValue());
            }
        }
        if (!duplicatedJars.isEmpty()) {
            return duplicatedJars.toArray(new String[duplicatedJars.size()]);
        }
        return new String[0];
    }

    /**
     * Log in the console jars that are likely to cause issues running the tests:</br>
     *  - duplicated jars </br>
     *  - jars that are used by ATS and are not supposed to be added by the user manually
     */
    public void logProblematicJars() {

        loadJarsFromClasspath();

        StringBuilder logMessage = new StringBuilder();
        for (Entry<String, List<String>> loadedJarEntry : loadedJarsMap.entrySet()) {
            if (loadedJarEntry.getValue().size() > 1) {
                logMessage.append("\n ").append(loadedJarEntry.getKey()).append(":");
                for (String jarPath : loadedJarEntry.getValue()) {
                    logMessage.append("\n\t").append(jarPath);
                }
            }
            if ("log4j".equalsIgnoreCase(loadedJarEntry.getKey()) && loadedJarEntry.getValue().size() > 1) {
                String errorMsg = "Log4j library is supposed to be present only in lib_common subfolder. "
                                  + "Otherwise it should cause no logging into the agent log file. Currently Log4j is found at: "
                                  + loadedJarEntry.getValue();
                log.warn(errorMsg);
                log.warn(errorMsg);
            }
            if (PROBLEMATIC_JARS.contains(loadedJarEntry.getKey())) {
                String errorMsg = "The following libraries " + loadedJarEntry.getKey() + " located in "
                                  + loadedJarEntry.getValue()
                                  + ". These different jars can cause issues.";
                log.warn(errorMsg);
                log.warn(errorMsg);
            }
        }
        
        if( logMessage.length() > 0 ) {
            log.warn( "The following libraries seems to be present more than once in the classpath: "
                      + logMessage.toString() );
        }
    }

    /**
     * This method detects and add any JAR to loadedJarsMap
     */
    private void loadJarsFromClasspath() {

        loadedJarsMap.clear();

        ClassLoader classLoader = getClass().getClassLoader();
        URL[] urls = null;
        do {
            //check if the class loader is instance of URL and cast it
            if (classLoader instanceof URLClassLoader) {
                urls = ((URLClassLoader) classLoader).getURLs();
            } else {
                // if the ClassLoader is not instance of URLClassLoader we will break the cycle and log a message
                log.info("ClassLoader " + classLoader
                         + " is not instance of URLClassLoader, so it will skip it.");

                // if the ClassLoader is from JBoss, it is instance of BaseClassLoader,
                // we can take the ClassPath from a public method -> listResourceCache(), from JBoss-classloader.jar
                // this ClassLoader is empty, we will get the parent
                classLoader = classLoader.getParent();
                continue;
            }
            try {
                loadJarsFromManifestFile(classLoader);
            } catch (IOException ioe) {
                log.warn("MANIFEST.MF is loaded, so we will not search for duplicated jars!");
            }

            // add all jars from ClassPath to the map
            for (int i = 0; i < urls.length; i++) {
                addJarToMap(urls[i].getFile());
            }

            // get the parent classLoader
            classLoader = classLoader.getParent();
        } while (classLoader != null);

        if (loadedJarsMap.isEmpty()) {
            // jars are not found, so probably no URL ClassLoaders are found
            throw new RuntimeException("Most probrably specific server is used without URLClassLoader instances!");
        }
    }

    /**
    * Find and load all MANIFEST.MF files and get jars from the 'Class-Path' value
    */
    private void loadJarsFromManifestFile( ClassLoader classLoader ) throws IOException {

        Enumeration<URL> manifestUrls = ((URLClassLoader) classLoader).findResources("META-INF/MANIFEST.MF");
        Manifest manifest = null;
        URL manifestElement = null;

        if (manifestUrls != null) {
            while (manifestUrls.hasMoreElements()) {
                manifestElement = manifestUrls.nextElement();
                try (InputStream is = manifestElement.openStream()) {
                    manifest = new Manifest(is);

                    // get the 'Class-Path' value from the MANIFEST.MF file
                    String manifestClassPathValue = manifest.getMainAttributes().getValue("Class-Path");
                    if (manifestClassPathValue != null) {
                        log.trace("Parsing MANIFEST file \"" + manifestElement.getPath());
                        String[] arr = manifestClassPathValue.split(" ");
                        for (int i = 0; i < arr.length; i++) {
                            // add listed jars from MANIFEST file to the map
                            String jarSimpleName = getJarSimpleName(arr[i]);
                            String manifestFile = manifestElement.getFile();
                            manifestFile = manifestFile.replace("\\", "/");

                            if (manifestFile.startsWith("file:/")) {
                                manifestFile = manifestFile.substring("file:/".length());
                            }

                            manifestFile = manifestFile.substring(0,
                                                                  manifestFile.indexOf("!/META-INF/MANIFEST.MF"));
                            manifestFile = manifestFile.substring(0, manifestFile.lastIndexOf('/'));

                            if (!StringUtils.isNullOrEmpty(jarSimpleName)) {
                                String jarAbsolutePath = "";
                                if (arr[i].startsWith("file")) {
                                    jarAbsolutePath = arr[i].substring(6, arr[i].length());
                                } else {
                                    jarAbsolutePath = manifestFile + "/" + arr[i];
                                }
                                if (new File(jarAbsolutePath).exists()) {
                                    addJarToMap(jarAbsolutePath);
                                } else {
                                    log.trace("File \"" + jarAbsolutePath
                                              + "\" is defined in /META-INF/MANIFEST.MF \""
                                              + manifestElement.getPath() + "\", but does not exist!");
                                }
                            }
                        }
                    }
                } catch (IOException ioe) {
                    log.error("Unable to read the MANIFEST.MF file", ioe);
                }
            }
        }
    }

    private void addJarToMap( String jar ) {

        String jarSimpleName = getJarSimpleName(jar);
        String jarAbsolutePath = jar;
        if (jarAbsolutePath.startsWith("/")) {
            jarAbsolutePath = jarAbsolutePath.substring(1);
        }

        if (!StringUtils.isNullOrEmpty(jarSimpleName)) {
            if (!loadedJarsMap.containsKey(jarSimpleName)) {
                loadedJarsMap.put(jarSimpleName, new ArrayList<>(Arrays.asList(jarAbsolutePath)));
            } else {
                if (!loadedJarsMap.get(jarSimpleName).contains(jarAbsolutePath)) {
                    loadedJarsMap.get(jarSimpleName).add(jarAbsolutePath);
                }
            }
        } else {
            log.trace("Could not parse simple name for JAR \"" + jar + "\"");
        }
    }

    /**
     * Extract jar name without version and extension
     *
     * @param jarFullPath full jar file name with possible version and extension, e.g. jaxws-api-2.2.1.jar
     * @return jar name without version, e.g. jaxws-api
     */
    private String getJarSimpleName( String jarFullPath ) {

        String jarName = jarFullPath.substring(jarFullPath.lastIndexOf('/') + 1);
        boolean isSourceFile = false;

        String pattern = "(.+?)(-\\d)(.)"; // we get only jar name without version
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(jarName);

        if (!StringUtils.isNullOrEmpty(jarName)
            && jarName.substring(0, jarName.length() - 4).endsWith("sources")) {
            isSourceFile = true;
        }
        if (m.find()) {
            jarName = m.group(1);
        } else {
            if (jarName.endsWith(".jar")) {
                //here we will cut last 4 characters -'.jar'
                jarName = jarName.substring(0, jarName.length() - 4);
            }
        }
        if (isSourceFile)
            return jarName + "-sources";

        return jarName;
    }
}
