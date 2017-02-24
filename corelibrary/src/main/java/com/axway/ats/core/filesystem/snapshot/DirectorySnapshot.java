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

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.axway.ats.common.filesystem.snapshot.FileSystemSnapshotException;
import com.axway.ats.common.filesystem.snapshot.equality.EqualityState;
import com.axway.ats.core.utils.IoUtils;

public class DirectorySnapshot implements Serializable {

    private static final long                   serialVersionUID = 1L;

    private static final Logger                 log              = Logger.getLogger( DirectorySnapshot.class );

    private String                              path;

    private Map<String, DirectorySnapshot>      dirSnapshots     = new HashMap<String, DirectorySnapshot>();
    private Map<String, FileSnapshot>           fileSnapshots    = new HashMap<String, FileSnapshot>();

    private Map<String, FindRules>              fileRules        = new HashMap<String, FindRules>();
    private Map<String, Map<String, FindRules>> subDirFileRules  = new HashMap<String, Map<String, FindRules>>();

    private Set<String>                         skippedSubDirs   = new HashSet<String>();

    private EqualityState                       equality;

    DirectorySnapshot( String path, EqualityState equality ) {

        this.path = IoUtils.normalizeUnixDir( path.trim() );
        this.equality = equality;
    }

    DirectorySnapshot( String path, Map<String, FindRules> fileRules, EqualityState equality ) {

        this.path = IoUtils.normalizeUnixDir( path.trim() );
        this.equality = equality;
        if( fileRules != null && !fileRules.isEmpty() ) {
            this.fileRules.putAll( fileRules );
        }

        log.debug( "Add " + this.toString().trim() );
    }

    /**
     * Create an instance from a file
     * @param dirNode
     * @param equality
     */
    static DirectorySnapshot fromFile( Element dirNode, EqualityState equality ) {

        // this dir
        String dirPath = dirNode.getAttributes().getNamedItem( "path" ).getNodeValue();

        DirectorySnapshot dirSnapshot = new DirectorySnapshot( dirPath, equality );

        // its file find rules
        List<Element> fileRuleNodes = SnapshotUtils.getChildrenByTagName( dirNode,
                                                                          LocalFileSystemSnapshot.NODE_FILE_RULE );
        for( Element fileRuleNode : fileRuleNodes ) {

            dirSnapshot.fileRules.put( fileRuleNode.getAttribute( "file" ),
                                       FindRules.getFromString( fileRuleNode.getAttribute( "rules" ) ) );
        }

        // its skipped sub-directories
        List<Element> skippedSubDirNodes = SnapshotUtils.getChildrenByTagName( dirNode,
                                                                               LocalFileSystemSnapshot.NODE_SKIPPED_DIRECTORY );
        for( Element skippedSubDirNode : skippedSubDirNodes ) {

            dirSnapshot.skippedSubDirs.add( skippedSubDirNode.getAttribute( "name" ) );
        }

        // its files
        List<Element> fileNodes = SnapshotUtils.getChildrenByTagName( dirNode,
                                                                      LocalFileSystemSnapshot.NODE_FILE );
        for( Element fileNode : fileNodes ) {
            FileSnapshot fileSnapshot = FileSnapshot.fromFile( fileNode );

            dirSnapshot.fileSnapshots.put( IoUtils.getFileName( fileSnapshot.getPath() ), fileSnapshot );
        }

        // its subdirs
        List<Element> subdirNodes = SnapshotUtils.getChildrenByTagName( dirNode,
                                                                        LocalFileSystemSnapshot.NODE_DIRECTORY );
        for( Element subdirNode : subdirNodes ) {

            DirectorySnapshot subdirSnapshot = DirectorySnapshot.fromFile( subdirNode, equality );
            dirSnapshot.dirSnapshots.put( SnapshotUtils.getDirPathLastToken( subdirSnapshot.getPath() ),
                                          subdirSnapshot );
        }

        return dirSnapshot;
    }

    String getPath() {

        return this.path;
    }

    void skipSubDirectory( String dirPath ) {

        /*
         * This method is called only for top level directories.
         *
         * Prior to taking the snapshot, we do not know the content of this root directory.
         * So here we just remember the dirs that are to be skipped.
         *
         * Currently the only rule for a directory is to SKIP it
         */
        skippedSubDirs.add( dirPath );
    }

    Set<String> getSkippedSubDirectories() {

        return skippedSubDirs;
    }

    void addFindRules( String filePathInThisDirectory, int... rules ) {

        /*
         * This method is called only for top level directories.
         *
         * Prior to taking the snapshot, we do not know the content of this root directory.
         * So here we just remember the rules, but we will apply them later.
         *
         */
        fileRules.put( filePathInThisDirectory, new FindRules( rules ) );
    }

