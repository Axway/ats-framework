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

import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.ActionRequestInfo;
import com.axway.ats.agent.core.model.MemberClasses;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.core.system.LocalSystemOperations;

@MemberClasses(
        classes = { InternalSystemInputOperations.class },
        namesOfInstances = { "inputOperations" })
public class InternalSystemOperations {

    private LocalSystemOperations localSystemOperations = null;

    @Action(
            name = "Internal System Operations initialize")
    @ActionRequestInfo(
            requestMethod = "PUT",
            requestUrl = "system")
    public void initialize() {

        localSystemOperations = new LocalSystemOperations();
    }

    @Action(
            name = "Internal System Operations get Operating System Type")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "system/os")
    public OperatingSystemType getOperatingSystemType() {

        return localSystemOperations.getOperatingSystemType();
    }

    @Action(
            name = "Internal System Operations Get System Property")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "system/property")
    public String getSystemProperty(
                                     @Parameter(
                                             name = "propertyName") String propertyName ) throws AgentException {

        return localSystemOperations.getSystemProperty(propertyName);
    }

    @Action(
            name = "Internal System Operations get Time")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "system/time")
    public String getTime(
                           @Parameter(
                                   name = "inMilliseconds") boolean inMilliseconds ) {

        return localSystemOperations.getTime(inMilliseconds);
    }

    @Action(
            name = "Internal System Operations set Time")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/time")
    public void setTime(
                         @Parameter(
                                 name = "timestamp") String timestamp,
                         @Parameter(
                                 name = "inMilliseconds") boolean inMilliseconds ) {

        localSystemOperations.setTime(timestamp, inMilliseconds);
    }

    @Action(
            name = "Internal System Operations get Ats Version")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "system/atsVersion")
    public String getAtsVersion() {

        return localSystemOperations.getAtsVersion();
    }

    @Action(
            name = "Internal System Operations is Listening")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "system/listening")
    public boolean isListening(
                                @Parameter(
                                        name = "host") String host,
                                @Parameter(
                                        name = "port") int port,
                                @Parameter(
                                        name = "timeout") int timeout ) {

        return localSystemOperations.isListening(host, port, timeout);
    }

    @Action(
            name = "Internal System Operations create Screenshot")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/screenshot")
    public String createScreenshot(
                                    @Parameter(
                                            name = "filePath") String filePath ) {

        return localSystemOperations.createScreenshot(filePath);
    }

    @Action(
            name = "Internal System Operations get Hostname")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "system/hostname")
    public String getHostname() {

        return localSystemOperations.getHostname();
    }

    @Action(
            name = "Internal System Operations get Class Path")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "system/classpath")
    public String[] getClassPath() {

        return localSystemOperations.getClassPath();
    }

    @Action(
            name = "Internal System Operations log Class Path")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/classpath/log")
    public void logClassPath() {

        localSystemOperations.logClassPath();
    }

    @Action(
            name = "Internal System Operations get Duplicated Jars")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "system/classpath/duplicatedJars")
    public String[] getDuplicatedJars() {

        return localSystemOperations.getDuplicatedJars();
    }

    @Action(
            name = "Internal System Operations log Duplicated Jars")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/classpath/duplicatedJars/log")
    public void logDuplicatedJars() {

        localSystemOperations.logDuplicatedJars();
    }
}
