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

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.ActionRequestInfo;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.core.registry.LocalRegistryOperations;

public class InternalRegistryOperations {

    private LocalRegistryOperations localRegistryOperations = null;

    @Action(
            name = "Internal Registry Operations initialize")
    @ActionRequestInfo(
            requestMethod = "PUT",
            requestUrl = "registry")
    public void initialize() {

        localRegistryOperations = new LocalRegistryOperations();
    }

    @Action(
            name = "Internal Registry Operations Is Key Present")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "registry/key")
    public boolean isKeyPresent(
                                 @Parameter(
                                         name = "rootKey") String rootKey,
                                 @Parameter(
                                         name = "keyPath") String keyPath,
                                 @Parameter(
                                         name = "keyName") String keyName ) {

        return localRegistryOperations.isKeyPresent(rootKey, keyPath, keyName);
    }

    @Action(
            name = "Internal Registry Operations Get String Value")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "registry/key/string")
    public String getStringValue(
                                  @Parameter(
                                          name = "rootKey") String rootKey,
                                  @Parameter(
                                          name = "keyPath") String keyPath,
                                  @Parameter(
                                          name = "keyName") String keyName ) {

        return localRegistryOperations.getStringValue(rootKey, keyPath, keyName);
    }

    @Action(
            name = "Internal Registry Operations Get Int Value")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "registry/key/int")
    public int getIntValue(
                            @Parameter(
                                    name = "rootKey") String rootKey,
                            @Parameter(
                                    name = "keyPath") String keyPath,
                            @Parameter(
                                    name = "keyName") String keyName ) {

        return localRegistryOperations.getIntValue(rootKey, keyPath, keyName);
    }

    @Action(
            name = "Internal Registry Operations Get Long Value")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "registry/key/long")
    public long getLongValue(
                              @Parameter(
                                      name = "rootKey") String rootKey,
                              @Parameter(
                                      name = "keyPath") String keyPath,
                              @Parameter(
                                      name = "keyName") String keyName ) {

        return localRegistryOperations.getLongValue(rootKey, keyPath, keyName);
    }

    @Action(
            name = "Internal Registry Operations Get Binary Value")
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "registry/key/binary")
    public byte[] getBinaryValue(
                                  @Parameter(
                                          name = "rootKey") String rootKey,
                                  @Parameter(
                                          name = "keyPath") String keyPath,
                                  @Parameter(
                                          name = "keyName") String keyName ) {

        return localRegistryOperations.getBinaryValue(rootKey, keyPath, keyName);
    }

    @Action(
            name = "Internal Registry Operations Create Path")
    @ActionRequestInfo(
            requestMethod = "PUT",
            requestUrl = "registry/path")
    public void createPath(
                            @Parameter(
                                    name = "rootKey") String rootKey,
                            @Parameter(
                                    name = "keyPath") String keyPath ) {

        localRegistryOperations.createPath(rootKey, keyPath);
    }

    @Action(
            name = "Internal Registry Operations Set String Value")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "registry/key/string")
    public void setStringValue(
                                @Parameter(
                                        name = "rootKey") String rootKey,
                                @Parameter(
                                        name = "keyPath") String keyPath,
                                @Parameter(
                                        name = "keyName") String keyName,
                                @Parameter(
                                        name = "keyValue") String keyValue ) {

        localRegistryOperations.setStringValue(rootKey, keyPath, keyName, keyValue);
    }

    @Action(
            name = "Internal Registry Operations Set Int Value")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "registry/key/int")
    public void setIntValue(
                             @Parameter(
                                     name = "rootKey") String rootKey,
                             @Parameter(
                                     name = "keyPath") String keyPath,
                             @Parameter(
                                     name = "keyName") String keyName,
                             @Parameter(
                                     name = "keyValue") int keyValue ) {

        localRegistryOperations.setIntValue(rootKey, keyPath, keyName, keyValue);
    }

    @Action(
            name = "Internal Registry Operations Set Long Value")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "registry/key/long")
    public void setLongValue(
                              @Parameter(
                                      name = "rootKey") String rootKey,
                              @Parameter(
                                      name = "keyPath") String keyPath,
                              @Parameter(
                                      name = "keyName") String keyName,
                              @Parameter(
                                      name = "keyValue") long keyValue ) {

        localRegistryOperations.setLongValue(rootKey, keyPath, keyName, keyValue);
    }

    @Action(
            name = "Internal Registry Operations Set Binary Value")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "registry/key/binary")
    public void setBinaryValue(
                                @Parameter(
                                        name = "rootKey") String rootKey,
                                @Parameter(
                                        name = "keyPath") String keyPath,
                                @Parameter(
                                        name = "keyName") String keyName,
                                @Parameter(
                                        name = "keyValue") byte[] keyValue ) {

        localRegistryOperations.setBinaryValue(rootKey, keyPath, keyName, keyValue);
    }

    @Action(
            name = "Internal Registry Operations Delete Key")
    @ActionRequestInfo(
            requestMethod = "DELETE",
            requestUrl = "registry/key")
    public void deleteKey(
                           @Parameter(
                                   name = "rootKey") String rootKey,
                           @Parameter(
                                   name = "keyPath") String keyPath,
                           @Parameter(
                                   name = "keyName") String keyName ) {

        localRegistryOperations.deleteKey(rootKey, keyPath, keyName);
    }
}
