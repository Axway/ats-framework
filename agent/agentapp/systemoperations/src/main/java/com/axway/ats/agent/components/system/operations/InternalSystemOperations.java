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
package com.axway.ats.agent.components.system.operations;

import org.apache.logging.log4j.Level;

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.MemberClasses;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.system.LocalSystemOperations;

@MemberClasses( classes = { InternalSystemInputOperations.class }, namesOfInstances = { "inputOperations" })
public class InternalSystemOperations {

    private LocalSystemOperations localSystemOperations = new LocalSystemOperations();

    @Action( name = "Internal System Operations get Operating System Type")
    public OperatingSystemType getOperatingSystemType() {

        return localSystemOperations.getOperatingSystemType();
    }

    @Action( name = "Internal System Operations Get System Property")
    public String getSystemProperty(
                                     @Parameter( name = "propertyName") String propertyName ) throws AgentException {

        return localSystemOperations.getSystemProperty(propertyName);
    }

    @Action( name = "Internal System Operations get Time")
    public String getTime(
                           @Parameter( name = "inMilliseconds") boolean inMilliseconds ) {

        return localSystemOperations.getTime(inMilliseconds);
    }

    @Action( name = "Internal System Operations set Time")
    public void setTime(
                         @Parameter( name = "timestamp") String timestamp,
                         @Parameter( name = "inMilliseconds") boolean inMilliseconds ) {

        localSystemOperations.setTime(timestamp, inMilliseconds);
    }

    @Action( name = "Internal System Operations get Ats Version")
    public String getAtsVersion() {

        return localSystemOperations.getAtsVersion();
    }

    @Action( name = "Internal System Operations is Listening")
    public boolean isListening(
                                @Parameter( name = "host") String host,
                                @Parameter( name = "port") int port,
                                @Parameter( name = "timeout") int timeout ) {

        return localSystemOperations.isListening(host, port, timeout);
    }

    @Action( name = "Internal System Operations create Screenshot")
    public String createScreenshot(
                                    @Parameter( name = "filePath") String filePath ) {

        return localSystemOperations.createScreenshot(filePath);
    }

    @Action( name = "Internal System Operations get Hostname")
    public String getHostname() {

        return localSystemOperations.getHostname();
    }

    @Action( name = "Internal System Operations get Class Path")
    public String[] getClassPath() {

        return localSystemOperations.getClassPath();
    }

    @Action( name = "Internal System Operations log Class Path")
    public void logClassPath() {

        localSystemOperations.logClassPath();
    }

    @Action( name = "Internal System Operations get Duplicated Jars")
    public String[] getDuplicatedJars() {

        return localSystemOperations.getDuplicatedJars();
    }

    @Action( name = "Internal System Operations log Duplicated Jars")
    public void logDuplicatedJars() {

        localSystemOperations.logDuplicatedJars();
    }

    @Action( name = "Internal System Operations set Ats Db Appender Threshold")
    public void setAtsDbAppenderThreshold( @Parameter( name = "threshold") Level threshold ) {

        localSystemOperations.setAtsDbAppenderThreshold(threshold);
    }

    @Action( name = "Internal System Operations attach File Appender")
    public void attachFileAppender( @Parameter( name = "filepath") String filepath,
                                    @Parameter( name = "messageFormatPattern") String messageFormatPattern ) {

        localSystemOperations.attachFileAppender(filepath, messageFormatPattern);
    }
}
