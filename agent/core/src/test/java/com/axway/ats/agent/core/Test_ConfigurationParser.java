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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.axway.ats.agent.core.ant.component.agenttest.CleanupHandler;
import com.axway.ats.agent.core.ant.component.agenttest.FinalHandler;
import com.axway.ats.agent.core.ant.component.agenttest.InitHandler;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.dbaccess.DbKeys;
import com.axway.ats.common.dbaccess.OracleKeys;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.environment.EnvironmentUnit;
import com.axway.ats.environment.database.DatabaseEnvironmentUnit;
import com.axway.ats.environment.file.DirectoryEnvironmentUnit;
import com.axway.ats.environment.file.FileEnvironmentUnit;
import com.axway.ats.environment.process.SystemProcessAction;

public class Test_ConfigurationParser extends BaseTest {

    private InputStream descriptorFileStream;
    private String      jarFileAbsolutePath;

    @Before
    public void setUp() throws Exception {

        URI jarFileURI = Test_ConfigurationParser.class.getResource( "/agenttest.jar" ).toURI();

        JarFile jarFile = new JarFile( new File( jarFileURI ) );
        JarEntry entry = jarFile.getJarEntry( "META-INF/agent_descriptor.xml" );

        descriptorFileStream = jarFile.getInputStream( entry );
        jarFileAbsolutePath = ( new File( jarFileURI ) ).getAbsolutePath();
    }

    @After
    public void testDown() throws Exception {

        descriptorFileStream.close();
    }

    @Test
    public void testBackupFolder_forUnix_ifIsMissingInTheXml() throws Exception {

        OperatingSystemType currentOs = OperatingSystemType.getCurrentOsType();
        simulateOS( OperatingSystemType.LINUX );
        try {

            InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                              .getResourceAsStream( "test_descriptors/test_agent_descriptor_windows_backup_folder.xml" );

            ConfigurationParser configParser = new ConfigurationParser();
            configParser.parse( _descriptorFileStream, jarFileAbsolutePath );

            assertEquals( 1, configParser.getEnvironments().size() );

            String backupFileName = getBackupFileName( ( FileEnvironmentUnit ) configParser.getEnvironments()
                                                                                           .get( 0 )
                                                                                           .getEnvironmentUnits()
                                                                                           .get( 0 ) );
            String tempDir = IoUtils.normalizeDirPath( AtsSystemProperties.SYSTEM_USER_TEMP_DIR )
                             + "agent_components_backup/";
            assertEquals( IoUtils.normalizeDirPath( tempDir ) + "backup_test.txt", backupFileName );
        } finally {
            simulateOS( currentOs );
        }
    }

    @Test
    public void testBackupFolder_forUnix() throws Exception {

        OperatingSystemType currentOs = OperatingSystemType.getCurrentOsType();
        simulateOS( OperatingSystemType.LINUX );
        try {
            InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                              .getResourceAsStream( "test_descriptors/test_agent_descriptor_unix_backup_folder.xml" );

            ConfigurationParser configParser = new ConfigurationParser();
            configParser.parse( _descriptorFileStream, jarFileAbsolutePath );

            assertEquals( 1, configParser.getEnvironments().get( 0 ).getEnvironmentUnits().size() );

            String backupFileName = getBackupFileName( ( FileEnvironmentUnit ) configParser.getEnvironments()
                                                                                           .get( 0 )
                                                                                           .getEnvironmentUnits()
                                                                                           .get( 0 ) );
            assertEquals( IoUtils.normalizeFilePath( "/var/backup/agent_backup/backup_test.txt" ),
                          backupFileName );
        } finally {
            simulateOS( currentOs );
        }
    }

    @Test
    public void testBackupFolder_forWindows_ifIsMissingInTheXml() throws Exception {

        OperatingSystemType currentOs = OperatingSystemType.getCurrentOsType();
        simulateOS( OperatingSystemType.WINDOWS );
        try {

            InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                              .getResourceAsStream( "test_descriptors/test_agent_descriptor_unix_backup_folder.xml" );

            ConfigurationParser configParser = new ConfigurationParser();
            configParser.parse( _descriptorFileStream, jarFileAbsolutePath );

            assertEquals( 1, configParser.getEnvironments().get( 0 ).getEnvironmentUnits().size() );

            String backupFileName = getBackupFileName( ( FileEnvironmentUnit ) configParser.getEnvironments()
                                                                                           .get( 0 )
                                                                                           .getEnvironmentUnits()
                                                                                           .get( 0 ) );

            String tempDir = IoUtils.normalizeDirPath( IoUtils.normalizeDirPath( AtsSystemProperties.SYSTEM_USER_TEMP_DIR )
                                                         + "agent_components_backup" );
            assertEquals( IoUtils.normalizeDirPath( tempDir ) + "backup_test.txt", backupFileName );
        } finally {
            simulateOS( currentOs );
        }
    }

