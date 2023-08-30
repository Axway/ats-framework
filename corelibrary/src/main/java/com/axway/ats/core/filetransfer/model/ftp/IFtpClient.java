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
package com.axway.ats.core.filetransfer.model.ftp;

import java.io.InputStream;
import java.util.List;

/**
 * A base interface for clients that are implementing the File Transfer Protocol (FTP)</br>
 * Currently here are declared only all of the supported FTP commands.
 * */
public interface IFtpClient {

    /**
     * Execute command that will receive data from the server
     * */
    public String executeCommand( String command );

    /**
     * Execute command that will send data to the server
     * */
    public String executeCommand( String command, InputStream localData );

    public String[] getAllReplyLines();

    public String getAllReplyLinesAsString();

    public void logAllReplyLines();

    // commands

    public String help();

    public String pwd();

    public void cwd( String directory );

    public String cdup();

    public void mkd( String directory );

    public void rmd( String directory );

    public long size( String file );

    /**
     * Like ls -la (ll)
     * */
    public List<String> list( String directory );

    public String mlst( String directory );

    public List<String> mlsd( String directory );

    /**
     * Like ls
     * */
    public List<String> nlst( String directory );

    public void stor( String localFile, String remoteFile );

    /**
     * Retrieve file and return it's content
     * */
    public String retr( String file );

    /**
     * Retrieve file and save it on the local file system
     * */
    public void retr( String remoteFile, String localFile );

    public void appe( String file, String content );

    public void dele( String file );

    public void rename( String from, String to );

    public int pasv();

}