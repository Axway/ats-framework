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
import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.core.filesystem.snapshot.LocalFileSystemSnapshot;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;

public class InternalFileSystemSnapshot {

    private LocalFileSystemSnapshot localFileSystemSnapshot = null;

    public InternalFileSystemSnapshot() {

    }

    @Action
    @ActionRequestInfo(
            requestMethod = "PUT",
            requestUrl = "filesystem/snapshot")
    public void initFileSystemSnapshot(
                                        @Parameter(
                                                name = "name") String name,
                                        @Parameter(
                                                name = "configuration") SnapshotConfiguration configuration ) {

        this.localFileSystemSnapshot = new LocalFileSystemSnapshot(name, configuration);
    }

    /**
     * Return a remote instance
     * @return {@link LocalFileSystemSnapshot}
     */
    @Action
    @ActionRequestInfo(
            requestMethod = "GET",
            requestUrl = "filesystem/snapshot")
    public LocalFileSystemSnapshot getFileSystemSnapshot() {

        return this.localFileSystemSnapshot;
    }

    /**
     * Replace a remote instance with a new one
     * @param newSnapshot
     * @return
     */
    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot")
    public void pushFileSystemSnapshot(

                                        @Parameter(
                                                name = "newSnapshot") LocalFileSystemSnapshot newSnapshot ) {

        this.localFileSystemSnapshot = newSnapshot;
    }

