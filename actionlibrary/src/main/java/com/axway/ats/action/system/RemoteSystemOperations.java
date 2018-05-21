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
package com.axway.ats.action.system;

import com.axway.ats.action.filesystem.RemoteFileSystemOperations;
import com.axway.ats.agent.components.system.operations.clients.InternalSystemOperations;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.system.OperatingSystemType;
import com.axway.ats.common.system.SystemOperationException;
import com.axway.ats.core.system.model.ISystemInputOperations;
import com.axway.ats.core.system.model.ISystemOperations;

public class RemoteSystemOperations implements ISystemOperations {

    private String                     atsAgent;

    private InternalSystemOperations   remoteSystemOperations;

    public RemoteSystemInputOperations inputOperatins;

    /**
     * Constructor
     *
     * @param atsAgent
     */
    public RemoteSystemOperations( String atsAgent ) throws AgentException {

        this.atsAgent = atsAgent;
        this.remoteSystemOperations = new InternalSystemOperations(atsAgent);
    }

    @Override
    public OperatingSystemType getOperatingSystemType() {

        try {
            return this.remoteSystemOperations.getOperatingSystemType();
        } catch (AgentException e) {
            throw new SystemOperationException("Could not get OS type of machine " + atsAgent, e);
        }
    }

    @Override
    public String getSystemProperty(
                                     String propertyName ) {

        try {
            return this.remoteSystemOperations.getSystemProperty(propertyName);
        } catch (AgentException e) {
            throw new SystemOperationException("Could not get the system property with name '" + propertyName
                                               + "' of machine " + atsAgent, e);
        }
    }

    @Override
    public String getTime(
                           boolean inMilliseconds ) {

        try {
            return this.remoteSystemOperations.getTime(inMilliseconds);
        } catch (AgentException e) {
            throw new SystemOperationException("Could not get system time of machine " + atsAgent, e);
        }
    }

    @Override
    public void setTime(
                         String timestamp,
                         boolean inMilliseconds ) {

        try {
            this.remoteSystemOperations.setTime(timestamp, inMilliseconds);
        } catch (AgentException e) {
            throw new SystemOperationException("Could not set system time of machine " + atsAgent, e);
        }
    }

    @Override
    public String getAtsVersion() {

        try {
            return this.remoteSystemOperations.getAtsVersion();
        } catch (AgentException e) {
            throw new SystemOperationException("Could not get ATS version of agent: " + atsAgent, e);
        }
    }

    @Override
    public boolean isListening(
                                String host,
                                int port,
                                int timeout ) {

        try {
            return this.remoteSystemOperations.isListening(host, port, timeout);
        } catch (AgentException e) {
            throw new SystemOperationException("Unable to check whether host '" + host
                                               + "' is listening on port '" + port + "' of agent: "
                                               + atsAgent, e);
        }
    }

    @Override
    public String createScreenshot(
                                    String filePath ) {

        try {
            String remoteFilePath = null;
            int extIndex = filePath.lastIndexOf('.');
            if (extIndex > 0) {
                remoteFilePath = filePath.substring(extIndex);
            }
            remoteFilePath = this.remoteSystemOperations.createScreenshot(remoteFilePath);
            new RemoteFileSystemOperations(atsAgent).copyFileFrom(remoteFilePath, filePath, true);
        } catch (AgentException e) {
            throw new SystemOperationException("Could not create screenshot on agent: " + atsAgent, e);
        }
        return filePath;
    }

    @Override
    public ISystemInputOperations getInputOperations() {

        if (inputOperatins == null) {
            inputOperatins = new RemoteSystemInputOperations();
        }
        return inputOperatins;
    }

    @Override
    public String getHostname() {

        try {
            return remoteSystemOperations.getHostname();
        } catch (AgentException e) {
            throw new SystemOperationException("Could not get Hostname on agent: " + atsAgent, e);
        }
    }

    @Override
    public String[] getClassPath() {

        try {
            return remoteSystemOperations.getClassPath();
        } catch (AgentException e) {
            throw new SystemOperationException("Could not get ClassPath on agent: " + atsAgent, e);
        }
    }

    @Override
    public void logClassPath() {

        try {
            remoteSystemOperations.logClassPath();
        } catch (AgentException e) {
            throw new SystemOperationException("Could not log classpath on agent: " + atsAgent, e);
        }
    }

    @Override
    public String[] getDuplicatedJars() {

        try {
            return remoteSystemOperations.getDuplicatedJars();
        } catch (AgentException e) {
            throw new SystemOperationException("Could not get duplicated jars on agent: " + atsAgent, e);
        }
    }

    @Override
    public void logDuplicatedJars() {

        try {
            remoteSystemOperations.logDuplicatedJars();
        } catch (AgentException e) {
            throw new SystemOperationException("Could not log duplicated jars on agent: " + atsAgent, e);
        }
    }

    public class RemoteSystemInputOperations implements ISystemInputOperations {

        @Override
        public void clickAt(
                             int x,
                             int y ) {

            try {
                remoteSystemOperations.inputOperations.clickAt(x, y);
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute mouse click action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void type(
                          String text ) {

            try {
                remoteSystemOperations.inputOperations.type(text);
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard type action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void type(
                          int... keyCodes ) {

            try {
                remoteSystemOperations.inputOperations.type(keyCodes);
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard type action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void type(
                          String text,
                          int... keyCodes ) {

            try {
                remoteSystemOperations.inputOperations.type(text, keyCodes);
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard type action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void pressTab() {

            try {
                remoteSystemOperations.inputOperations.pressTab();
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard TAB action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void pressSpace() {

            try {
                remoteSystemOperations.inputOperations.pressSpace();
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard SPACE action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void pressEnter() {

            try {
                remoteSystemOperations.inputOperations.pressEnter();
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard Enter action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void pressEsc() {

            try {
                remoteSystemOperations.inputOperations.pressEsc();
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard Escape action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void pressAltF4() {

            try {
                remoteSystemOperations.inputOperations.pressAltF4();
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard Alt + F4 action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void keyPress(
                              int keyCode ) {

            try {
                remoteSystemOperations.inputOperations.keyPress(keyCode);
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard key press action on agent: "
                                                   + atsAgent, e);
            }
        }

        @Override
        public void keyRelease(
                                int keyCode ) {

            try {
                remoteSystemOperations.inputOperations.keyRelease(keyCode);
            } catch (AgentException e) {
                throw new SystemOperationException("Could not execute keyboard key release action on agent: "
                                                   + atsAgent, e);
            }
        }

    }
}
