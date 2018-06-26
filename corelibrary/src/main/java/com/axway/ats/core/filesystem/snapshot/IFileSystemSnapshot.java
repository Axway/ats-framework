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
package com.axway.ats.core.filesystem.snapshot;

public interface IFileSystemSnapshot {

    public IFileSystemSnapshot newSnapshot(
                                            String newSnapshotName );

    public void setName(
                         String newName );

    public void addDirectory(
                              String directoryAlias,
                              String directoryPath );

    public void skipDirectory(
                               String rootDirectoryAlias,
                               String relativeDirectoryPath );

    public void skipDirectoryByRegex(
                                      String rootDirectoryAlias,
                                      String relativeDirectoryPath );

    public void skipFile(
                          String rootDirectoryAlias,
                          String relativeFilePath,
                          int... skipRules );

    public void skipFileByRegex(
                                 String rootDirectoryAlias,
                                 String relativeFilePath,
                                 int... skipRules );

    public void checkFile(
                           String rootDirectoryAlias,
                           String relativeFilePath,
                           int... checkRules );

    public void skipPropertyWithKey( String rootDirectoryAlias, String relativeFilePath, String key,
                                     String matchType );

    public void skipPropertyWithValue( String rootDirectoryAlias, String relativeFilePath, String value,
                                       String matchType );

    public void skipNodeByValue( String rootDirectoryAlias, String relativeFilePath, String nodeXpath,
                                 String value, String matchType );

    public void skipNodeByAttribute( String rootDirectoryAlias, String relativeFilePath, String nodeXpath,
                                     String attributeKey, String attributeValue,
                                     String attributeValueMatchType );

    public void skipIniSection( String rootDirectoryAlias, String relativeFilePath, String section,
                                String matchType );

    public void skipIniPropertyWithKey( String rootDirectoryAlias, String relativeFilePath, String section,
                                        String propertyKey, String matchType );

    public void skipIniPropertyWithValue( String rootDirectoryAlias, String relativeFilePath, String section,
                                          String propertyValue, String matchType );

    public void skipTextLine( String rootDirectoryAlias, String relativeFilePath, String line,
                              String matchType );

    public void takeSnapshot();

    public void loadFromFile(
                              String sourceFile );

    public void toFile(
                        String backupFile );

    public String getDescription();

}
