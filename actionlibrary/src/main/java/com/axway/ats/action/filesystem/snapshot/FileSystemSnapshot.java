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

import com.axway.ats.action.ActionLibraryConfigurator;
import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.core.filesystem.snapshot.IFileSystemSnapshot;
import com.axway.ats.core.filesystem.snapshot.LocalFileSystemSnapshot;
import com.axway.ats.core.filesystem.snapshot.SnapshotConfiguration;
import com.axway.ats.core.utils.HostUtils;
import com.axway.ats.core.utils.StringUtils;

/**
 * Main class for comparing file system sets (of directories and contained files)
 * for changes like different attributes, some file/dir. missing or added, etc.
 *
 * <br/><br/>
 * <b>User guide</b>
 * <a href="https://techweb.axway.com/confluence/display/ATS/File+System+Snapshots">page</a>
 * related to this class
 */
@PublicAtsApi
public class FileSystemSnapshot {

    private IFileSystemSnapshot   fsSnapshotImpl;

    /**
     * Skip checking the size of a file
     */
    @PublicAtsApi
    public static final int       SKIP_FILE_SIZE               = LocalFileSystemSnapshot.SKIP_FILE_SIZE;
    /**
     * Skip checking the last modification time of a file
     */
    @PublicAtsApi
    public static final int       SKIP_FILE_MODIFICATION_TIME  = LocalFileSystemSnapshot.SKIP_FILE_MODIFICATION_TIME;
    /**
     * Skip checking the MD5 sum of a file
     */
    @PublicAtsApi
    public static final int       SKIP_FILE_MD5                = LocalFileSystemSnapshot.SKIP_FILE_MD5;
    /**
     * Skip checking the permissions attribute of a file
     */
    @PublicAtsApi
    public static final int       SKIP_FILE_PERMISSIONS        = LocalFileSystemSnapshot.SKIP_FILE_PERMISSIONS;

    /**
     * Must check the size of a file. This value will override the global settings when used
     */
    @PublicAtsApi
    public static final int       CHECK_FILE_SIZE              = LocalFileSystemSnapshot.CHECK_FILE_SIZE;
    /**
     * Must check the silast modification timeze of a file. This value will override the global settings when used
     */
    @PublicAtsApi
    public static final int       CHECK_FILE_MODIFICATION_TIME = LocalFileSystemSnapshot.CHECK_FILE_MODIFICATION_TIME;
    /**
     * Must check the MD5 sum of a file. This value will override the global settings when used
     */
    @PublicAtsApi
    public static final int       CHECK_FILE_MD5               = LocalFileSystemSnapshot.CHECK_FILE_MD5;
    /**
     * Must check the permissions attribute of a file. This value will override the global settings when used
     */
    @PublicAtsApi
    public static final int       CHECK_FILE_PERMISSIONS       = LocalFileSystemSnapshot.CHECK_FILE_PERMISSIONS;

    private String                atsAgent;

    private SnapshotConfiguration configuration;

    /**
     * Create an instance working on a remote host
     *
     * @param atsAgent the agent when to work
     * @param name Snapshot name. Used as identifier for comparison results
     */
    @PublicAtsApi
    public FileSystemSnapshot( String atsAgent, String name ) {

        if( StringUtils.isNullOrEmpty( name ) ) {
            throw new FileSystemSnapshotException( "Invalid snapshot name '" + name + "'" );
        }

        loadConfiguration();

        this.fsSnapshotImpl = getOperationsImplementationFor( atsAgent, name, configuration );
        this.atsAgent = atsAgent;

    }

    /**
     * Create a local instance
     *
     * @param name Snapshot name. Used as identifier for comparison results
     */
    @PublicAtsApi
    public FileSystemSnapshot( String name ) {

        if( StringUtils.isNullOrEmpty( name ) ) {
            throw new FileSystemSnapshotException( "Invalid snapshot name '" + name + "'" );
        }

        loadConfiguration();

        this.fsSnapshotImpl = getOperationsImplementationFor( null, name, configuration );

    }

    private FileSystemSnapshot() {

    }

    /**
     * Extract all the configuration information we are interested in.
     * It will be send to the local or remote instance.
     */
    private void loadConfiguration() {

        ActionLibraryConfigurator configurator = ActionLibraryConfigurator.getInstance();
        configuration = new SnapshotConfiguration();
        configuration.setCheckModificationTime( configurator.getFileSnapshotCheckModificationTime() );
        configuration.setCheckSize( configurator.getFileSnapshotCheckFileSize() );
        configuration.setCheckMD5( configurator.getFileSnapshotCheckFileMd5() );
        configuration.setCheckPermissions( configurator.getFileSnapshotCheckFilePermissions() );
        configuration.setSupportHidden( configurator.getFileSnapshotSupportHiddenFiles() );
    }

