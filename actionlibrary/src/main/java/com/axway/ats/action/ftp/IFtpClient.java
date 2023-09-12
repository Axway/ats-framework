package com.axway.ats.action.ftp;

import com.axway.ats.common.filetransfer.FileTransferException;

import java.io.InputStream;
import java.util.List;

/**
 * A base interface for clients that are implementing the File Transfer Protocol (FTP)</br>
 * Currently here are declared only all of the supported FTP commands.
 * */
public interface IFtpClient {

    /**
     * Connect to a remote host using basic authentication
     *
     * @param hostname the host to connect to
     * @param userName the user name
     * @param password the password for the provided user name
     * @throws FtpException
     */
    public void connect(String hostname, String userName, String password) throws FtpException;

    /**
     * Connect to a remote host using secure authentication
     *
     * @param hostname the host to connect to
     * @param keystoreFile the file containing the key store
     * @param keystorePassword the key store password
     * @param publicKeyAlias the public key alias
     * @throws FtpException
     */
    public void connect(String hostname, String keystoreFile, String keystorePassword, String publicKeyAlias)
            throws FtpException;

    /**
     * Disconnect from the remote host
     *
     * @throws FtpException
     */
    public void disconnect() throws FtpException;

    /**
     * Execute command that will receive data from the server
     * */
    public String executeCommand(String command);

    /**
     * Execute command that will send data to the server
     * */
    public String executeCommand(String command, InputStream localData);

    public Object executeCommand(String command, Object[] arguments);

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

    public void storeFile(String localFile, String remoteDir, String remoteFile);

    public void retrieveFile(String localFile, String remoteDir, String remoteFile);

}
