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
package com.axway.ats.action.filesystem.snapshot;

import com.axway.ats.agent.components.system.operations.clients.InternalFileSystemSnapshot;
import com.axway.ats.agent.core.exceptions.AgentException;
import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.core.filesystem.snapshot.IFileSystemSnapshot;
import com.axway.ats.core.filesystem.snapshot.LocalFileSystemSnapshot;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;

public class RemoteFileSystemSnapshot implements IFileSystemSnapshot {

    private String                     atsAgent;

    private InternalFileSystemSnapshot remoteFSSnapshot;

    private SnapshotConfiguration      configuration;

    public RemoteFileSystemSnapshot( String atsAgent,
                                     String name,
                                     SnapshotConfiguration configuration ) throws AgentException {

        this.atsAgent = atsAgent;
        this.configuration = configuration;
        this.remoteFSSnapshot = new InternalFileSystemSnapshot(atsAgent);
        try {
            this.remoteFSSnapshot.initFileSystemSnapshot(name, this.configuration);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    public String getAtsAgent() {

        return this.atsAgent;
    }

    /**
     * @return a remote snapshot
     */
    public LocalFileSystemSnapshot getFileSystemSnapshot() {

        try {
            return remoteFSSnapshot.getFileSystemSnapshot();
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    /**
     * Replace a remote snapshot instance with a local instance 
     * @param newSnapshot the new snapshot
     */
    public void pushFileSystemSnapshot(
                                        LocalFileSystemSnapshot newSnapshot ) {

        try {
            remoteFSSnapshot.pushFileSystemSnapshot(newSnapshot);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    /**
     * Not exposed to our users. Used to set a snapshot name when we are replacing one snapshot with another
     */
    @Override
    public void setName(
                         String name ) {

        try {
            remoteFSSnapshot.setName(name);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    /**
     * Create a new instance of same snapshot
     */
    @Override
    public RemoteFileSystemSnapshot newSnapshot(
                                                 String newSnapshotName ) {

        RemoteFileSystemSnapshot newSnapshot = null;
        try {
            newSnapshot = new RemoteFileSystemSnapshot(this.atsAgent,
                                                       newSnapshotName,
                                                       configuration);
            newSnapshot.remoteFSSnapshot.newSnapshot(remoteFSSnapshot.getFileSystemSnapshot(), newSnapshotName);
            return newSnapshot;
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void addDirectory(
                              String directoryAlias,
                              String directoryPath ) {

        try {
            remoteFSSnapshot.addDirectory(directoryAlias, directoryPath);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipDirectory(
                               String rootDirectoryAlias,
                               String relativeDirectoryPath ) {

        try {
            remoteFSSnapshot.skipDirectory(rootDirectoryAlias, relativeDirectoryPath);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipDirectoryByRegex(
                                      String rootDirectoryAlias,
                                      String relativeDirectoryPath ) {

        try {
            remoteFSSnapshot.skipDirectoryByRegex(rootDirectoryAlias, relativeDirectoryPath);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipFile(
                          String rootDirectoryAlias,
                          String relativeFilePath,
                          int... skipRules ) {

        try {
            remoteFSSnapshot.skipFile(rootDirectoryAlias, relativeFilePath, skipRules);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipFileByRegex(
                                 String rootDirectoryAlias,
                                 String relativeFilePath,
                                 int... skipRules ) {

        try {
            remoteFSSnapshot.skipFileByRegex(rootDirectoryAlias, relativeFilePath, skipRules);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void checkFile(
                           String rootDirectoryAlias,
                           String relativeFilePath,
                           int... checkRules ) {

        try {
            remoteFSSnapshot.checkFile(rootDirectoryAlias, relativeFilePath, checkRules);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipPropertyWithKey( String rootDirectoryAlias, String relativeFilePath, String key,
                                     String matchType ) {

        try {
            remoteFSSnapshot.skipPropertyWithKey(rootDirectoryAlias, relativeFilePath, key,
                                                 matchType);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipPropertyWithValue( String rootDirectoryAlias, String relativeFilePath, String value,
                                       String matchType ) {

        try {
            remoteFSSnapshot.skipPropertyWithValue(rootDirectoryAlias, relativeFilePath, value,
                                                   matchType);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipNodeByValue( String rootDirectoryAlias, String relativeFilePath, String nodeXpath,
                                 String value, String matchType ) {

        try {
            remoteFSSnapshot.skipNodeByValue(rootDirectoryAlias, relativeFilePath, nodeXpath, value,
                                             matchType);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipNodeByAttribute( String rootDirectoryAlias, String relativeFilePath, String nodeXpath,
                                     String attributeKey, String attributeValue,
                                     String attributeValueMatchType ) {

        try {
            remoteFSSnapshot.skipNodeByAttribute(rootDirectoryAlias, relativeFilePath, nodeXpath,
                                                 attributeKey, attributeValue, attributeValueMatchType);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipIniSection( String rootDirectoryAlias, String relativeFilePath, String section,
                                String matchType ) {

        try {
            remoteFSSnapshot.skipIniSection(rootDirectoryAlias, relativeFilePath, section,
                                            matchType);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipIniPropertyWithKey( String rootDirectoryAlias, String relativeFilePath, String section,
                                        String key, String matchType ) {

        try {
            remoteFSSnapshot.skipIniPropertyWithKey(rootDirectoryAlias, relativeFilePath,
                                                    section, key, matchType);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipIniPropertyWithValue( String rootDirectoryAlias, String relativeFilePath, String section,
                                          String value, String matchType ) {

        try {
            remoteFSSnapshot.skipIniPropertyWithValue(rootDirectoryAlias, relativeFilePath,
                                                      section, value, matchType);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void skipTextLine( String rootDirectoryAlias, String relativeFilePath, String line,
                              String matchType ) {

        try {
            remoteFSSnapshot.skipTextLine(rootDirectoryAlias, relativeFilePath, line,
                                          matchType);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public void takeSnapshot() {

        try {
            remoteFSSnapshot.takeSnapshot();
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    public void compare(
                         RemoteFileSystemSnapshot that ) {

        //        try {
        //            remoteFSSnapshot.compare(internalId, that.internalId);
        //        } catch (AgentException e) {
        //            throw new FileSystemSnapshotException(e);
        //        }
    }

    @Override
    public void loadFromFile(
                              String sourceFile ) {

        try {
            remoteFSSnapshot.loadFromFile(sourceFile);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    /**
     * Save snapshot into a remote file
     */
    @Override
    public void toFile(
                        String backupFile ) {

        try {
            remoteFSSnapshot.toFile(backupFile);
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

    @Override
    public String getDescription() {

        try {
            return remoteFSSnapshot.getDescription();
        } catch (AgentException e) {
            throw new FileSystemSnapshotException(e);
        }
    }

}
