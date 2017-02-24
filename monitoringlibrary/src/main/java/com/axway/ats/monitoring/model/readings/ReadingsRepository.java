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
package com.axway.ats.monitoring.model.readings;

import java.io.IOException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.axway.ats.common.performance.monitor.beans.BasicReadingBean;
import com.axway.ats.common.performance.monitor.beans.FullReadingBean;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.XmlUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;
import com.axway.ats.monitoring.model.exceptions.MonitorConfigurationException;
import com.axway.ats.monitoring.model.exceptions.UnsupportedReadingException;

/**
 * The is responsible for reading all the properties from the configuration files and
 * creating the corresponding entities.
 * These entities can later on be used by the Monitoring Service to execute specific commands on the monitored
 * machine.
 */
public class ReadingsRepository {

    private Logger                     log                   = Logger.getLogger( ReadingsRepository.class );
    
    /** We assign a unique id to each reading. 
     * This id is passed all the time between Test Executor and Agent in order to identify the reading bean.
     * This id is not the actual DB id, the Test Executor maintains a mapping between "reading id <-> DB id"
     */
    private int                          readingsUniqueId;

    private static final String        MONITOR_NODE          = "monitor";
    private static final String        MONITOR_NODE__CLASS   = "class";

    private static final String        READING_NODE          = "reading";
    private static final String        READING_NODE__NAME    = "name";
    private static final String        READING_NODE__UNIT    = "unit";
    private static final String        READING_NODE__DYNAMIC = "dynamic";

    private static ReadingsRepository  instance;

    private XmlReadingsRepository      xmlRepository;
    private DatabaseReadingsRepository dbRepository;

    private Map<String, Integer>       readingIdToDbIdMap;

    private ParseReadingState          readingParseState;

    static {
        instance = new ReadingsRepository();
    }

    /**
     * @return the instance of the {@link ReadingsRepository}
     * @throws MonitorConfigurationException
     */
    public static final ReadingsRepository getInstance() {

        return instance;
    }

    public final FullReadingBean getReadingXmlDefinition(
                                                          String readingName,
                                                          Map<String, String> parameters )
                                                                                          throws UnsupportedReadingException {

        FullReadingBean reading = xmlRepository.getReadingDefinition( readingName );
        reading.setId( String.valueOf( getNewUniqueId() ) );
        reading.setParameters( parameters );
        return reading;
    }

    public final List<FullReadingBean> getReadingXmlDefinitions(
                                                                 Set<String> readingNames )
                                                                                           throws UnsupportedReadingException {

        List<FullReadingBean> readingBeans = new ArrayList<FullReadingBean>();
        for( String readingName : readingNames ) {
            FullReadingBean reading = xmlRepository.getReadingDefinition( readingName );
            reading.setId( String.valueOf( getNewUniqueId() ) );
            readingBeans.add( reading );
        }

        return readingBeans;
    }

    /**
     * Constructor
     */
    private ReadingsRepository() {

        cleanRepository();
    }

    public boolean isConfigured() {

        return this.xmlRepository.isConfigured();
    }

    public void cleanRepository() {

        if( this.xmlRepository != null ) {
            this.xmlRepository.cleanRepository();
        } else {
            this.xmlRepository = new XmlReadingsRepository();
            this.dbRepository = new DatabaseReadingsRepository();
            this.readingIdToDbIdMap = new HashMap<String, Integer>();
        }

        this.readingsUniqueId = 0;
        this.readingParseState = new ParseReadingState();
    }
    
    public int getNewUniqueId(){
        
        return ++readingsUniqueId;
    }
    
    /**
     * This reading is already populated to the DB, get its DB ID
     * @param readingId
     * @return
     */
    public Integer getReadingDbId(
                                   String readingId ) {

        return readingIdToDbIdMap.get( readingId );
    }

