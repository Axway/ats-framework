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

import com.axway.ats.agent.core.action.CallerRelatedAction;
import com.axway.ats.agent.core.action.CallerRelatedInfoRepository;
import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.core.filesystem.snapshot.LocalFileSystemSnapshot;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;

public class InternalFileSystemSnapshot extends CallerRelatedAction {

    private static final String OBJECT_KEY_PREFIX = CallerRelatedInfoRepository.KEY_FILESYSTEM_SNAPSHOT;

    public InternalFileSystemSnapshot( String caller ) {

        super( caller );
    }

    @Action
    public String initFileSystemSnapshot(
                                          @Parameter(name = "name") String name,
                                          @Parameter(name = "configuration") SnapshotConfiguration configuration ) {

        // Add a new instance into the repository and return its unique counter 
        // which will be used in next calls
        return dataRepo.addObject( OBJECT_KEY_PREFIX, new LocalFileSystemSnapshot( name, configuration ) );
    }

    /**
     * Return a remote instance
     * 
     * @param internalProcessId
     * @return
     */
    @Action
    public LocalFileSystemSnapshot getFileSystemSnapshot(
                                                          @Parameter(name = "internalProcessId") String internalProcessId ) {

        return ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId );
    }

    /**
     * Replace a remote instance with a new one
     * 
     * @param internalProcessId
     * @param newSnapshot
     * @return
     */
    @Action
    public String pushFileSystemSnapshot(
                                          @Parameter(name = "internalProcessId") String internalProcessId,
                                          @Parameter(name = "newSnapshot") LocalFileSystemSnapshot newSnapshot ) {

        // clean up the old object
        dataRepo.removeObject( internalProcessId );

        // from now on we will use the new instance
        return dataRepo.addObject( OBJECT_KEY_PREFIX, newSnapshot );
    }

    /**
     * Create a new remote instance
     * 
     * @param internalProcessId
     * @param newSnapshotName
     * @return
     */
    @Action
    public String newSnapshot(
                               @Parameter(name = "internalProcessId") String internalProcessId,
                               @Parameter(name = "newSnapshotName") String newSnapshotName ) {

        return dataRepo.addObject( OBJECT_KEY_PREFIX,
                                   ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                                                     + internalProcessId ) ).newSnapshot( newSnapshotName ) );
    }

    @Action
    public void setName(
                         @Parameter(name = "internalProcessId") String internalProcessId,
                         @Parameter(name = "name") String name ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).setName( name );

    }

    @Action
    public void addDirectory(
                              @Parameter(name = "internalProcessId") String internalProcessId,
                              @Parameter(name = "directoryAlias") String directoryAlias,
                              @Parameter(name = "directoryPath") String directoryPath ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).addDirectory( directoryAlias,
                                                                                                                  directoryPath );
    }

    @Action
    public void skipDirectory(
                               @Parameter(name = "internalProcessId") String internalProcessId,
                               @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                               @Parameter(name = "relativeDirectoryPath") String relativeDirectoryPath) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).skipDirectory( rootDirectoryAlias,
                                                                                                                   relativeDirectoryPath );
    }
    
    @Action
    public void skipDirectoryByRegex(
                               @Parameter(name = "internalProcessId") String internalProcessId,
                               @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                               @Parameter(name = "relativeDirectoryPath") String relativeDirectoryPath ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).skipDirectoryByRegex( rootDirectoryAlias,
                                                                                                                   relativeDirectoryPath );
    }

    @Action
    public void skipFile(
                          @Parameter(name = "internalProcessId") String internalProcessId,
                          @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                          @Parameter(name = "relativeFilePath") String relativeFilePath,
                          @Parameter(name = "skipRules") int... skipRules ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).skipFile( rootDirectoryAlias,
                                                                                                              relativeFilePath,
                                                                                                              skipRules );
    }

    @Action
    public void skipFileByRegex(
                                 @Parameter(name = "internalProcessId") String internalProcessId,
                                 @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                                 @Parameter(name = "relativeFilePath") String relativeFilePath,
                                 @Parameter(name = "skipRules") int... skipRules ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).skipFileByRegex( rootDirectoryAlias,
                                                                                                                     relativeFilePath,
                                                                                                                     skipRules );
    }

    @Action
    public void checkFile(
                           @Parameter(name = "internalProcessId") String internalProcessId,
                           @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                           @Parameter(name = "relativeFilePath") String relativeFilePath,
                           @Parameter(name = "checkRules") int... checkRules ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).checkFile( rootDirectoryAlias,
                                                                                                               relativeFilePath,
                                                                                                               checkRules );
    }
    
    @Action
    public void skipPropertyWithKey( @Parameter(name = "internalProcessId") String internalProcessId,
                                     @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                                     @Parameter(name = "relativeFilePath") String relativeFilePath,
                                     @Parameter(name = "key") String key,
                                     @Parameter(name = "matchType") String matchType ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                          + internalProcessId ) ).skipPropertyWithKey( rootDirectoryAlias,
                                                                                                       relativeFilePath,
                                                                                                       key,
                                                                                                       matchType );
    }

    @Action
    public void skipPropertyWithValue( @Parameter(name = "internalProcessId") String internalProcessId,
                                       @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                                       @Parameter(name = "relativeFilePath") String relativeFilePath,
                                       @Parameter(name = "value") String value,
                                       @Parameter(name = "matchType") String matchType ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                          + internalProcessId ) ).skipPropertyWithKey( rootDirectoryAlias,
                                                                                                       relativeFilePath,
                                                                                                       value,
                                                                                                       matchType );
    } 

    @Action
    public void skipNodeByValue( @Parameter(name = "internalProcessId") String internalProcessId,
                                 @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                                 @Parameter(name = "relativeFilePath") String relativeFilePath,
                                 @Parameter(name = "nodeXpath") String nodeXpath,
                                 @Parameter(name = "value") String value,
                                 @Parameter(name = "matchType") String matchType ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                          + internalProcessId ) ).skipNodeByValue( rootDirectoryAlias,
                                                                                                   relativeFilePath,
                                                                                                   nodeXpath,
                                                                                                   value,
                                                                                                   matchType );
    }

    @Action
    public void
            skipNodeByAttribute( @Parameter(name = "internalProcessId") String internalProcessId,
                                 @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                                 @Parameter(name = "relativeFilePath") String relativeFilePath,
                                 @Parameter(name = "nodeXpath") String nodeXpath,
                                 @Parameter(name = "attributeKey") String attributeKey,
                                 @Parameter(name = "attributeValue") String attributeValue,
                                 @Parameter(name = "attributeValueMatchType") String attributeValueMatchType ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                          + internalProcessId ) ).skipNodeByAttribute( rootDirectoryAlias,
                                                                                                       relativeFilePath,
                                                                                                       nodeXpath,
                                                                                                       attributeKey,
                                                                                                       attributeValue,
                                                                                                       attributeValueMatchType );
    }

    @Action
    public void skipIniSection( @Parameter(name = "internalProcessId") String internalProcessId,
                                @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                                @Parameter(name = "relativeFilePath") String relativeFilePath,
                                @Parameter(name = "section") String section,
                                @Parameter(name = "matchType") String matchType ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                          + internalProcessId ) ).skipIniSection( rootDirectoryAlias,
                                                                                                  relativeFilePath,
                                                                                                  section,
                                                                                                  matchType );
    }

    @Action
    public void skipIniPropertyWithKey( @Parameter(name = "internalProcessId") String internalProcessId,
                                        @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                                        @Parameter(name = "relativeFilePath") String relativeFilePath,
                                        @Parameter(name = "section") String section,
                                        @Parameter(name = "key") String key,
                                        @Parameter(name = "matchType") String matchType ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                          + internalProcessId ) ).skipIniPropertyWithKey( rootDirectoryAlias,
                                                                                                          relativeFilePath,
                                                                                                          section,
                                                                                                          key,
                                                                                                          matchType );
    }

    @Action
    public void skipIniPropertyWithValue( @Parameter(name = "internalProcessId") String internalProcessId,
                                          @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                                          @Parameter(name = "relativeFilePath") String relativeFilePath,
                                          @Parameter(name = "section") String section,
                                          @Parameter(name = "value") String value,
                                          @Parameter(name = "matchType") String matchType ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                          + internalProcessId ) ).skipIniPropertyWithValue( rootDirectoryAlias,
                                                                                                            relativeFilePath,
                                                                                                            section,
                                                                                                            value,
                                                                                                            matchType );
    }

    @Action
    public void skipTextLine( @Parameter(name = "internalProcessId") String internalProcessId,
                              @Parameter(name = "rootDirectoryAlias") String rootDirectoryAlias,
                              @Parameter(name = "relativeFilePath") String relativeFilePath,
                              @Parameter(name = "line") String line,
                              @Parameter(name = "matchType") String matchType ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                          + internalProcessId ) ).skipTextLine( rootDirectoryAlias,
                                                                                                relativeFilePath,
                                                                                                line,
                                                                                                matchType );
    }

    @Action
    public void takeSnapshot(
                              @Parameter(name = "internalProcessId") String internalProcessId ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).takeSnapshot();
    }

    @Action
    public void compare(
                         @Parameter(name = "thisInternalProcessId") String thisInternalProcessId,
                         @Parameter(name = "thatInternalProcessId") String thatInternalProcessId ) {

        LocalFileSystemSnapshot thisLocalFileSystemSnapshot = ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                                                                              + thisInternalProcessId );
        LocalFileSystemSnapshot thatLocalFileSystemSnapshot = ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX
                                                                                                              + thatInternalProcessId );

        thisLocalFileSystemSnapshot.compare( thatLocalFileSystemSnapshot );
    }

    @Action
    public void loadFromFile(
                              @Parameter(name = "internalProcessId") String internalProcessId,
                              @Parameter(name = "sourceFile") String sourceFile ) throws FileSystemSnapshotException {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).loadFromFile( sourceFile );
    }

    @Action
    public void toFile(
                        @Parameter(name = "internalProcessId") String internalProcessId,
                        @Parameter(name = "backupFile") String backupFile ) {

        ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).toFile( backupFile );
    }

    @Action
    public String toString(
                            @Parameter(name = "internalProcessId") String internalProcessId ) {

        return ( ( LocalFileSystemSnapshot ) dataRepo.getObject( OBJECT_KEY_PREFIX + internalProcessId ) ).toString();
    }
}