    void takeSnapshot( SnapshotConfiguration configuration ) {

        log.debug( "Add directory " + this.path );

        // do some cleanup - in case user call this method for a second time
        dirSnapshots.clear();
        fileSnapshots.clear();

        // take the snapshot now
        if( !new File( this.path ).exists() ) {
            throw new FileSystemSnapshotException( "Directory '" + this.path + "' does not exist" );
        }

        // check current dir files;  add their absolute path;  distribute sub-directory rules
        Map<String, FindRules> tmpFilesRules = new HashMap<String, FindRules>( fileRules );
        for( Entry<String, FindRules> fileRuleEntry : tmpFilesRules.entrySet() ) {

            // if takeSnapshot() was already called, the fileName is actually a file absolute path
            String fileAbsPath = fileRuleEntry.getKey();
            if( !fileRuleEntry.getKey().startsWith( this.path ) ) {
                fileAbsPath = IoUtils.normalizeUnixFile( this.path + fileRuleEntry.getKey() );
            }
            FindRules rules = fileRuleEntry.getValue();
            if( !rules.isSearchFilenameByRegex() && !new File( fileAbsPath ).exists() ) {

                log.warn( "File \"" + fileAbsPath + "\" doesn't exist, but there is a rule for it!" );
                continue;
            } else {

                if( SnapshotUtils.isFileFromThisDirectory( this.path, fileAbsPath ) ) {
                    fileRules.put( fileAbsPath, rules );
                } else {

                    // the rule belongs to a sub-directory, so we will add it to subDirFileRules
                    int subDirEndIndex = fileAbsPath.indexOf( IoUtils.FORWARD_SLASH, this.path.length() );
                    String subDirPath = fileAbsPath.substring( 0, subDirEndIndex );
                    // because it is a sub-dir, we have to skip the following /, that is why we have that '+ 1' at the end
                    String subDirPathRest = fileAbsPath.substring( subDirPath.length() + 1 );

                    if( !subDirFileRules.containsKey( subDirPath ) ) {
                        subDirFileRules.put( subDirPath, new HashMap<String, FindRules>() );
                    }
                    subDirFileRules.get( subDirPath ).put( subDirPathRest, rules );
                }
            }
            fileRules.remove( fileRuleEntry.getKey() );
        }

        boolean supportHiddenFiles = configuration.isSupportHidden();
        File[] files = new File( this.path ).listFiles();
        if( files != null ) {
            for( File file : files ) {
                if( !file.isHidden() || supportHiddenFiles ) {
                    if( file.isDirectory() ) {

                        String unixDirName = IoUtils.normalizeUnixDir( file.getName() ); // skipped dirs are also in UnixDir (ends with '/') format
                        if( skippedSubDirs.contains( unixDirName ) ) {
                            continue; // skip the sub directory
                        }
                        DirectorySnapshot dirSnapshot = generateDirectorySnapshot( unixDirName, file );
                        dirSnapshot.takeSnapshot( configuration );
                        dirSnapshots.put( file.getName(), dirSnapshot );
                    } else {

                        FileSnapshot fileSnapshot = generateFileSnapshot( configuration, file );
                        if( fileSnapshot != null ) { // if the file is not skipped
                            log.debug( "Add file " + fileSnapshot.toString() );
                            fileSnapshots.put( file.getName(), fileSnapshot );
                        }
                    }
                } else {
                    log.debug( "The hidden " + ( file.isDirectory()
                                                                    ? "directory"
                                                                    : "file" )
                               + " '" + file.getAbsolutePath() + "' will not be processed" );
                }
            }
        }
    }

    private DirectorySnapshot generateDirectorySnapshot( String unixDirName, File file ) {

        DirectorySnapshot dirSnapshot = new DirectorySnapshot( file.getAbsolutePath(),
                                                               subDirFileRules.get( IoUtils.normalizeUnixFile( file.getAbsolutePath() ) ),
                                                               equality );

        // pass sub-dir skipped directories
        for( String skippedSubDir : skippedSubDirs ) {
            if( skippedSubDir.startsWith( unixDirName ) ) { // dirName ends with '/', so skippedSubDir contains sub-dir of dirName
                String subDirToSkip = skippedSubDir.substring( unixDirName.length() );
                dirSnapshot.skipSubDirectory( subDirToSkip );
            }
        }

        // pass sub-dir file rules
        for( Entry<String, FindRules> fileRuleEntry : fileRules.entrySet() ) {
            if( fileRuleEntry.getKey().startsWith( unixDirName ) ) { // it is a subdir file rule
                dirSnapshot.addFindRules( fileRuleEntry.getKey().substring( unixDirName.length() ),
                                          fileRuleEntry.getValue().getRules() );
            }
        }
        return dirSnapshot;
    }