    /**
     * Create an instance from an existing one. Used to verify the same
     * directories and files as the ones specified in provided argument.
     *
     * @param newSnapshotName snapshot name for the new instance
     * @return the new instance
     */
    @PublicAtsApi
    public FileSystemSnapshot newSnapshot( String newSnapshotName ) {

        FileSystemSnapshot fss = new FileSystemSnapshot();
        if( HostUtils.isLocalAtsAgent( this.atsAgent ) ) {
            fss.fsSnapshotImpl = ( ( LocalFileSystemSnapshot ) this.fsSnapshotImpl ).newSnapshot( newSnapshotName );
        } else {
            fss.fsSnapshotImpl = ( ( RemoteFileSystemSnapshot ) this.fsSnapshotImpl ).newSnapshot( newSnapshotName );
        }
        return fss;
    }

    /**
     * Add a directory to the snapshot. This is a root directory for this snapshot
     *
     * @param directoryAlias directory alias. Case sensitive.
     * @param directoryPath directory path
     */
    @PublicAtsApi
    public void addDirectory( String directoryAlias, String directoryPath ) {

        this.fsSnapshotImpl.addDirectory( directoryAlias, directoryPath );
    }

    /**
     * Point a sub directory which will not be processed
     *
     * @param rootDirectoryAlias the alias of the root directory
     * @param relativeDirectoryPath path to this directory relative to the path of the one with provided alias
     */
    @PublicAtsApi
    public void skipDirectory( String rootDirectoryAlias, String relativeDirectoryPath ) {

        this.fsSnapshotImpl.skipDirectory( rootDirectoryAlias, relativeDirectoryPath );
    }

    /**
     * Point a file which will not be processed
     *
     * @param rootDirectoryAlias the alias of the root directory
     * @param relativeFilePath path to this file relative to the path of directory with specified alias
     * @param skipRules specifies which file attributes to be skipped.<br/>
     *                  Use one of the FileSystemSnapshot.SKIP_* constants<br/>
     *                  This is an optional parameter - if not used, the whole file will be skipped
     */
    @PublicAtsApi
    public void skipFile( String rootDirectoryAlias, String relativeFilePath, int... skipRules ) {

        this.fsSnapshotImpl.skipFile( rootDirectoryAlias, relativeFilePath, skipRules );
    }

    /**
     * Point a file which will not be processed. The file name is searched by regular expression
     *
     * @param rootDirectoryAlias the alias of the root directory
     * @param relativeFilePath path to this file relative to the root directory
     * @param skipRules specifies which file attributes to be skipped.<br/>
     *                  Use one of the FileSystemSnapshot.SKIP_* constants<br/>
     *                  This is an optional parameter - if not used, the whole file will be skipped
     */
    @PublicAtsApi
    public void skipFileByRegex( String rootDirectoryAlias, String relativeFilePath, int... skipRules ) {

        this.fsSnapshotImpl.skipFileByRegex( rootDirectoryAlias, relativeFilePath, skipRules );
    }

    /**
     * Explicitly request to check some file attribute. <br/>
     * Example: Globally you have disabled checking the files' last modification time, but using this
     * method you can override this setting for some particular files
     *
     * @param rootDirectoryAlias the alias of the root directory
     * @param relativeFilePath path to this file relative to the directory with provided alias
     * @param checkRules specifies which file attributes to be checked.<br/>
     *                   Use one of the FileSystemSnapshot.CHECK_* constants<br/>
     *                   This is an optional parameter - if not used, the all supported attributes will be checked
     */
    @PublicAtsApi
    public void checkFile( String rootDirectoryAlias, String relativeFilePath, int... checkRules ) {

        this.fsSnapshotImpl.checkFile( rootDirectoryAlias, relativeFilePath, checkRules );
    }

    /**
     * Take the file system snapshot.
     * This is the moment when all given directories are searched and an actual snapshot is made.
     */
    @PublicAtsApi
    public void takeSnapshot() {

        this.fsSnapshotImpl.takeSnapshot();
    }

    /**
     * Compare two snapshots
     * @param that the other snapshot
     */
    @PublicAtsApi
    public void compare( FileSystemSnapshot that ) {

        // both instances are compared locally

        LocalFileSystemSnapshot thisLocal;
        if( this.fsSnapshotImpl instanceof LocalFileSystemSnapshot ) {
            thisLocal = ( LocalFileSystemSnapshot ) this.fsSnapshotImpl;
        } else {
            thisLocal = ( ( RemoteFileSystemSnapshot ) this.fsSnapshotImpl ).getFileSystemSnapshot();
        }

        LocalFileSystemSnapshot thatLocal;
        if( that.fsSnapshotImpl instanceof LocalFileSystemSnapshot ) {
            thatLocal = ( LocalFileSystemSnapshot ) that.fsSnapshotImpl;
        } else {
            thatLocal = ( ( RemoteFileSystemSnapshot ) that.fsSnapshotImpl ).getFileSystemSnapshot();
        }

        thisLocal.compare( thatLocal );
    }

