/*
 * Copyright 2018-2019 Axway Software
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
package com.axway.ats.examples.common;

import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.harness.config.CommonConfigurator;

/**
 * This class is a wrapper around the ATS Common Configurator.
 * This class provides well defined names of the different getter methods (like getServerIp() etc.),
 * while the ATS Common Configurator provides generic getter methods (like getString() etc.)
 */
public class AllInOneConfiguration {

    // Instance to the ATS common configurator.
    protected static CommonConfigurator configurator;
    private static AllInOneConfiguration instance;

    private AllInOneConfiguration() {

        // get an instance to the ATS common configurator
        configurator = CommonConfigurator.getInstance();

        // load all the data from a configuration file which is placed in the classpath
        configurator.registerConfigFileFromClasspath("configuration_data.properties");
    }

    public static AllInOneConfiguration getInstance() {

        if (instance == null) {
            instance = new AllInOneConfiguration();
        }

        return instance;
    }

    public String getServerIp() {

        return configurator.getProperty("server.ip");
    }

    public String getUserName() {

        return configurator.getProperty("user.name");
    }

    public String getUserPassword() {

        return configurator.getProperty("user.password");
    }

    public int getHttpServerPort() {

        return Integer.parseInt(configurator.getProperty("http.server.port"));
    }

    public String getHttpServerWebappWar() {

        return configurator.getProperty("http.server.webapp.war");
    }

    public int getFtpServerPort() {

        return Integer.parseInt(configurator.getProperty("ftp.server.port"));
    }

    public String getDatabaseHost() {

        return configurator.getProperty("db.host");
    }

    public String getDatabaseType() {

        return configurator.getProperty("db.type");
    }

    public String getDatabaseName() {

        return configurator.getProperty("db.name");
    }

    public String getBackupDatabaseName() {

        return configurator.getProperty("db.name.backup");
    }

    public String getDatabaseUser() {

        return configurator.getProperty("db.user");
    }

    public int getDbPort() {

        return Integer.parseInt(configurator.getProperty("db.port"));
    }

    public String getRootDir() {

        return IoUtils.normalizeDirPath(configurator.getProperty("root.dir"));
    }

    public String getResourcesRootDir() {

        return getRootDir() + IoUtils.normalizeDirPath(configurator.getProperty("resources.root"));
    }

    public String getDatabasePassword() {

        return configurator.getProperty("db.pass");
    }

    public String getProperty( String name ) {

        return configurator.getProperty(name);
    }

    public String getAgent1Address() {

        return configurator.getProperty("agent1.ip") + ":" + configurator.getProperty("agent1.port");
    }

    public String getAgent2Address() {

        return configurator.getProperty("agent2.ip") + ":" + configurator.getProperty("agent2.port");
    }
}
