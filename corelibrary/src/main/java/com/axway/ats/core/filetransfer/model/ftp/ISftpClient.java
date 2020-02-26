/*
 * Copyright 2019 Axway Software
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
package com.axway.ats.core.filetransfer.model.ftp;

import java.io.InputStream;
import java.util.List;

/**
 * The base interface that each SFTP client must implement
 * */
public interface ISftpClient {

    public String pwd();

    public List<String> ls();

    public List<String> ls( boolean namesOnly );

    public List<String> ls( String filepath );

    public List<String> ls( String filepath, boolean namesOnly );

    public void cd( String directory );

    public void cdup();

    public void mkdir( String directory );

    public void rmdir( String directory );

    public void rmdir( String directory, boolean recursive );

    public void rm( String filename );

    public void put( InputStream inputStream, String remoteFile );

    public void put( String localFile, String remoteFile );

    public void put( String localFile );

    public void get( String remoteFile, String localFile, boolean append );

    public InputStream get( String remoteFile );

    public void rename( String oldfilepath, String newfilepath );

    public void chmod( String filepath, int permissions );

    public void chmod( String filepath, int permissions, boolean recursive );

    public void chgrp( String filepath, int gid );

    public void chgrp( String filepath, int gid, boolean recursive );

    public void chown( String filepath, int uid );

    public void chown( String filepath, int uid, boolean recursive );

    public void ln( String oldpath, String newpath, boolean symbolic );

    public String readlink( String filepath );

    public void symlink( String oldpath, String newpath );

    public void hardlink( String oldpath, String newpath );

    public Object executeCommand( String command, Object[] arguments );

    // additional methods that are not explicitly part of the SFTP standard, but are useful (a lot)

    public boolean isDirectory( String filepath );

    public boolean isFile( String filepath );

    public boolean isLink( String filepath );

    public boolean doesFileExist( String filepath );

    public boolean doesDirectoryExist( String filepath );

    public int getGID( String filepath );

    public int getUID( String filepath );

    public String getPermissions( String filepath );

    public long getFileSize( String filepath );

    public long getModificationTime( String filepath );

}