    /**
     * Create a new remote instance
     * @param srcFileSystemSnapshot the source file system snapshot from which a new one will be created
     * @param newSnapshotName
     * @return
     */
    @Action
    @ActionRequestInfo(
            requestMethod = "PUT",
            requestUrl = "filesystem/snapshot/new")
    public void newSnapshot(
                             @Parameter(
                                     name = "srcFileSystemSnapshot") LocalFileSystemSnapshot srcFileSystemSnapshot,
                             @Parameter(
                                     name = "newSnapshotName") String newSnapshotName ) {

        this.localFileSystemSnapshot = (LocalFileSystemSnapshot) srcFileSystemSnapshot.newSnapshot(newSnapshotName);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/name")
    public void setName(

                         @Parameter(
                                 name = "name") String name ) {

        this.localFileSystemSnapshot.setName(name);

    }

    @Action
    @ActionRequestInfo(
            requestMethod = "PUT",
            requestUrl = "filesystem/snapshot/directory/add")
    public void addDirectory(

                              @Parameter(
                                      name = "directoryAlias") String directoryAlias,
                              @Parameter(
                                      name = "directoryPath") String directoryPath ) {

        this.localFileSystemSnapshot.addDirectory(directoryAlias,
                                                  directoryPath);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/directory/skip")
    public void skipDirectory(

                               @Parameter(
                                       name = "rootDirectoryAlias") String rootDirectoryAlias,
                               @Parameter(
                                       name = "relativeDirectoryPath") String relativeDirectoryPath ) {

        this.localFileSystemSnapshot.skipDirectory(rootDirectoryAlias,
                                                   relativeDirectoryPath);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/directory/skip/byRegex")
    public void skipDirectoryByRegex(

                                      @Parameter(
                                              name = "rootDirectoryAlias") String rootDirectoryAlias,
                                      @Parameter(
                                              name = "relativeDirectoryPath") String relativeDirectoryPath ) {

        this.localFileSystemSnapshot.skipDirectoryByRegex(rootDirectoryAlias,
                                                          relativeDirectoryPath);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/skip")
    public void skipFile(

                          @Parameter(
                                  name = "rootDirectoryAlias") String rootDirectoryAlias,
                          @Parameter(
                                  name = "relativeFilePath") String relativeFilePath,
                          @Parameter(
                                  name = "skipRules") int... skipRules ) {

        this.localFileSystemSnapshot.skipFile(rootDirectoryAlias,
                                              relativeFilePath,
                                              skipRules);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/skip/byRegex")
    public void skipFileByRegex(

                                 @Parameter(
                                         name = "rootDirectoryAlias") String rootDirectoryAlias,
                                 @Parameter(
                                         name = "relativeFilePath") String relativeFilePath,
                                 @Parameter(
                                         name = "skipRules") int... skipRules ) {

        this.localFileSystemSnapshot.skipFileByRegex(rootDirectoryAlias,
                                                     relativeFilePath,
                                                     skipRules);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/check")
    public void checkFile(

                           @Parameter(
                                   name = "rootDirectoryAlias") String rootDirectoryAlias,
                           @Parameter(
                                   name = "relativeFilePath") String relativeFilePath,
                           @Parameter(
                                   name = "checkRules") int... checkRules ) {

        this.localFileSystemSnapshot.checkFile(rootDirectoryAlias,
                                               relativeFilePath,
                                               checkRules);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/property/skip/key")
    public void skipPropertyWithKey(
                                     @Parameter(
                                             name = "rootDirectoryAlias") String rootDirectoryAlias,
                                     @Parameter(
                                             name = "relativeFilePath") String relativeFilePath,
                                     @Parameter(
                                             name = "key") String key,
                                     @Parameter(
                                             name = "matchType") String matchType ) {

        this.localFileSystemSnapshot.skipPropertyWithKey(rootDirectoryAlias,
                                                         relativeFilePath,
                                                         key,
                                                         matchType);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/property/skip/value")
    public void skipPropertyWithValue(
                                       @Parameter(
                                               name = "rootDirectoryAlias") String rootDirectoryAlias,
                                       @Parameter(
                                               name = "relativeFilePath") String relativeFilePath,
                                       @Parameter(
                                               name = "value") String value,
                                       @Parameter(
                                               name = "matchType") String matchType ) {

        this.localFileSystemSnapshot.skipPropertyWithKey(rootDirectoryAlias,
                                                         relativeFilePath,
                                                         value,
                                                         matchType);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/xml/skip/node/value")
    public void skipNodeByValue(
                                 @Parameter(
                                         name = "rootDirectoryAlias") String rootDirectoryAlias,
                                 @Parameter(
                                         name = "relativeFilePath") String relativeFilePath,
                                 @Parameter(
                                         name = "nodeXpath") String nodeXpath,
                                 @Parameter(
                                         name = "value") String value,
                                 @Parameter(
                                         name = "matchType") String matchType ) {

        this.localFileSystemSnapshot.skipNodeByValue(rootDirectoryAlias,
                                                     relativeFilePath,
                                                     nodeXpath,
                                                     value,
                                                     matchType);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/xml/skip/node/attribute")
    public void
            skipNodeByAttribute(
                                 @Parameter(
                                         name = "rootDirectoryAlias") String rootDirectoryAlias,
                                 @Parameter(
                                         name = "relativeFilePath") String relativeFilePath,
                                 @Parameter(
                                         name = "nodeXpath") String nodeXpath,
                                 @Parameter(
                                         name = "attributeKey") String attributeKey,
                                 @Parameter(
                                         name = "attributeValue") String attributeValue,
                                 @Parameter(
                                         name = "attributeValueMatchType") String attributeValueMatchType ) {

        this.localFileSystemSnapshot.skipNodeByAttribute(rootDirectoryAlias,
                                                         relativeFilePath,
                                                         nodeXpath,
                                                         attributeKey,
                                                         attributeValue,
                                                         attributeValueMatchType);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/ini/skip/section")
    public void skipIniSection(
                                @Parameter(
                                        name = "rootDirectoryAlias") String rootDirectoryAlias,
                                @Parameter(
                                        name = "relativeFilePath") String relativeFilePath,
                                @Parameter(
                                        name = "section") String section,
                                @Parameter(
                                        name = "matchType") String matchType ) {

        this.localFileSystemSnapshot.skipIniSection(rootDirectoryAlias,
                                                    relativeFilePath,
                                                    section,
                                                    matchType);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/ini/skip/property/key")
    public void skipIniPropertyWithKey(
                                        @Parameter(
                                                name = "rootDirectoryAlias") String rootDirectoryAlias,
                                        @Parameter(
                                                name = "relativeFilePath") String relativeFilePath,
                                        @Parameter(
                                                name = "section") String section,
                                        @Parameter(
                                                name = "key") String key,
                                        @Parameter(
                                                name = "matchType") String matchType ) {

        this.localFileSystemSnapshot.skipIniPropertyWithKey(rootDirectoryAlias,
                                                            relativeFilePath,
                                                            section,
                                                            key,
                                                            matchType);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/ini/skip/property/value")
    public void skipIniPropertyWithValue(
                                          @Parameter(
                                                  name = "rootDirectoryAlias") String rootDirectoryAlias,
                                          @Parameter(
                                                  name = "relativeFilePath") String relativeFilePath,
                                          @Parameter(
                                                  name = "section") String section,
                                          @Parameter(
                                                  name = "value") String value,
                                          @Parameter(
                                                  name = "matchType") String matchType ) {

        this.localFileSystemSnapshot.skipIniPropertyWithValue(rootDirectoryAlias,
                                                              relativeFilePath,
                                                              section,
                                                              value,
                                                              matchType);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/file/skip/line")
    public void skipTextLine(
                              @Parameter(
                                      name = "rootDirectoryAlias") String rootDirectoryAlias,
                              @Parameter(
                                      name = "relativeFilePath") String relativeFilePath,
                              @Parameter(
                                      name = "line") String line,
                              @Parameter(
                                      name = "matchType") String matchType ) {

        this.localFileSystemSnapshot.skipTextLine(rootDirectoryAlias,
                                                  relativeFilePath,
                                                  line,
                                                  matchType);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/take")
    public void takeSnapshot() {

        this.localFileSystemSnapshot.takeSnapshot();
    }

    @Action
    @ActionRequestInfo(
                       requestMethod = "POST",
                       requestUrl = "filesystem/snapshot/compare")
    public void compare(
                         @Parameter(
                                 name = "thisLocalFileSystemSnapshot") LocalFileSystemSnapshot thisLocalFileSystemSnapshot,
                         @Parameter(
                                 name = "thatLocalFileSystemSnapshot") LocalFileSystemSnapshot thatLocalFileSystemSnapshot ) {

        thisLocalFileSystemSnapshot.compare(thatLocalFileSystemSnapshot);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/loadFromFile")
    public void loadFromFile(

                              @Parameter(
                                      name = "sourceFile") String sourceFile ) throws FileSystemSnapshotException {

        this.localFileSystemSnapshot.loadFromFile(sourceFile);
    }

    @Action
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "filesystem/snapshot/toFile")
    public void toFile(
                        @Parameter(
                                name = "backupFile") String backupFile ) {

        this.localFileSystemSnapshot.toFile(backupFile);
    }

    @Action
    @ActionRequestInfo(
                       requestMethod = "GET",
                       requestUrl = "filesystem/snapshot/description")
    public String getDescription() {

        return this.localFileSystemSnapshot.getDescription();

    }
}
