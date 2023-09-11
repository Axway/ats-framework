package com.axway.ats.action.ftp;

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
    public String executeCommand(String command);

    /**
     * Execute command that will send data to the server
     * */
    public String executeCommand(String command, InputStream localData);

    public String[] getAllReplyLines();

    public String getAllReplyLinesAsString();

    public void logAllReplyLines();

    // commands

    public String help();

    public String pwd();

    public void cwd(String directory);

    public String cdup();

    public void mkd(String directory);

    public void rmd(String directory);

    public long size(String file);

    /**
     * Like ls -la (ll)
     * */
    public List<String> list(String directory);

    List<String> listFileNames(String directory);

    public String mlst(String directory);

    int getLastReplyCode();

    public List<String> mlsd(String directory);

    /**
     * Like ls
     * */
    public List<String> nlst(String directory);

    public void appe(String file, String content);

    public void dele(String file);

    public void rename(String from, String to);

    public int pasv();

}
