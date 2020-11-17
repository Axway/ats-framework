/*
 * Copyright 2017-2019 Axway Software
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
package com.axway.ats.agent.core.monitoring.systemmonitor;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.axway.ats.common.performance.monitor.beans.ReadingBean;
import com.axway.ats.core.monitoring.MonitorConfigurationException;
import com.axway.ats.core.monitoring.UnsupportedReadingException;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.XmlUtils;
import com.axway.ats.log.autodb.exceptions.DatabaseAccessException;

/**
 * The is responsible for reading all the properties from the configuration files and
 * creating the corresponding entities.
 * These entities can later on be used by the Monitoring Service to execute specific commands on the monitored
 * machine.
 */
public class ReadingsRepository {

    private Logger log = LogManager.getLogger(ReadingsRepository.class);

    /** We assign a unique id to each reading. 
     * This id is not the actual DB id
     */
    private int readingsUniqueId;

    private static final String MONITOR_NODE        = "monitor";
    private static final String MONITOR_NODE__CLASS = "class";

    private static final String READING_NODE          = "reading";
    private static final String READING_NODE__NAME    = "name";
    private static final String READING_NODE__UNIT    = "unit";
    private static final String READING_NODE__DYNAMIC = "dynamic";

    private static ReadingsRepository instance;

    private XmlReadingsRepository      xmlRepository;
    private DatabaseReadingsRepository dbRepository;

    private ParseReadingState readingParseState;

    // keeps track of each already loaded monitor and its custom.performance.configuration.xml file
    private Map<String, String> alreadyLoadedMonitors = new HashMap<String, String>();

    static {
        instance = new ReadingsRepository();
    }

    /**
     * @return the instance of the {@link ReadingsRepository}
     */
    public static final ReadingsRepository getInstance() {

        return instance;
    }

    public final ReadingBean getReadingXmlDefinition(
            String readingName,
            Map<String, String> parameters ) throws UnsupportedReadingException {

        ReadingBean reading = xmlRepository.getReadingDefinition(readingName);
        reading.setId(getNewUniqueId());
        reading.setParameters(parameters);
        return reading;
    }

