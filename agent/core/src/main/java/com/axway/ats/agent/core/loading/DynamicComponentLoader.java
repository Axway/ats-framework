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
package com.axway.ats.agent.core.loading;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.ConfigurationParser;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.core.utils.IoUtils;

public class DynamicComponentLoader extends AbstractComponentLoader {

    //variable to hold the time at which the components were last loaded
    private File                          componentsLocation;

    //instance to the class loader used for loading the action classes
    //we need to keep it as a member, so it can be properly garbage collected
    //after it is no longer used
    private ReversedDelegationClassLoader actionClassesClassLoader;

    public DynamicComponentLoader( File componentsLocation, Object loadingMutex ) {

        super(loadingMutex);
        this.componentsLocation = componentsLocation;
    }

    @Override
    protected void loadComponents( ComponentRepository componentRepository ) throws AgentException {

        if (componentsLocation == null || !componentsLocation.isDirectory()
            || !componentsLocation.exists()) {
            throw new AgentException("Components location not set, does not exist or is not a directory");
        }

        log.debug("Unloading any existing components");

        //clear the action map before registering
        componentRepository.clear();

        //init the class loader for loading the action classes
        ClassLoader currentClassLoader = DynamicComponentLoader.class.getClassLoader();
        actionClassesClassLoader = new ReversedDelegationClassLoader(new URL[]{}, currentClassLoader);

        File[] files = componentsLocation.listFiles();
        // java.io.File constructor accepts URIs where ClassLoader requires URLs
        List<URI> jarFileURIs = new ArrayList<URI>();

        if (files != null) {
            //get only the jar files
            for (File file : files) {
                if (file.getAbsolutePath().endsWith(".jar")) {
                    try {
                        URI jarFileURI = file.toURI(); // file.toURL() is deprecated and in case with spaces in directories there were errors

                        jarFileURIs.add(jarFileURI);

                        //add the jar file to our custom class loader
                        //it may not be an Agent component (i.e. no descriptor) but
                        //an Agent component might depend on it anyway
                        actionClassesClassLoader.addURL(jarFileURI.toURL());

                    } catch (MalformedURLException mue) {
                        log.warn("Malformed URL", mue);
                    }
                }
            }
        }

        for (URI uri : jarFileURIs) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(new File(uri));

                JarEntry entry = jarFile.getJarEntry(COMPONENT_DESCRIPTOR_NAME);

                if (entry != null) {
                    log.info("Found component library in '" + uri.toURL().getFile()
                             + "', starting registration process");

                    InputStream descriptorStream = jarFile.getInputStream(entry);
                    ConfigurationParser configParser = new ConfigurationParser();

                    try {
                        configParser.parse(descriptorStream, (new File(uri)).getAbsolutePath());
                        registerComponent(configParser, componentRepository);

                        log.info("Successfully registered component library with name '"
                                 + configParser.getComponentName() + "'");
                    } catch (Exception e) {
                        log.warn("Exception thrown while loading library '" + uri.toURL().getFile()
                                 + "', skipping it", e);
                        continue;
                    }
                }
            } catch (IOException ioe) {
                try {
                    log.warn("Could not open library '" + uri.toURL().getFile() + "', skipping it");
                } catch (MalformedURLException e) {
                    log.error(e);
                }
            } finally {
                IoUtils.closeStream(jarFile);
            }
        }
    }

    @Override
    protected Class<?> loadClass( String className ) throws ClassNotFoundException {

        return actionClassesClassLoader.loadClass(className);
    }

    public File getComponentsLocation() {

        return componentsLocation;
    }
}