    @Test
    public void testBackupFolder_forWindows() throws Exception {

        OperatingSystemType currentOs = OperatingSystemType.getCurrentOsType();
        simulateOS( OperatingSystemType.WINDOWS );
        try {

            InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                              .getResourceAsStream( "test_descriptors/test_agent_descriptor_windows_backup_folder.xml" );

            ConfigurationParser configParser = new ConfigurationParser();
            configParser.parse( _descriptorFileStream, jarFileAbsolutePath );

            String backupFileName = getBackupFileName( ( FileEnvironmentUnit ) configParser.getEnvironments()
                                                                                           .get( 0 )
                                                                                           .getEnvironmentUnits()
                                                                                           .get( 0 ) );
            assertEquals( IoUtils.normalizeFilePath( "C:/agent_backup_dir/backup_test.txt" ),
                          backupFileName );
        } finally {
            simulateOS( currentOs );
        }
    }

    @Test
    public void testBackupFolder_ifBackupFolderAttributesAreMissing() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor_no_backup_folders.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( _descriptorFileStream, jarFileAbsolutePath );

        assertEquals( 1, configParser.getEnvironments().get( 0 ).getEnvironmentUnits().size() );

        String backupFileName = getBackupFileName( ( FileEnvironmentUnit ) configParser.getEnvironments()
                                                                                       .get( 0 )
                                                                                       .getEnvironmentUnits()
                                                                                       .get( 0 ) );
        String tempDir = IoUtils.normalizeDirPath( IoUtils.normalizeDirPath( AtsSystemProperties.SYSTEM_USER_TEMP_DIR )
                                                     + "agent_components_backup" );
        assertEquals( IoUtils.normalizeDirPath( tempDir ) + "backup_test.txt", backupFileName );
    }

    @Test
    public void getActionClassName() throws Exception {

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( descriptorFileStream, jarFileAbsolutePath );

        assertEquals( 3, configParser.getActionClassNames().size() );
    }

    @Test
    public void getInitializationHandler() throws Exception {

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( descriptorFileStream, jarFileAbsolutePath );

        assertEquals( InitHandler.class.getName(), configParser.getInitializationHandler() );
    }

    @Test
    public void getFinalizationHandler() throws Exception {

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( descriptorFileStream, jarFileAbsolutePath );

        assertEquals( FinalHandler.class.getName(), configParser.getFinalizationHandler() );
    }

    @Test
    public void getCleanupHandler() throws Exception {

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( descriptorFileStream, jarFileAbsolutePath );

        assertEquals( CleanupHandler.class.getName(), configParser.getCleanupHandler() );
    }

    @Test
    public void getComponentName() throws Exception {

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( descriptorFileStream, jarFileAbsolutePath );

        assertEquals( "agenttest", configParser.getComponentName() );
    }

    @Test
    public void getEnvironmentUnitsNoPort() throws Exception {

        InputStream descriptorFileStream = Test_ConfigurationParser.class.getResourceAsStream( "descriptor_oracle.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( descriptorFileStream, jarFileAbsolutePath );

        List<EnvironmentUnit> environmentUnits = configParser.getEnvironments()
                                                             .get( 0 )
                                                             .getEnvironmentUnits();
        assertEquals( 3, environmentUnits.size() );
        assertEquals( DatabaseEnvironmentUnit.class, environmentUnits.get( 0 ).getClass() );
        assertEquals( FileEnvironmentUnit.class, environmentUnits.get( 1 ).getClass() );
        assertEquals( FileEnvironmentUnit.class, environmentUnits.get( 2 ).getClass() );
    }

    @Test
    public void getEnvironmentUnitsWithPort() throws Exception {

        InputStream descriptorFileStream = Test_ConfigurationParser.class.getResourceAsStream( "descriptor_with_port.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( descriptorFileStream, jarFileAbsolutePath );

        List<EnvironmentUnit> environmentUnits = configParser.getEnvironments()
                                                             .get( 0 )
                                                             .getEnvironmentUnits();
        assertEquals( 3, environmentUnits.size() );
        assertEquals( DatabaseEnvironmentUnit.class, environmentUnits.get( 0 ).getClass() );
        assertEquals( FileEnvironmentUnit.class, environmentUnits.get( 1 ).getClass() );
        assertEquals( FileEnvironmentUnit.class, environmentUnits.get( 2 ).getClass() );

        // check connection properties
        DatabaseEnvironmentUnit dbUnit = ( DatabaseEnvironmentUnit ) configParser.getEnvironments()
                                                                                 .get( 0 )
                                                                                 .getEnvironmentUnits()
                                                                                 .get( 0 );
        assertEquals( "127.127.127.127", dbUnit.getDbConnection().getHost() );
        assertEquals( "st", dbUnit.getDbConnection().getDb() );
        assertEquals( "root", dbUnit.getDbConnection().getUser() );
        assertEquals( "axway", dbUnit.getDbConnection().getPassword() );
        assertEquals( "mysql".toUpperCase(), dbUnit.getDbConnection().getDbType().toString() );

        Map<String, Object> customDbConnectionProperties = dbUnit.getDbConnection().getCustomProperties();
        assertEquals( 33060, customDbConnectionProperties.get( DbKeys.PORT_KEY ) );
    }

    @Test
    public void checkDbEnvironmentUnit_OracleSID() throws Exception {

        InputStream descriptorFileStream = Test_ConfigurationParser.class.getResourceAsStream( "descriptor_oracle.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( descriptorFileStream, jarFileAbsolutePath );

        assertEquals( DatabaseEnvironmentUnit.class,
                      configParser.getEnvironments().get( 0 ).getEnvironmentUnits().get( 0 ).getClass() );

        DatabaseEnvironmentUnit dbUnit = ( DatabaseEnvironmentUnit ) configParser.getEnvironments()
                                                                                 .get( 0 )
                                                                                 .getEnvironmentUnits()
                                                                                 .get( 0 );
        Map<String, Object> customDbConnectionProperties = dbUnit.getDbConnection().getCustomProperties();
        // check SID
        assertEquals( "some_sid", customDbConnectionProperties.get( OracleKeys.SID_KEY ) );

        // check that the URL is right (using the SID correctly)
        assertEquals( "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=127.0.0.1)(PORT=1521))(CONNECT_DATA=(SID=some_sid)))", dbUnit.getDbConnection().getURL() );
    }

    @Test
    public void checkDbEnvironmentUnit_OracleServiceName() throws Exception {

        InputStream descriptorFileStream = Test_ConfigurationParser.class.getResourceAsStream( "descriptor_oracle_with_service_name.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( descriptorFileStream, jarFileAbsolutePath );

        assertEquals( DatabaseEnvironmentUnit.class,
                      configParser.getEnvironments().get( 0 ).getEnvironmentUnits().get( 0 ).getClass() );

        DatabaseEnvironmentUnit dbUnit = ( DatabaseEnvironmentUnit ) configParser.getEnvironments()
                                                                                 .get( 0 )
                                                                                 .getEnvironmentUnits()
                                                                                 .get( 0 );
        Map<String, Object> customDbConnectionProperties = dbUnit.getDbConnection().getCustomProperties();

        // check the Service Name
        assertEquals( "some_service_name",
                      customDbConnectionProperties.get( OracleKeys.SERVICE_NAME_KEY ) );

        // check that the URL is right (using the service name correctly)
        assertEquals( "jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=TCP)(HOST=127.0.0.1)(PORT=1521))(CONNECT_DATA=(SERVICE_NAME=some_service_name)))",
                      dbUnit.getDbConnection().getURL() );
    }

    @Test
    public void testAdditionalActionOfFileEnvironmentUnit() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor_with_additional_actions.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( _descriptorFileStream, jarFileAbsolutePath );

        List<EnvironmentUnit> environmentUnits = configParser.getEnvironments()
                                                             .get( 0 )
                                                             .getEnvironmentUnits();
        assertEquals( 3, environmentUnits.size() );

        // check unit 1
        FileEnvironmentUnit fUnit1 = ( FileEnvironmentUnit ) environmentUnits.get( 0 );
        assertEquals( 0, fUnit1.getAdditionalActions().size() );

        // check unit 2
        FileEnvironmentUnit fUnit2 = ( FileEnvironmentUnit ) environmentUnits.get( 1 );
        assertEquals( 1, fUnit2.getAdditionalActions().size() );

        SystemProcessAction proc1 = ( SystemProcessAction ) fUnit2.getAdditionalActions().get( 0 );
        assertEquals( 3, proc1.getSleepInterval() );
        assertEquals( " shell command 'proc1'", proc1.getDescription() );

        // check unit 3
        FileEnvironmentUnit fUnit3 = ( FileEnvironmentUnit ) environmentUnits.get( 2 );
        assertEquals( 2, fUnit3.getAdditionalActions().size() );

        SystemProcessAction proc2 = ( SystemProcessAction ) fUnit3.getAdditionalActions().get( 0 );
        assertEquals( 5, proc2.getSleepInterval() );
        assertEquals( " shell command 'proc1'", proc2.getDescription() );

        SystemProcessAction proc3 = ( SystemProcessAction ) fUnit3.getAdditionalActions().get( 1 );
        assertEquals( 0, proc3.getSleepInterval() );
        assertEquals( " shell command 'proc2'", proc3.getDescription() );
    }

    @Test
    public void testParsingMoreEnvironments() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor_with_more_env.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( _descriptorFileStream, jarFileAbsolutePath );

        List<ComponentEnvironment> environments = configParser.getEnvironments();

        assertEquals( 2, environments.size() );

        assertEquals( "env1", environments.get( 0 ).getEnvironmentName() );
        assertEquals( "env2", environments.get( 1 ).getEnvironmentName() );

        assertEquals( 2, environments.get( 0 ).getEnvironmentUnits().size() );
        assertEquals( 1, environments.get( 1 ).getEnvironmentUnits().size() );

        // check unit 1 of env1
        FileEnvironmentUnit fUnit1 = ( FileEnvironmentUnit ) environments.get( 0 )
                                                                         .getEnvironmentUnits()
                                                                         .get( 0 );
        assertEquals( 0, fUnit1.getAdditionalActions().size() );

        // check unit 2 of env1
        FileEnvironmentUnit fUnit2 = ( FileEnvironmentUnit ) environments.get( 0 )
                                                                         .getEnvironmentUnits()
                                                                         .get( 1 );
        assertEquals( 1, fUnit2.getAdditionalActions().size() );

        SystemProcessAction proc1 = ( SystemProcessAction ) fUnit2.getAdditionalActions().get( 0 );
        assertEquals( 3, proc1.getSleepInterval() );
        assertEquals( " shell command 'proc1'", proc1.getDescription() );

        // check unit 1 of env2
        assertTrue( environments.get( 1 )
                                .getEnvironmentUnits()
                                .get( 0 ) instanceof DirectoryEnvironmentUnit );
    }

    @Test(expected = AgentException.class)
    public void testParsingMoreEnvironments_noEnvironmentName() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor_with_more_env_no_env_name.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( _descriptorFileStream, jarFileAbsolutePath );
    }

    @Test(expected = AgentException.class)
    public void testParsingMoreEnvironments_sameEnvironmentNames() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor_with_more_env_same_env_names.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( _descriptorFileStream, jarFileAbsolutePath );
    }

    @Test(expected = AgentException.class)
    public void testParsingMoreEnvironments_sameBackupFolder() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor_with_more_env_same_backup_folder.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( _descriptorFileStream, jarFileAbsolutePath );
    }

    @Test
    public void testBackupDirectory_withBackupName_forWindows() throws Exception {

        OperatingSystemType currentOs = OperatingSystemType.getCurrentOsType();
        simulateOS( OperatingSystemType.WINDOWS );
        try {
            InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                              .getResourceAsStream( "test_descriptors/test_agent_descriptor_windows_backup_folder.xml" );

            ConfigurationParser configParser = new ConfigurationParser();
            configParser.parse( _descriptorFileStream, jarFileAbsolutePath );

            String backupFileName = getBackupDirName( ( DirectoryEnvironmentUnit ) configParser.getEnvironments()
                                                                                               .get( 0 )
                                                                                               .getEnvironmentUnits()
                                                                                               .get( 1 ) );
            if( currentOs.isWindows() ) {
                assertEquals( IoUtils.normalizeDirPath( "C:/agent_backup_dir/backup dir/" ),
                              backupFileName );
            } else {
                // due to file canonicalization path is interpreted as relative and gets like this:
                // /home/user/workspace/.../C:/agent_backup_dir/original dir2/
                backupFileName.endsWith( IoUtils.normalizeDirPath( "C:/agent_backup_dir/backup dir/" ) );
            }
        } finally {
            simulateOS( currentOs );
        }
    }

    @Test
    public void testBackupDirectory_NoBackupName_forWindows() throws Exception {

        OperatingSystemType currentOs = OperatingSystemType.getCurrentOsType();
        simulateOS( OperatingSystemType.WINDOWS );
        try {
            InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                              .getResourceAsStream( "test_descriptors/test_agent_descriptor_windows_backup_folder.xml" );

            ConfigurationParser configParser = new ConfigurationParser();
            configParser.parse( _descriptorFileStream, jarFileAbsolutePath );

            String backupFileName = getBackupDirName( ( DirectoryEnvironmentUnit ) configParser.getEnvironments()
                                                                                               .get( 0 )
                                                                                               .getEnvironmentUnits()
                                                                                               .get( 2 ) );

            if( currentOs.isWindows() ) {
                assertEquals( IoUtils.normalizeDirPath( "C:/agent_backup_dir/original dir2/" ),
                              backupFileName );
            } else {
                // due to file canonicalization path is interpreted as relative and gets like this:
                // /home/user/workspace/.../C:/agent_backup_dir/original dir2/
                backupFileName.endsWith( IoUtils.normalizeDirPath( "C:/agent_backup_dir/original dir2/" ) );
            }
        } finally {
            simulateOS( currentOs );
        }
    }
    
    @Test
    public void testDB_dropTable() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( _descriptorFileStream, jarFileAbsolutePath );
    }

    @Test
    public void testDB_NoDbName_butServiceName() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor_no_database_name.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        configParser.parse( _descriptorFileStream, jarFileAbsolutePath );
    }

    @Test
    public void testDB_NoDbNameServiceNameAndSID_forOracle() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor_no_db_name_sid_sname.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        try {
            configParser.parse( _descriptorFileStream, jarFileAbsolutePath );
            fail();
        } catch( AgentException ae ) {
            assertEquals( "No DB Name/SID/Service Name is specified.", ae.getMessage() );
        }
    }

    @Test
    public void testDB_NoDbNameServiceNameAndSID_forMySQL() throws Exception {

        InputStream _descriptorFileStream = Test_ConfigurationParser.class.getClassLoader()
                                                                          .getResourceAsStream( "test_descriptors/test_agent_descriptor_no_db_name_sid_sname_mysql.xml" );

        ConfigurationParser configParser = new ConfigurationParser();
        try {
            configParser.parse( _descriptorFileStream, jarFileAbsolutePath );
            fail();
        } catch( AgentException ae ) {
            assertEquals( "No DB Name is specified.", ae.getMessage() );
        }
    }

    private String getBackupFileName( FileEnvironmentUnit fileEnv ) throws Exception {

        Field privateBackupFileNameField = FileEnvironmentUnit.class.getDeclaredField( "backupFileName" );
        privateBackupFileNameField.setAccessible( true );
        String backupFileName = ( String ) privateBackupFileNameField.get( fileEnv );
        privateBackupFileNameField.setAccessible( false );

        Field privateBackupDirPathField = FileEnvironmentUnit.class.getDeclaredField( "backupDirPath" );
        privateBackupDirPathField.setAccessible( true );
        String backupDirPath = ( String ) privateBackupDirPathField.get( fileEnv );
        privateBackupDirPathField.setAccessible( false );

        return IoUtils.normalizeFilePath( backupDirPath + backupFileName );
    }

    private String getBackupDirName( DirectoryEnvironmentUnit dirEnv ) throws Exception {

        Field privateBackupDirNameField = DirectoryEnvironmentUnit.class.getDeclaredField( "backupDirName" );
        privateBackupDirNameField.setAccessible( true );
        String backupDirName = ( String ) privateBackupDirNameField.get( dirEnv );
        privateBackupDirNameField.setAccessible( false );

        Field privateBackupDirPathField = DirectoryEnvironmentUnit.class.getDeclaredField( "backupDirPath" );
        privateBackupDirPathField.setAccessible( true );
        String backupDirPath = ( String ) privateBackupDirPathField.get( dirEnv );
        privateBackupDirPathField.setAccessible( false );

        return IoUtils.normalizeDirPath( backupDirPath + backupDirName );
    }

    /**
     *
     * @param ostype Operating System type
     * @throws Exception an error
     */
    private void simulateOS( OperatingSystemType ostype ) throws Exception {

        Field privateField = OperatingSystemType.class.getDeclaredField( "currentOs" );
        privateField.setAccessible( true );
        privateField.set( OperatingSystemType.getCurrentOsType(), ostype );
        privateField.setAccessible( false );
    }

}
