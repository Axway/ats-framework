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
package com.axway.ats.core.atsconfig.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;

import com.axway.ats.core.atsconfig.AtsProjectConfiguration;
import com.axway.ats.core.atsconfig.exceptions.AtsConfigurationException;
import com.axway.ats.core.utils.IoUtils;
import com.axway.ats.core.utils.XmlUtils;

public class PathInfo {

    private String              path;
    private String              sftpPath;
    private boolean             isFile;
    private boolean             isRecursive;
    private boolean             upgrade;

    private boolean             isChecked;

    private Map<String, String> internalFiles = new HashMap<String, String>(); // Map<fileName, absoluteFilePath>

    public PathInfo( Element pathNode, boolean isFile, String homeFolder ) throws AtsConfigurationException {

        this( pathNode, isFile, homeFolder, null, true );
    }

    public PathInfo( Element pathNode, boolean isFile, String homeFolder, String sftpHome,
                     boolean isUnix ) throws AtsConfigurationException {

        this.isFile = isFile;
        this.path = XmlUtils.getMandatoryAttribute( pathNode, AtsProjectConfiguration.NODE_ATTRIBUTE_PATH );
        boolean isAbsolutePath = XmlUtils.getBooleanAttribute( pathNode, "absolute", false );
        if( !isFile ) {
            this.isRecursive = XmlUtils.getBooleanAttribute( pathNode, "recursive", false );
        }

        char endChar = homeFolder.charAt( homeFolder.length() - 1 );
        if( endChar != '/' && endChar != '\\' ) {
            homeFolder = homeFolder + '/';
        }

        if( sftpHome != null ) {

            if( isAbsolutePath ) {
                if( !isUnix ) {
                    // remove drive letter and assume that the STFP home folder is the drive root folder
                    this.sftpPath = this.path.substring( this.path.indexOf( ':' ) + 1 );
                } else {
                    this.sftpPath = this.path;
                }
            } else {
                this.sftpPath = sftpHome + this.path;
            }

            if( isFile ) {
                this.sftpPath = IoUtils.normalizeUnixFile( this.sftpPath );
            } else {
                this.sftpPath = IoUtils.normalizeUnixDir( this.sftpPath );
            }
            this.sftpPath = this.sftpPath.replace( "//", "/" ).replace( "\\\\", "\\" );
        }

        if( !isAbsolutePath ) {
            this.path = homeFolder + this.path;
        }

        this.upgrade = XmlUtils.getBooleanAttribute( pathNode, "upgrade", true );
    }

    public String getPath() {

        return path;
    }

    public String getSftpPath() {

        return sftpPath;
    }

    public boolean isFile() {

        return isFile;
    }

    public boolean isUpgrade() {

        return upgrade;
    }

    public boolean isRecursive() {

        return isRecursive;
    }

    public Map<String, String> getInternalFiles() {

        return internalFiles;
    }

    public void loadInternalFilesMap() throws AtsConfigurationException {

        if( !isFile ) {

            File folder = new File( this.path );
            if( !folder.exists() || !folder.isDirectory() ) {
                throw new AtsConfigurationException( "Directory '" + this.path
                                                     + "' doesn't exist or is not a directory" );
            }
            collectFiles( folder, isRecursive );
        }
    }

    private void collectFiles( File folder, boolean recursive ) throws AtsConfigurationException {

        File[] files = folder.listFiles();
        if( files != null ) {

            for( File file : files ) {
                if( file.isFile() ) {
                    try {
                        internalFiles.put( file.getName(), file.getCanonicalPath() );
                    } catch( IOException ioe ) {
                        throw new AtsConfigurationException( "Could not get file path", ioe );
                    }
                } else if( recursive ) {
                    collectFiles( file, recursive );
                }
            }
        }
    }

    @Override
    public boolean equals( Object obj ) {

        if( this == obj ) {
            return true;
        }
        if( obj == null || ! ( obj instanceof PathInfo ) ) {
            return false;
        }
        return ( ( PathInfo ) obj ).path.equals( this.path );
    }

    public boolean isChecked() {

        return isChecked;
    }

    public void setChecked( boolean isChecked ) {

        this.isChecked = isChecked;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append( isFile
                          ? "file"
                          : "folder" );
        sb.append( " path=" + path );
        if( sftpPath != null ) {
            sb.append( ", sftpPath=" + sftpPath );
        }
        sb.append( ", recursive=" + isRecursive );
        sb.append( ", upgrade=" + upgrade );

        return sb.toString();
    }
}