    private FileSnapshot generateFileSnapshot( SnapshotConfiguration configuration, File file ) {

        // find file rules and check whether the file is skipped
        FindRules rules = null;
        for( Entry<String, FindRules> fileRuleEntry : fileRules.entrySet() ) {

            FindRules currentRules = fileRuleEntry.getValue();
            if( currentRules.isSearchFilenameByRegex() ) {
                if( IoUtils.normalizeUnixFile( file.getAbsolutePath() ).matches( fileRuleEntry.getKey() ) ) {
                    rules = currentRules;
                    break;
                }
            } else if( IoUtils.normalizeUnixFile( fileRuleEntry.getKey() )
                                .equals( IoUtils.normalizeUnixFile( file.getAbsolutePath() ) ) ) {
                rules = currentRules;
                break;
            }
        }
        if( rules != null && rules.isSkipFilePath() ) {
            // file is skipped
            return null;
        }

        return new FileSnapshot( configuration, file.getAbsolutePath(), rules );
    }

    void compare( String thisSnapshotName, String thatSnapshotName, DirectorySnapshot that,
                  boolean checkDirName, EqualityState equality ) {

        // check the last entity in the dir path
        if( checkDirName ) {
            String thisDirName = SnapshotUtils.getDirPathLastToken( this.path );
            String thatDirName = SnapshotUtils.getDirPathLastToken( that.path );

            if( !thisDirName.equals( thatDirName ) ) {
                throw new FileSystemSnapshotException( "Directory name " + thisDirName + " of " + this.path
                                             + " is not the same as directory name " + thatDirName + " of "
                                             + that.path );
            }
        }

        // check the files in this directory
        SnapshotUtils.checkFileSnapshots( thisSnapshotName, this.fileSnapshots, thatSnapshotName,
                                          that.fileSnapshots, equality );

        // check the sub-directories
        SnapshotUtils.checkDirSnapshotsDeepLevel( thisSnapshotName, this.dirSnapshots, thatSnapshotName,
                                                  that.dirSnapshots, equality );
    }

    void toFile( Document dom, Element dirSnapshotNode ) {

        // this dir
        dirSnapshotNode.setAttribute( "path", this.path );

        // its file find rules
        for( Entry<String, FindRules> fileRuleEntry : this.fileRules.entrySet() ) {

            String fileNameKey = fileRuleEntry.getKey();
            Element fileRuleNode = dom.createElement( LocalFileSystemSnapshot.NODE_FILE_RULE );
            dirSnapshotNode.appendChild( fileRuleNode );

            fileRuleNode.setAttribute( "rules", fileRuleEntry.getValue().getAsString() );

            if( !fileRuleEntry.getValue().isSearchFilenameByRegex() ) {
                fileNameKey = fileNameKey.substring( this.path.length() );
            }
            fileRuleNode.setAttribute( "file", fileNameKey );

        }

        // its skipped sub-directories
        for( String skippedSubDir : this.skippedSubDirs ) {

            Element skippedSubDirNode = dom.createElement( LocalFileSystemSnapshot.NODE_SKIPPED_DIRECTORY );
            dirSnapshotNode.appendChild( skippedSubDirNode );

            skippedSubDirNode.setAttribute( "name", skippedSubDir );
        }

        // its files
        for( Entry<String, FileSnapshot> fileSnapshotEntry : this.fileSnapshots.entrySet() ) {

            Element fileSnapshotNode = dom.createElement( LocalFileSystemSnapshot.NODE_FILE );
            dirSnapshotNode.appendChild( fileSnapshotNode );

            fileSnapshotNode.setAttribute( "alias", fileSnapshotEntry.getKey() );
            fileSnapshotNode = fileSnapshotEntry.getValue().toFile( dom, fileSnapshotNode );

        }

        // its subdirs
        for( Entry<String, DirectorySnapshot> dirSnapshotNameEntry : this.dirSnapshots.entrySet() ) {

            Element subdirSnapshotNode = dom.createElement( LocalFileSystemSnapshot.NODE_DIRECTORY );
            dirSnapshotNode.appendChild( subdirSnapshotNode );

            subdirSnapshotNode.setAttribute( "alias", dirSnapshotNameEntry.getKey() );
            dirSnapshotNameEntry.getValue().toFile( dom, subdirSnapshotNode );
        }
    }

    public Map<String, DirectorySnapshot> getDirSnapshots() {

        return this.dirSnapshots;
    }

    public Map<String, FindRules> getFileRules() {

        return this.fileRules;
    }

    /**
     * This method is good for debug purpose
     */
    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append( "\n dir: " + this.path + "\n" );

        for( FileSnapshot f : fileSnapshots.values() ) {
            sb.append( f.toString() + "\n" );
        }

        for( DirectorySnapshot d : dirSnapshots.values() ) {
            sb.append( d.toString() );
        }
        return sb.toString();
    }
}