    /**
     * Load a snapshot from a local file
     *
     * @param newSnapshotName the name of the new snapshot
     * </br>Pass null or empty string if want to use the snapshot name as saved in the file,
     * or provide a new name here
     * @param sourceFile the source file
     * @return the new snapshot instance
     */
    @PublicAtsApi
    public void loadFromLocalFile( String newSnapshotName, String sourceFile ) throws FileSystemSnapshotException {

        if( this.fsSnapshotImpl instanceof LocalFileSystemSnapshot ) {
            // local file - local instance
            ( ( LocalFileSystemSnapshot ) this.fsSnapshotImpl ).loadFromFile( sourceFile );

            if( !StringUtils.isNullOrEmpty( newSnapshotName ) ) {
                // update snapshot name
                ( ( LocalFileSystemSnapshot ) this.fsSnapshotImpl ).setName( newSnapshotName );
            }
        } else {
            // local file must be pushed to a remote File System Snapshot instance
            LocalFileSystemSnapshot localFSS = new LocalFileSystemSnapshot( newSnapshotName, configuration );
            localFSS.loadFromFile( sourceFile );

            // now we will replace the remote instance with the local one which was just loaded from a local file
            ( ( RemoteFileSystemSnapshot ) this.fsSnapshotImpl ).pushFileSystemSnapshot( localFSS );
        }
    }

    /**
     * Load a snapshot from the remote host as specified when creating this instance.
     * </br><b>Note:</b> It will throw an error if this is a local instance.
     *
     * @param newSnapshotName the name of the new snapshot
     * </br>Pass null or empty string if want to use the snapshot name as saved in the file,
     * or provide a new name here
     * @param sourceFile the source file
     * @return the new snapshot instance
     */
    @PublicAtsApi
    public void loadFromRemoteFile( String newSnapshotName, String sourceFile ) throws FileSystemSnapshotException {

        if( this.fsSnapshotImpl instanceof LocalFileSystemSnapshot ) {
            throw new FileSystemSnapshotException( "Cannot load snapshot from a remote host as this is a local File System Snapshot instance" );
        }

        // load the snapshot
        ( ( RemoteFileSystemSnapshot ) this.fsSnapshotImpl ).loadFromFile( sourceFile );

        // update the snapshot name
        ( ( RemoteFileSystemSnapshot ) this.fsSnapshotImpl ).setName( newSnapshotName );
    }

    /**
     * Save a snapshot in a local file
     * @param backupFile the backup file name
     */
    @PublicAtsApi
    public void toLocalFile( String backupFile ) {

        LocalFileSystemSnapshot localFSS;
        if( this.fsSnapshotImpl instanceof LocalFileSystemSnapshot ) {
            localFSS = ( LocalFileSystemSnapshot ) this.fsSnapshotImpl;
        } else {
            localFSS = ( ( RemoteFileSystemSnapshot ) this.fsSnapshotImpl ).getFileSystemSnapshot();
        }

        localFSS.toFile( backupFile );
    }

    /**
     * Save a snapshot on the remote host as specified when creating this instance.
     * </br><b>Note:</b> It will throw an error if this is a local instance.
     * @param backupFile the backup file name
     */
    @PublicAtsApi
    public void toRemoteFile( String backupFile ) {

        if( this.fsSnapshotImpl instanceof LocalFileSystemSnapshot ) {
            throw new FileSystemSnapshotException( "Cannot save on a remote host as this is a local File System Snapshot instance" );
        }

        ( ( RemoteFileSystemSnapshot ) fsSnapshotImpl ).toFile( backupFile );
    }

    /**
     * This method is good for debug purpose
     */
    @PublicAtsApi
    public String toString() {

        return this.fsSnapshotImpl.toString();
    }

    private IFileSystemSnapshot getOperationsImplementationFor( String atsAgent, String name,
                                                                SnapshotConfiguration configuration ) {

        atsAgent = HostUtils.getAtsAgentIpAndPort( atsAgent );

        if( HostUtils.isLocalAtsAgent( atsAgent ) ) {
            return new LocalFileSystemSnapshot( name, configuration );
        } else {
            return new RemoteFileSystemSnapshot( atsAgent, name, configuration );
        }
    }
}