    public final List<ReadingBean> getReadingXmlDefinitions(
            Set<String> readingNames ) throws UnsupportedReadingException {

        List<ReadingBean> readingBeans = new ArrayList<ReadingBean>();
        for (String readingName : readingNames) {
            ReadingBean reading = xmlRepository.getReadingDefinition(readingName);
            reading.setId(getNewUniqueId());
            readingBeans.add(reading);
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

        if (this.xmlRepository != null) {
            this.xmlRepository.cleanRepository();
        } else {
            this.xmlRepository = new XmlReadingsRepository();
            this.dbRepository = new DatabaseReadingsRepository();
        }

        this.readingsUniqueId = 0;
        this.readingParseState = new ParseReadingState();
    }

    public int getNewUniqueId() {

        return ++readingsUniqueId;
    }

    /**
     * Loads the user provided configuration files which ends up in updating the repository with supported readings.
     * URLs could be direct files, in-JAR-files, tmp/work dir extracted files like in WildFly VFS
     *
     * @param configurationFileUrls - list of URLs of configuration files
     * @throws MonitorConfigurationException
     */
    public void loadConfigurations(
            List<URL> configurationFileUrls ) throws MonitorConfigurationException {

        try {
            // empty the repository with readings definitions
            cleanRepository();
            // clear previously loaded monitors
            this.alreadyLoadedMonitors.clear();

            for (URL configurationFileUrl : configurationFileUrls) {
                InputStream configurationFileStream = null;
                try {
                    configurationFileStream = configurationFileUrl.openStream();
                } catch (IOException e) {
                    throw new MonitorConfigurationException("Error reading performance configuration data from '"
                                                            + configurationFileUrl + "'", e);
                }

                // fill the repository with readings definitions
                readConfiguration(configurationFileUrl, configurationFileStream);
            }
        } catch (MonitorConfigurationException e) {
            // on error empty the repository with readings definitions, so we know it is all clean
            cleanRepository();
            // on error clear previously loaded monitors
            this.alreadyLoadedMonitors.clear();
            throw e;
        }
    }

    private InputStream readFileFromJar(
            String jarFile ) throws MonitorConfigurationException {

        // extract the file path in the jar
        String configurationFile = jarFile.substring(jarFile.lastIndexOf('!') + 2, jarFile.length());
        log.info("Loading monitoring service configuration file '" + configurationFile + "' from "
                 + jarFile);
        try {
            return IoUtils.readFileFromJar(jarFile, configurationFile);
        } catch (IOException e) {
            throw new MonitorConfigurationException("Error loading monitoring service configuration file '"
                                                    + configurationFile + "' from " + jarFile);
        }
    }

    private InputStream readStandAloneFile(
            String configurationFile ) throws MonitorConfigurationException {

        log.info("Loading monitoring service configuration file '" + configurationFile + "'");
        try {
            return IoUtils.readFile(configurationFile);
        } catch (IOException e) {
            throw new MonitorConfigurationException("Error loading monitoring service configuration file '"
                                                    + configurationFile + "'");
        }
    }

    void readConfiguration(
            URL configurationFileNameURL,
            InputStream configurationFileStream ) throws MonitorConfigurationException {

        this.readingParseState.rememberConfigurationFileName(configurationFileNameURL.toString());

        Document xmlDocument;
        try {
            xmlDocument = XmlUtils.loadXMLFile(configurationFileStream);
        } catch (IOException e) {
            throw new MonitorConfigurationException("Error reading monitoring service configuration file from '"
                                                    + configurationFileNameURL + "'", e);
        } catch (SAXException e) {
            throw new MonitorConfigurationException("Error parsing monitoring service configuration file from '"
                                                    + configurationFileNameURL + "'", e);
        }

        Element rootElement = xmlDocument.getDocumentElement();

        // iterate over all children, all of them being monitors
        NodeList monitorLevelElements = rootElement.getChildNodes();
        for (int i = 0; i < monitorLevelElements.getLength(); i++) {
            Node monitorLevelElement = monitorLevelElements.item(i);
            if (monitorLevelElement.getNodeType() == Node.ELEMENT_NODE
                && monitorLevelElement.getNodeName().equals(MONITOR_NODE)) {
                extractMonitor((Element) monitorLevelElement);
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

        String monitorClass = extractAttribute(monitorLevelElement, MONITOR_NODE__CLASS);
        if (monitorClass == null) {
            this.readingParseState.throwException("No monitor class specified");
        }
        if (this.alreadyLoadedMonitors.containsKey(monitorClass)) {
            // the jar, that had monitor with the same name as the current one
            String alreadyProcessedJarFilename = extractJarFilepath(this.alreadyLoadedMonitors.get(monitorClass));
            // the currently processed jar, which contains custom performance monitor
            String currentJarFilename = extractJarFilepath(this.readingParseState.configurationFileName);
            String errMsg = "Duplicated monitor class name. Monitor '" + monitorClass
                            + "' is presented in both '" + alreadyProcessedJarFilename + "' and '"
                            + currentJarFilename + "'";
            throw new MonitorConfigurationException(errMsg);
        }
        this.readingParseState.rememberMonitorClass(monitorClass);

        // iterate over all children, all of them being filters
        NodeList readingLevelElements = monitorLevelElement.getChildNodes();
        for (int i = 0; i < readingLevelElements.getLength(); i++) {
            Node readingLevelElement = readingLevelElements.item(i);
            if (readingLevelElement.getNodeType() == Node.ELEMENT_NODE
                && readingLevelElement.getNodeName().equals(READING_NODE)) {
                extractReading((Element) readingLevelElement, monitorClass);
            }
        }

        // remember that this monitor's readings were already loaded 
        this.alreadyLoadedMonitors.put(this.readingParseState.monitorClass,
                                       this.readingParseState.configurationFileName);
        this.readingParseState.forgetMonitorClass();
    }

    private String extractJarFilepath( String configFilepath ) {

        return configFilepath.substring(configFilepath.indexOf(":") + 1,
                                        configFilepath.indexOf("!/"));
    }

    /**
     * Extracts all the data for a given monitor by iterating over it's filters
     *
     * @param readingLevelElement the root element of the tree
     * @throws MonitorConfigurationException
     */
    private void extractReading(
            Element readingLevelElement,
            String monitorClass ) throws MonitorConfigurationException {

        String readingName = extractAttribute(readingLevelElement, READING_NODE__NAME);
        if (readingName == null) {
            this.readingParseState.throwException("No reading name specified");
        }
        this.readingParseState.rememberReadingName(readingName);

        String readingUnit = extractAttribute(readingLevelElement, READING_NODE__UNIT);
        if (readingUnit == null) {
            this.readingParseState.throwException("No reading unit specified");
        }

        boolean dynamicReadingValue = false;
        String dynamicReading = extractAttribute(readingLevelElement, READING_NODE__DYNAMIC);
        if (dynamicReading != null && "true".equals(dynamicReading.trim())) {
            dynamicReadingValue = true;
        }

        ReadingBean readingBean = new ReadingBean(monitorClass, readingName, readingUnit);
        readingBean.setDynamicReading(dynamicReadingValue);
        this.xmlRepository.addReading(readingName, readingBean);

        // inform the user what we got
        log.debug(readingBean.getDescription());
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
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);

            String propertyName = attribute.getName();
            if (attribName.equals(propertyName)) {
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
            List<ReadingBean> readings ) throws DatabaseAccessException {

        this.dbRepository.updateDatabaseRepository(monitoredHost, readings);
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

            StringBuilder errorMessage = new StringBuilder(userMessage);
            if (configurationFileName != null) {
                errorMessage.append("; Configuration file: ").append(configurationFileName);
            }
            if (monitorClass != null) {
                errorMessage.append("; Monitor class: ").append(monitorClass);
            }
            if (readingName != null) {
                errorMessage.append("; Reading name: ").append(readingName);
            }

            throw new MonitorConfigurationException(errorMessage.toString());
        }
    }
}