    /**
     * Loads the user provided configuration files which ends up in updating the repository with supported readings.
     *
     * @param configurationFiles
     * @throws MonitorConfigurationException
     */
    public void loadConfigurarions(
                                    List<String> configurationFiles ) throws MonitorConfigurationException {

        try {
            // empty the repository with readings definitions
            cleanRepository();

            for( String configurationFile : configurationFiles ) {
                InputStream configurationFileStream;
                if( configurationFile.contains( ".jar!" ) ) {
                    configurationFileStream = readFileFromJar( configurationFile );
                } else {
                    configurationFileStream = readStandAloneFile( configurationFile );
                }

                // fill the repository with readings definitions
                readConfiguration( configurationFile, configurationFileStream );
            }
        } catch( MonitorConfigurationException e ) {
            // on error empty the repository with readings definitions, so we know it is all clean
            cleanRepository();
            throw e;
        }
    }

    private InputStream readFileFromJar(
                                         String jarFile ) throws MonitorConfigurationException {

        // extract the file path in the jar
        String configurationFile = jarFile.substring( jarFile.lastIndexOf( '!' ) + 2, jarFile.length() );
        log.info( "Loading monitoring service configuration file '" + configurationFile + "' from " + jarFile );
        try {
            return IoUtils.readFileFromJar( jarFile, configurationFile );
        } catch( IOException e ) {
            throw new MonitorConfigurationException( "Error loading monitoring service configuration file '"
                                                     + configurationFile + "' from " + jarFile );
        }
    }

    private InputStream readStandAloneFile(
                                            String configurationFile ) throws MonitorConfigurationException {

        log.info( "Loading monitoring service configuration file '" + configurationFile + "'" );
        try {
            return IoUtils.readFile( configurationFile );
        } catch( IOException e ) {
            throw new MonitorConfigurationException( "Error loading monitoring service configuration file '"
                                                     + configurationFile + "'" );
        }
    }

    void readConfiguration(
                            String configurationFileName,
                            InputStream configurationFileStream ) throws MonitorConfigurationException {

        this.readingParseState.rememberConfigurationFileName( configurationFileName );

        Document xmlDocument;
        try {
            xmlDocument = XmlUtils.loadXMLFile( configurationFileStream );
        } catch( IOException e ) {
            throw new MonitorConfigurationException( "Error reading configuration file '"
                                                     + configurationFileName + "'", e );
        } catch( SAXException e ) {
            throw new MonitorConfigurationException( "Error parsing configuration file '"
                                                     + configurationFileName + "'", e );
        }

        Element rootElement = xmlDocument.getDocumentElement();

        // iterate over all children, all of them being monitors
        NodeList monitorLevelElements = rootElement.getChildNodes();
        for( int i = 0; i < monitorLevelElements.getLength(); i++ ) {
            Node monitorLevelElement = monitorLevelElements.item( i );
            if( monitorLevelElement.getNodeType() == Node.ELEMENT_NODE
                && monitorLevelElement.getNodeName().equals( MONITOR_NODE ) ) {
                extractMonitor( ( Element ) monitorLevelElement );
            }
        }

        this.readingParseState.forgetConfigurationFileName();
    }

    /**
     * Extracts all the data for a given monitor by iterating over it's filters
     *
     * @param monitorLevelElement the root element of the tree
     * @throws MonitorConfigurationException
     */
    private void extractMonitor(
                                 Element monitorLevelElement ) throws MonitorConfigurationException {

        String monitorClass = extractAttribute( monitorLevelElement, MONITOR_NODE__CLASS );
        if( monitorClass == null ) {
            this.readingParseState.throwException( "No monitor class specified" );
        }
        this.readingParseState.rememberMonitorClass( monitorClass );

        // iterate over all children, all of them being filters
        NodeList readingLevelElements = monitorLevelElement.getChildNodes();
        for( int i = 0; i < readingLevelElements.getLength(); i++ ) {
            Node readingLevelElement = readingLevelElements.item( i );
            if( readingLevelElement.getNodeType() == Node.ELEMENT_NODE
                && readingLevelElement.getNodeName().equals( READING_NODE ) ) {
                extractReading( ( Element ) readingLevelElement, monitorClass );
            }
        }

        this.readingParseState.forgetMonitorClass();
    }

