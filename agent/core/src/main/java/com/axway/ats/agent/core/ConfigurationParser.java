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
package com.axway.ats.agent.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.common.dbaccess.OracleKeys;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.dbaccess.DatabaseProviderFactory;
import com.axway.ats.core.dbaccess.DbConnection;
import com.axway.ats.core.dbaccess.oracle.DbConnOracle;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.StringUtils;
import com.axway.ats.environment.AdditionalAction;
import com.axway.ats.environment.EnvironmentUnit;
import com.axway.ats.environment.database.DatabaseEnvironmentUnit;
import com.axway.ats.environment.database.model.DbTable;
import com.axway.ats.environment.file.DirectoryEnvironmentUnit;
import com.axway.ats.environment.file.FileEnvironmentUnit;
import com.axway.ats.environment.process.SystemProcessAction;

/**
 * A XML parser class for user configuring the Agent environment
 */
public class ConfigurationParser {

    private static final String        COMPONENT                       = "component";
    private static final String        ACTION_CLASS                    = "actionClass";
    private static final String        INITIALIZATION_HANDLER          = "initHandler";
    private static final String        FINALIZATION_HANDLER            = "finalHandler";
    private static final String        CLEANUP_HANDLER                 = "cleanupHandler";

    private static final String        ENVIRONMENT                     = "environment";
    private static final String        DATABASE                        = "database";
    private static final String        TABLE                           = "table";
    private static final String        FILE                            = "file";
    private static final String        DIRECTORY                       = "directory";

    private static final String        TABLE_ATTR_AUTOINCR_RESET_VALUE = "autoIncrementResetValue";

    /**
     * Hold the name of the component
     */
    private String                     componentName;

    /**
     * Hold a set of action class names
     */
    private Set<String>                actionClassNames;
    private String                     initializationHandler;
    private String                     finalizationHandler;
    private String                     cleanupHandler;

    private List<ComponentEnvironment> environments;

    /**
     * Temporary lists with environment names and backup folders to check for duplications
     */
    private List<String>               environmentNames                = new ArrayList<String>();
    private List<String>               backupFolders                   = new ArrayList<String>();

    /**
     * The logger for this class.
     */
    private static final Logger        log                             = LogManager.getLogger(ConfigurationParser.class);

    /**
     * The document supposed to be parsed.
     */
    private Document                   mDocument;

