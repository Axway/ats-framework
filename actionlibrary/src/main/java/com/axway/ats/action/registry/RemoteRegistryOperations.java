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
package com.axway.ats.action.registry;

import com.axway.ats.agent.components.system.operations.clients.InternalRegistryOperations;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.registry.RegistryOperationsException;
import com.axway.ats.core.registry.model.IRegistryOperations;

public class RemoteRegistryOperations implements IRegistryOperations {

    private String                     atsAgent;

    private InternalRegistryOperations registryOperations;

    public RemoteRegistryOperations( String atsAgent ) throws AgentException {

        this.atsAgent = atsAgent;
        this.registryOperations = new InternalRegistryOperations(atsAgent);
    }

    @Override
    public boolean isKeyPresent(
                                 String rootKey,
                                 String keyPath,
                                 String keyName ) {

        try {
            return this.registryOperations.isKeyPresent(rootKey, keyPath, keyName);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could check for registry key existence on " + atsAgent, e);
        }
    }

    @Override
    public String getStringValue(
                                  String rootKey,
                                  String keyPath,
                                  String keyName ) {

        try {
            return this.registryOperations.getStringValue(rootKey, keyPath, keyName);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could get registry key from " + atsAgent, e);
        }
    }

    @Override
    public int getIntValue(
                            String rootKey,
                            String keyPath,
                            String keyName ) {

        try {
            return this.registryOperations.getIntValue(rootKey, keyPath, keyName);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could get registry key from " + atsAgent, e);
        }
    }

    @Override
    public long getLongValue(
                              String rootKey,
                              String keyPath,
                              String keyName ) {

        try {
            return this.registryOperations.getLongValue(rootKey, keyPath, keyName);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could get registry key from " + atsAgent, e);
        }
    }

    @Override
    public byte[] getBinaryValue(
                                  String rootKey,
                                  String keyPath,
                                  String keyName ) {

        try {
            return this.registryOperations.getBinaryValue(rootKey, keyPath, keyName);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could get registry key from " + atsAgent, e);
        }
    }

    @Override
    public void createPath(
                            String rootKey,
                            String keyPath ) {

        try {
            this.registryOperations.createPath(rootKey, keyPath);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could create registry path on " + atsAgent, e);
        }
    }

    @Override
    public void setStringValue(
                                String rootKey,
                                String keyPath,
                                String keyName,
                                String keyValue ) {

        try {
            this.registryOperations.setStringValue(rootKey, keyPath, keyName, keyValue);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could set registry key on " + atsAgent, e);
        }
    }

    @Override
    public void setIntValue(
                             String rootKey,
                             String keyPath,
                             String keyName,
                             int keyValue ) {

        try {
            this.registryOperations.setIntValue(rootKey, keyPath, keyName, keyValue);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could set registry key on " + atsAgent, e);
        }
    }

    @Override
    public void setLongValue(
                              String rootKey,
                              String keyPath,
                              String keyName,
                              long keyValue ) {

        try {
            this.registryOperations.setLongValue(rootKey, keyPath, keyName, keyValue);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could set registry key on " + atsAgent, e);
        }
    }

    @Override
    public void setBinaryValue(
                                String rootKey,
                                String keyPath,
                                String keyName,
                                byte[] keyValue ) {

        try {
            this.registryOperations.setBinaryValue(rootKey, keyPath, keyName, keyValue);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could set registry key on " + atsAgent, e);
        }
    }

    @Override
    public void deleteKey(
                           String rootKey,
                           String keyPath,
                           String keyName ) {

        try {
            this.registryOperations.deleteKey(rootKey, keyPath, keyName);
        } catch (AgentException e) {
            throw new RegistryOperationsException("Could delete registry key on " + atsAgent, e);
        }
    }
}
