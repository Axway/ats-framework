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
package com.axway.ats.core.registry.model;

public interface IRegistryOperations {

    public boolean isKeyPresent(
                                 String rootKey,
                                 String keyPath,
                                 String keyName );

    public String getStringValue(
                                  String rootKey,
                                  String keyPath,
                                  String keyName );

    public int getIntValue(
                            String rootKey,
                            String keyPath,
                            String keyName );

    public long getLongValue(
                              String rootKey,
                              String keyPath,
                              String keyName );

    public byte[] getBinaryValue(
                                  String rootKey,
                                  String keyPath,
                                  String keyName );

    public void createPath(
                            String rootKey,
                            String keyPath );

    public void setStringValue(
                                String rootKey,
                                String keyPath,
                                String keyName,
                                String keyValue );

    public void setIntValue(
                             String rootKey,
                             String keyPath,
                             String keyName,
                             int keyValue );

    public void setLongValue(
                              String rootKey,
                              String keyPath,
                              String keyName,
                              long keyValue );

    public void setBinaryValue(
                                String rootKey,
                                String keyPath,
                                String keyName,
                                byte[] keyValue );

    public void deleteKey(
                           String rootKey,
                           String keyPath,
                           String keyName );
}
