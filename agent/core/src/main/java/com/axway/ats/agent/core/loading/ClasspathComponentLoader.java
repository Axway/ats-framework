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
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import com.axway.ats.agent.core.ComponentRepository;
import com.axway.ats.agent.core.ConfigurationParser;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.core.utils.IoUtils;

public class ClasspathComponentLoader extends AbstractComponentLoader {

    private static final String IN_JAR_FILE_PREFIX_LOWERCASE = ".jar!";
    private static final int    IN_JAR_FILE_PREFIX_LENGTH    = IN_JAR_FILE_PREFIX_LOWERCASE.length();

    public ClasspathComponentLoader( Object loadingMutex ) {

        super( loadingMutex );
    }

    // TODO investigate warning for not closed resource (JarFile below) in Java 7
    @Override
    protected void loadComponents( ComponentRepository componentRepository ) throws AgentException {

        //clear the action map before registering
        componentRepository.clear();

        ClassLoader classLoader = ClasspathComponentLoader.class.getClassLoader();
        while( classLoader != null ) {
            URL[] urls = {};
            if( classLoader instanceof URLClassLoader ) {
                URLClassLoader urlClassLoader = ( URLClassLoader ) classLoader;
                // getURLs() does not work when resource is defined in Class-Path in MANIFEST.MF.
                // This is the case with Maven (surefire test runner)
                // obsolete: urls = urlClassLoader.getURLs();

                // TODO check here new code
                try {
                    Enumeration<URL> res = urlClassLoader.findResources( COMPONENT_DESCRIPTOR_NAME );
                    List<URL> urlList = new LinkedList<URL>();
                    while( res.hasMoreElements() ) {
                        URL urlMatching = res.nextElement();
                        log.debug( "Found Agent descriptor: " + urlMatching );
                        urlList.add( urlMatching );
                    }
                    urls = urlList.toArray( new URL[0] ); // use filtered path
                } catch( IOException ex ) {
                    // resource not closed warning with Java 7 should be ignored as
                    // class loader is not open here but SuppressWarnings is not
                    // added in order to hint for other possible problems
                    throw new AgentException( "Could not search for Agent descriptors. "
                                              + "Class path could not be enumerated", ex );
                } finally {
                    //                    IoUtils.closeStream( urlClassLoader );
                }
                // 2nd alternative is to use: urlClassLoader.getResources( COMPONENT_DESCRIPTOR_NAME )
            }

            String filePath, filePathLC;
            for( URL jarFileUrl : urls ) {
                filePath = jarFileUrl.getFile();
                filePathLC = filePath.toLowerCase();
                // Inherited: check only in JAR files
                if( filePathLC.endsWith( ".jar" ) || filePathLC.contains( IN_JAR_FILE_PREFIX_LOWERCASE ) ) {
                    if( filePathLC.contains( IN_JAR_FILE_PREFIX_LOWERCASE ) ) { // direct match
                        int afterJarExtensionIdx = filePathLC.indexOf( IN_JAR_FILE_PREFIX_LOWERCASE )
                                                   + IN_JAR_FILE_PREFIX_LENGTH - 1;
                        try {
                            jarFileUrl = new URL( filePath.substring( 0, afterJarExtensionIdx ) );
                        } catch( MalformedURLException ex ) {
                            throw new AgentException( "Could not parse JAR file name", ex );
                        }
                    }
                    try {
                        JarFile jarFile = null;

                        try {
                            try {
                                jarFile = new JarFile( new File( jarFileUrl.toURI() ) );
                            } catch( URISyntaxException e ) {
                                log.error( "Could not convert jar file URL to URI: '"
                                           + jarFileUrl.toExternalForm() + "'" );
                                continue;
                            }

                            JarEntry entry = jarFile.getJarEntry( COMPONENT_DESCRIPTOR_NAME );

                            if( entry != null ) {
                                log.info( "Found component library '" + jarFileUrl.getFile()
                                          + "', starting registration process" );

                                InputStream descriptorStream = jarFile.getInputStream( entry );
                                try {
                                    ConfigurationParser configParser = new ConfigurationParser();

                                    configParser.parse( descriptorStream,
                                                        ( new File( jarFileUrl.toURI() ) ).getAbsolutePath() );
                                    registerComponent( configParser, componentRepository );
                                } catch( Exception e ) {
                                    log.warn( "Exception throws while loading library '"
                                              + jarFileUrl.getFile() + "', skipping it", e );
                                    continue;
                                }

                            }
                        } finally {
                            IoUtils.closeStream( jarFile );
                            // TODO - jarFile.close() probably should not be invoked as streams are needed to classloader
                        }
                    } catch( IOException ioe ) {
                        log.warn( "Could not open library '" + jarFileUrl.getFile() + "', skipping it" );
                    }
                }
            }

            classLoader = classLoader.getParent();
        }
    }

    @Override
    protected Class<?> loadClass( String className ) throws ClassNotFoundException {

        return Class.forName( className );
    }
}