    /**
     * Extracts all the data for a given monitor by iterating over it's filters
     *
     * @param element the root element of the tree
     * @throws MonitorConfigurationException
     */
    private void extractReading(
                                 Element readingLevelElement,
                                 String monitorClass ) throws MonitorConfigurationException {

        String readingName = extractAttribute( readingLevelElement, READING_NODE__NAME );
        if( readingName == null ) {
            this.readingParseState.throwException( "No reading name specified" );
        }
        this.readingParseState.rememberReadingName( readingName );

        String readingUnit = extractAttribute( readingLevelElement, READING_NODE__UNIT );
        if( readingUnit == null ) {
            this.readingParseState.throwException( "No reading unit specified" );
        }

        boolean dynamicReadingValue = false;
        String dynamicReading = extractAttribute( readingLevelElement, READING_NODE__DYNAMIC );
        if( dynamicReading != null && "true".equals( dynamicReading.trim() ) ) {
            dynamicReadingValue = true;
        }

        FullReadingBean readingBean = new FullReadingBean( monitorClass, readingName, readingUnit );
        readingBean.setDynamicReading( dynamicReadingValue );
        this.xmlRepository.addReading( readingName, readingBean );

        // inform the user what we got
        log.debug( readingBean.getDescription() );
        this.readingParseState.forgetReadingName();
    }

    /**
     * Search for a specific attribute in an element and return it's value
     *
     * @param attribName the name of the attribute
     * @param element the {@link Element} to search in
     * @return the value of the attribute
     */
    private String extractAttribute(
                                     Element element,
                                     String attribName ) {

        NamedNodeMap attributes = element.getAttributes();
        for( int i = 0; i < attributes.getLength(); i++ ) {
            Attr attribute = ( Attr ) attributes.item( i );

            String propertyName = attribute.getName();
            if( attribName.equals( propertyName ) ) {
                return attribute.getValue();
            }
        }
        return null;
    }

    /**
     * Populate these reading to the DB, so they have their own DB IDs
     *
     * @param monitoredHost
     * @param readings
     * @throws DatabaseAccessException
     */
    public void updateDatabaseRepository(
                                          String monitoredHost,
                                          List<BasicReadingBean> readings ) throws DatabaseAccessException {

        // we must be sure that FullReadingBean population is finished,
        // so no other thread can access a not populated BasicReadingBean
        synchronized( readingIdToDbIdMap ) {

            this.dbRepository.updateDatabaseRepository( monitoredHost, readings, readingIdToDbIdMap );
        }
    }

    /**
     * Some simple class to track down where we are in the
     * processed configuration file and give this info
     * to the user when an error occur.
     */
    public class ParseReadingState {

        private String configurationFileName;
        private String monitorClass;
        private String readingName;

        public void rememberConfigurationFileName(
                                                   String configurationFileName ) {

            this.configurationFileName = configurationFileName;
        }

        public void rememberMonitorClass(
                                          String monitorClass ) {

            this.monitorClass = monitorClass;
        }

        public void rememberReadingName(
                                         String readingName ) {

            this.readingName = readingName;
        }

        public void forgetConfigurationFileName() {

            this.configurationFileName = null;
        }

        public void forgetMonitorClass() {

            this.monitorClass = null;
        }

        public void forgetReadingName() {

            this.readingName = null;
        }

        public void throwException(
                                    String userMessage ) throws MonitorConfigurationException {

            StringBuilder errorMessage = new StringBuilder( userMessage );
            if( configurationFileName != null ) {
                errorMessage.append( "; Configuration file: " + configurationFileName );
            }
            if( monitorClass != null ) {
                errorMessage.append( "; Monitor class: " + monitorClass );
            }
            if( readingName != null ) {
                errorMessage.append( "; Reading name: " + readingName );
            }

            throw new MonitorConfigurationException( errorMessage.toString() );
        }
    }
}