    /**
     * Initializer method of the parser. Initializes the document instance.
     * @param inputStream - configuration file input stream to be parsed
     * @param systemId Provide a base for resolving relative URIs.
     * @throws IOException - if the streamed object is not found
     */
    private void inititalizeParser( InputStream inputStream, String systemId ) throws IOException,
                                                                               SAXException,
                                                                               ParserConfigurationException {

        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setIgnoringElementContentWhitespace(true);
        documentBuilderFactory.setNamespaceAware(true);
        documentBuilderFactory.setValidating(false);
        documentBuilderFactory.setIgnoringComments(true);

        try {
            // skip DTD validation
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                                              false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd",
                                              false);

            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            mDocument = documentBuilder.parse(inputStream, systemId);

            /* NOTE:
             * XSD Validation process is performed after the XML parsing (not during), 
             * because when we do it during the parsing, the XML Document element adds attributes which has a default values in the XSD.
             * In our case for example, it adds lock="true" for all 'table' elements and when the database is oracle
             * we log WARN messages. It's wrong. That's why we do the validation after parsing the XML.
             */

            ConfigurationParser.class.getClassLoader().getResource("agent_descriptor.xsd");

            // XSD Validation
            SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            Schema schema = schemaFactory.newSchema(this.getClass()
                                                        .getClassLoader()
                                                        .getResource("agent_descriptor.xsd"));
            Validator validator = schema.newValidator();
            validator.validate(new DOMSource(mDocument));

        } catch (ParserConfigurationException pce) {
            log.error(pce.getMessage());
            throw pce;
        } catch (IOException ioe) {
            log.error(ioe.getMessage());
            throw ioe;
        } catch (SAXException saxe) {
            log.error(saxe.getMessage());
            throw saxe;
        }
    }

    /**
     * Init the component meta data - should be called before parsing is started
     */
    private void initializeComponentMetaData() {

        actionClassNames = new HashSet<String>();
        environments = new ArrayList<ComponentEnvironment>();
        initializationHandler = null;
        finalizationHandler = null;
        cleanupHandler = null;
    }

    /**
     * Parses the input stream from the CTF configuration file
     * and fills <code>registeredClasses</code> and <code>registeredListeners</code>
     * with classes found.
     * @param inputStream - the input stream
     * @param classLoader - the class loader for the current thread
     * @throws ClassNotFoundException - if class is not found
     */
    public void parse( InputStream inputStream, String systemID ) throws ClassNotFoundException, IOException,
                                                                  SAXException, ParserConfigurationException,
                                                                  AgentException {

        inititalizeParser(inputStream, systemID);
        initializeComponentMetaData();

        NodeList list = mDocument.getChildNodes();
        for (int i = 0; i < list.getLength(); i++) {
            Node childNode = list.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE
                && childNode.getNodeName().equals(COMPONENT)) {
                componentName = childNode.getAttributes().getNamedItem("name").getNodeValue();

                NodeList componentSubElementsList = childNode.getChildNodes();
                for (int k = 0; k < componentSubElementsList.getLength(); k++) {
                    Node componentSubElement = componentSubElementsList.item(k);

                    if (componentSubElement.getNodeName().equals(ACTION_CLASS)) {
                        String nameAttribute = componentSubElement.getAttributes()
                                                                  .getNamedItem("name")
                                                                  .getNodeValue();
                        actionClassNames.add(nameAttribute);
                    } else if (componentSubElement.getNodeName().equals(INITIALIZATION_HANDLER)) {
                        initializationHandler = componentSubElement.getAttributes()
                                                                   .getNamedItem("name")
                                                                   .getNodeValue();
                    } else if (componentSubElement.getNodeName().equals(FINALIZATION_HANDLER)) {
                        finalizationHandler = componentSubElement.getAttributes()
                                                                 .getNamedItem("name")
                                                                 .getNodeValue();
                    } else if (componentSubElement.getNodeName().equals(CLEANUP_HANDLER)) {
                        cleanupHandler = componentSubElement.getAttributes()
                                                            .getNamedItem("name")
                                                            .getNodeValue();
                    } else if (componentSubElement.getNodeName().equals(ENVIRONMENT)) {
                        parseEnvironment(componentSubElement);
                    }
                }
            }
        }
    }

    /**
     * Parse the environment node in the Agent descriptor
     *
     * @param environmentNode the environment node
     * @throws AgentException on error
     */
    private void parseEnvironment( Node environmentNode ) throws AgentException {

        NodeList environmentChildNodes = environmentNode.getChildNodes();

        //read the environment name
        String environmentName = parseEnvironmentName(environmentNode);

        //read the backup folder
        String backupFolder = parseBackupFolder(environmentNode);

        //now read the individual entries in the environment
        //these can be databases, files, processes, etc.
        List<EnvironmentUnit> environmentUnits = new ArrayList<EnvironmentUnit>();
        for (int i = 0; i < environmentChildNodes.getLength(); i++) {
            Node individualEnvironmentNode = environmentChildNodes.item(i);

            if (individualEnvironmentNode.getNodeName().equals(DATABASE)) {
                environmentUnits.add(parseDbEnvironment(individualEnvironmentNode, backupFolder));
            } else if (individualEnvironmentNode.getNodeName().equals(FILE)) {
                environmentUnits.add(parseFileEnvironment(individualEnvironmentNode, backupFolder));
            } else if (individualEnvironmentNode.getNodeName().equals(DIRECTORY)) {
                environmentUnits.add(parseDirectoryEnvironment(individualEnvironmentNode, backupFolder));
            }
        }

        environments.add(new ComponentEnvironment(componentName, environmentName, environmentUnits,
                                                  backupFolder));
    }

    /**
     *
     * @param environmentNode the environment node
     * @return backup folder (with the right slashes and slash at the end)
     * @throws AgentException if the backup folder is duplicated in other environments
     */
    private String parseBackupFolder( Node environmentNode ) throws AgentException {

        Node unixBackupFolderItem = environmentNode.getAttributes().getNamedItem("backupFolder");
        Node winBackupFolderItem = environmentNode.getAttributes().getNamedItem("windowsBackupFolder");
        String backupFolder = null;
        boolean isUnix = OperatingSystemType.getCurrentOsType().isUnix();

        if (isUnix) {
            if (unixBackupFolderItem != null) {
                String unixBackupFolder = unixBackupFolderItem.getNodeValue();
                if (unixBackupFolder != null && unixBackupFolder.trim().length() > 0) {

                    backupFolder = unixBackupFolder.trim();
                }
            }

        } else if (winBackupFolderItem != null) {

            String winBackupFolder = winBackupFolderItem.getNodeValue();
            if (winBackupFolder != null && winBackupFolder.trim().length() > 0) {

                backupFolder = winBackupFolder.trim();
            }
        }
        if (backupFolder == null) {

            String tempFolder = IoUtils.normalizeDirPath(AtsSystemProperties.SYSTEM_USER_TEMP_DIR);
            backupFolder = IoUtils.normalizeDirPath(tempFolder + "agent_components_backup");
            log.warn("No valid '" + (isUnix
                                            ? "backupFolder"
                                            : "windowsBackupFolder")
                     + "' environment attribute for '" + componentName + "' component. Backup folder '"
                     + backupFolder + "' will be used instead.");
        } else {
            backupFolder = IoUtils.normalizeDirPath(backupFolder);
        }

        if (backupFolders.contains(backupFolder)) {

            throw new AgentException("There is more than one environment configuration with the same backup folder '"
                                     + backupFolder + "'");
        }
        backupFolders.add(backupFolder);

        return backupFolder;
    }

    private String parseEnvironmentName( Node environmentNode ) throws AgentException {

        Node envNameItem = environmentNode.getAttributes().getNamedItem("name");

        String envName = null;
        if (envNameItem != null) {
            String name = envNameItem.getNodeValue();
            if (name != null && name.trim().length() > 0) {

                envName = name.trim();
            }
        }

        if ( (envName == null && environmentNames.size() > 0) || environmentNames.contains(null)) {

            throw new AgentException("More than one environment is defined. In such case you must specify environment name.");
        }
        if (environmentNames.contains(envName)) {

            throw new AgentException("There is more than one environment with name '" + envName + "'");
        }
        environmentNames.add(envName);

        return envName;
    }

    /**
     * Parse the database environment node in the Agent descriptor.
     *
     * @param dbEnvironmentNode the DB environment node.
     * @throws AgentException on error.
     */
    private EnvironmentUnit parseDbEnvironment( Node dbEnvironmentNode,
                                                String backupFolder ) throws AgentException {

        //create the connection descriptor
        DbConnection dbConnection = createConnection(dbEnvironmentNode.getAttributes());

        //read the tables
        List<DbTable> dbTables = new ArrayList<DbTable>();

        NodeList dbChildNodes = dbEnvironmentNode.getChildNodes();
        for (int k = 0; k < dbChildNodes.getLength(); k++) {
            Node dbChildNode = dbChildNodes.item(k);

            if (dbChildNode.getNodeName().equals(TABLE)) {
                String tableName = dbChildNode.getAttributes().getNamedItem("name").getNodeValue();
                String schemaName = new String();
                String[] tableNames = tableName.split("\\.");
                if(!StringUtils.isNullOrEmpty(tableName) && tableNames.length > 1) {
                    // Note that if the table name contains dot (.), even if the table name is escaped properly, according to the database server,
                    // we will consider the presence of dot as a sign that the table names is of the format <schema_name>.<table_name>
                    schemaName = tableNames[0];
                    tableName = tableNames[1];
                }

                String[] columnsToExclude = {};
                if (dbChildNode.getAttributes().getNamedItem("columnsToExclude") != null) {
                    columnsToExclude = dbChildNode.getAttributes()
                                                  .getNamedItem("columnsToExclude")
                                                  .getNodeValue()
                                                  .split(",");
                }

                DbTable dbTable = new DbTable(tableName, schemaName, Arrays.asList(columnsToExclude));
                // parse db table 'lock' attribute
                if (dbChildNode.getAttributes().getNamedItem("lock") != null) {

                    if (dbConnection.getDbType().equals(DbConnOracle.DATABASE_TYPE)) {
                        log.warn("Db table 'lock' attribute is NOT implemented for Oracle yet. "
                                 + "Table locking is skipped for the moment.");
                    }

                    String nodeValue = dbChildNode.getAttributes()
                                                  .getNamedItem("lock")
                                                  .getNodeValue()
                                                  .trim();
                    if ("false".equalsIgnoreCase(nodeValue) || "true".equalsIgnoreCase(nodeValue)) {
                        dbTable.setLockTable(Boolean.parseBoolean(nodeValue));
                    } else {
                        log.warn("Invalid db table 'lock' attribute value '" + nodeValue
                                 + "'. Valid values are 'true' and 'false'. The default value 'true' will be used.");
                    }
                } 
            	if (dbChildNode.getAttributes().getNamedItem("drop") != null) {

                    String nodeValue = dbChildNode.getAttributes()
                                                  .getNamedItem("drop")
                                                  .getNodeValue()
                                                  .trim();
                    if ("false".equalsIgnoreCase(nodeValue) || "true".equalsIgnoreCase(nodeValue)) {
                        dbTable.setDropTable(Boolean.parseBoolean(nodeValue));
                    } else {
                        log.warn("Invalid db table 'drop' attribute value '" + nodeValue
                                 + "'. Valid values are 'true' and 'false'. The default value 'false' will be used.");
                    }
                }

                // parse db table 'autoIncrementResetValue' attribute
                if (dbChildNode.getAttributes().getNamedItem(TABLE_ATTR_AUTOINCR_RESET_VALUE) != null) {
                    if (dbConnection.getDbType().equals(DbConnOracle.DATABASE_TYPE)) {

                        throw new AgentException("Db table 'autoIncrementResetValue' attribute is NOT implemented for Oracle yet.");
                    }
                    String autoInrcResetValue = dbChildNode.getAttributes()
                                                           .getNamedItem(TABLE_ATTR_AUTOINCR_RESET_VALUE)
                                                           .getNodeValue()
                                                           .trim();
                    try {
                        Integer.parseInt(autoInrcResetValue);
                    } catch (NumberFormatException nfe) {
                        throw new AgentException("Ivalid db table 'autoIncrementResetValue' attribute value '"
                                                 + autoInrcResetValue + "'. It must be valid number.");
                    }
                    dbTable.setAutoIncrementResetValue(autoInrcResetValue);
                }

                dbTables.add(dbTable);
            }
        }

        String backupFileName = componentName + "-" + dbConnection.getDb() + ".sql";

        //create the environment unit
        DatabaseEnvironmentUnit dbEnvironment = new DatabaseEnvironmentUnit(backupFolder, backupFileName,
                                                                            dbConnection, dbTables);
        return dbEnvironment;
    }

    /**
     * Parse the file environment node in the Agent descriptor.
     *
     * @param fileEnvironmentNode the file environment node.
     * @throws AgentException on error.
     */
    private EnvironmentUnit parseFileEnvironment( Node fileEnvironmentNode, String backupFolder ) {

        // get the original file
        String originalFile = fileEnvironmentNode.getAttributes().getNamedItem("path").getNodeValue();
        originalFile = IoUtils.normalizeUnixFile(originalFile);

        // get the backup file
        String backupName;
        if (fileEnvironmentNode.getAttributes().getNamedItem("backupName") != null) {
            backupName = fileEnvironmentNode.getAttributes().getNamedItem("backupName").getNodeValue();
        } else {
            backupName = IoUtils.getFileName(originalFile);
        }
        // get the optional addition actions to executed on file restore
        List<AdditionalAction> additionalActions = parseAdditionalActions(fileEnvironmentNode);

        FileEnvironmentUnit fileEnvironment = new FileEnvironmentUnit(originalFile, backupFolder,
                                                                      backupName);
        fileEnvironment.addAdditionalActions(additionalActions);

        return fileEnvironment;
    }

    /**
     * Parse the directory environment node in the Agent descriptor.
     *
     * @param directoryEnvironmentNode the directory environment node.
     * @throws AgentException on error.
     */
    private EnvironmentUnit parseDirectoryEnvironment( Node directoryEnvironmentNode, String backupFolder ) {

        String originalDir = directoryEnvironmentNode.getAttributes().getNamedItem("path").getNodeValue();
        originalDir = IoUtils.normalizeDirPath(originalDir);
        String backupName;

        if (directoryEnvironmentNode.getAttributes().getNamedItem("backupName") != null) {
            backupName = directoryEnvironmentNode.getAttributes().getNamedItem("backupName").getNodeValue();
        } else {
            // remove the last slash and get the directory name as a file name
            backupName = IoUtils.getFileName(originalDir.substring(0, originalDir.length() - 1));
        }

        // get the optional addition actions to executed on directory restore
        List<AdditionalAction> additionalActions = parseAdditionalActions(directoryEnvironmentNode);

        DirectoryEnvironmentUnit directoryEnvironment = new DirectoryEnvironmentUnit(originalDir,
                                                                                     backupFolder,
                                                                                     backupName);
        directoryEnvironment.addAdditionalActions(additionalActions);

        return directoryEnvironment;
    }

    /**
     * @param environmentNode
     * @return
     */
    private List<AdditionalAction> parseAdditionalActions( Node environmentNode ) {

        NodeList environmentNodeChildren = environmentNode.getChildNodes();
        List<AdditionalAction> actions = new ArrayList<AdditionalAction>();

        for (int i = 0; i < environmentNodeChildren.getLength(); i++) {
            Node environmentNodeChild = environmentNodeChildren.item(i);
            if (environmentNodeChild.getNodeName().equals("action")) {
                String shellCommand;
                int sleepInterval;
                Node shellCommandNode = environmentNodeChild.getAttributes().getNamedItem("command");
                Node sleepNode = environmentNodeChild.getAttributes().getNamedItem("sleep");
                if (shellCommandNode != null && sleepNode != null) {
                    shellCommand = shellCommandNode.getNodeValue();
                    sleepInterval = Integer.parseInt(sleepNode.getNodeValue().trim());

                    actions.add(new SystemProcessAction(shellCommand, sleepInterval));
                }
            }
        }
        return actions;
    }

    /**
     * Create a DB connection from an attributes map
     *
     * @param nodeMap the map of attributes
     * @return the db connection
     * @throws AgentException on error
     */
    private DbConnection createConnection( NamedNodeMap nodeMap ) throws AgentException {

        //the default host is localhost, if the attribute host is set
        //it will override the default setting
        String host = "127.0.0.1";
        if (nodeMap.getNamedItem("host") != null) {
            host = nodeMap.getNamedItem("host").getNodeValue();
        }
        String user = nodeMap.getNamedItem("user").getNodeValue();
        String password = nodeMap.getNamedItem("password").getNodeValue();
        String type = nodeMap.getNamedItem("type").getNodeValue();

        Map<String, Object> customProperties = new HashMap<String, Object>();
        if (nodeMap.getNamedItem("port") != null) {
            String portValue = nodeMap.getNamedItem("port").getNodeValue();
            try {
                Integer port = Integer.parseInt(portValue);
                customProperties.put(DbKeys.PORT_KEY, port);
            } catch (NumberFormatException nfe) {
                throw new AgentException("Cannot convert DB port number to integer: '" + portValue + "'");
            }
        }

        //get the database type
        String dbType = type.toUpperCase();

        if (dbType == null) {
            throw new AgentException("Database type " + type + " not supported");
        }

        // custom SID / Service Name (Oracle only)
        if (dbType.equals(DbConnOracle.DATABASE_TYPE)) {
            //SID - identifies the database instance (database name + instance number).
            // So if your database name is some db and your instance number is 3, then your SID is somedb3.
            if (nodeMap.getNamedItem("sid") != null) {
                customProperties.put(OracleKeys.SID_KEY, nodeMap.getNamedItem("sid").getNodeValue());
            }
            //Service Name - A "connector" to one or more instances. It is often useful to create additional
            //  service names in a RAC environment since the service can be modified to use particular SIDs as primary
            //  or secondary connections, or to not use certain SIDs at all.
            if (nodeMap.getNamedItem("serviceName") != null) {
                customProperties.put(OracleKeys.SERVICE_NAME_KEY,
                                     nodeMap.getNamedItem("serviceName").getNodeValue());
            }
        }

        String database;
        if (nodeMap.getNamedItem("name") != null) {

            database = nodeMap.getNamedItem("name").getNodeValue();
        } else if (dbType.equals(DbConnOracle.DATABASE_TYPE) && (nodeMap.getNamedItem("sid") != null
                                                                 || nodeMap.getNamedItem("serviceName") != null)) {

            database = "";
        } else {
            throw new AgentException("No DB Name" + (dbType.equals(DbConnOracle.DATABASE_TYPE)
                                                                                               ? "/SID/Service Name"
                                                                                               : "")
                                     + " is specified.");
        }

        return DatabaseProviderFactory.createDbConnection(dbType, host, -1, database, user, password,
                                                          customProperties);
    }

    public Set<String> getActionClassNames() {

        return actionClassNames;
    }

    public String getInitializationHandler() {

        return initializationHandler;
    }

    public String getFinalizationHandler() {

        return finalizationHandler;
    }

    public String getCleanupHandler() {

        return cleanupHandler;
    }

    public String getComponentName() {

        return componentName;
    }

    public List<ComponentEnvironment> getEnvironments() {

        return environments;
    }
}
