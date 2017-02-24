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
package com.axway.ats.action.smtp;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import com.axway.ats.action.model.InetSmtpConnection;
import com.axway.ats.common.PublicAtsApi;

import gnu.inet.smtp.ParameterList;

/**
 * SMTP connection
 */
@PublicAtsApi
public class SmtpConnection {

    private InetSmtpConnection connectionObject;

    /**
     * @param remoteHost the target host
     * @throws IOException
     */
    @PublicAtsApi
    public SmtpConnection( String remoteHost ) {

        try {
            connectionObject = new InetSmtpConnection( remoteHost );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param remoteHost the target host
     * @param remotePort the target SMTP port
     * @
     */
    @PublicAtsApi
    public SmtpConnection( String remoteHost,
                           int remotePort ) {

        try {
            connectionObject = new InetSmtpConnection( remoteHost, remotePort );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param remoteHost the target host
     * @param remotePort the target SMTP port
     * @param bindAddress the local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     */
    @PublicAtsApi
    public SmtpConnection( String remoteHost,
                           int remotePort,
                           String bindAddress ) {

        try {
            connectionObject = new InetSmtpConnection( remoteHost, remotePort, bindAddress );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    /**
     * @param remoteHost the target host
     * @param remotePort the target SMTP port
     * @param bindAddress the local address where the socket will be bind. By default it is null and then the system
     *   picks up an ephemeral port and a valid local address to bind the socket.
     * @param connectionTimeout the connection timeout in milliseconds
     * @param timeout the I/O timeout in milliseconds
     */
    @PublicAtsApi
    public SmtpConnection( String remoteHost,
                           int remotePort,
                           String bindAddress,
                           int connectionTimeout,
                           int timeout ) {

        try {
            connectionObject = new InetSmtpConnection( remoteHost,
                                                       remotePort,
                                                       bindAddress,
                                                       connectionTimeout,
                                                       timeout );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public boolean authenticate(
                                 String mechanism,
                                 String username,
                                 String password ) {

        try {
            return connectionObject.authenticate( mechanism, username, password );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public OutputStream data() {

        try {
            return connectionObject.data();
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public void data(
                      String text ) {

        try {
            //obtain an output stream
            OutputStream dataStream = data();
            dataStream.write( text.getBytes() );
            dataStream.flush();
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }

        if( !finishData() ) {
            throw new RuntimeException( "Error executing data command" );
        }
    }

    /**
     * Issues an EHLO command.
     * If successful, returns a list of the SMTP extensions supported by the
     * server.
     * Otherwise returns null, and HELO should be called.
     * @param hostname the local host name
     */
    @PublicAtsApi
    public List<String> ehlo(
                              String hostname ) {

        try {

            return getStringList( connectionObject.ehlo( hostname ) );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public List<String> expn(
                              String address ) {

        try {
            return getStringList( connectionObject.expn( address ) );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public boolean finishData() {

        try {
            return connectionObject.finishData();
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public String getGreeting() {

        return connectionObject.getGreeting();
    }

    @PublicAtsApi
    public String getLastResponse() {

        return connectionObject.getLastResponse();
    }

    @PublicAtsApi
    public int getLastResponseCode() {

        return connectionObject.getLastResponseCode();
    }

    @PublicAtsApi
    public boolean helo(
                         String hostname ) {

        try {
            return connectionObject.helo( hostname );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public List<String> help(
                              String arg ) {

        try {
            return getStringList( connectionObject.help( arg ) );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public boolean mailFrom(
                             String reversePath ) {

        try {
            return connectionObject.mailFrom( reversePath, null );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public boolean mailFrom(
                             String reversePath,
                             ParameterList parameters ) {

        try {
            return connectionObject.mailFrom( reversePath, parameters );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public void noop() {

        try {
            connectionObject.noop();
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public void quit() {

        try {
            connectionObject.quit();
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public boolean rcptTo(
                           String forwardPath ) {

        try {
            return connectionObject.rcptTo( forwardPath, null );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public boolean rcptTo(
                           String forwardPath,
                           ParameterList parameters ) {

        try {
            return connectionObject.rcptTo( forwardPath, parameters );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public void rset() {

        try {
            connectionObject.rset();
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public int sendCommand(
                            String command ) {

        try {
            return connectionObject.sendCommand( command );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public boolean starttls() {

        try {
            return connectionObject.starttls();
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    @PublicAtsApi
    public List<String> vrfy(
                              String address ) {

        try {
            return getStringList( connectionObject.vrfy( address ) );
        } catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    private List<String> getStringList(
                                        List<?> objectsList ) {

        List<String> stringList = new ArrayList<String>();
        for( Object object : objectsList ) {
            stringList.add( String.valueOf( object ) );
        }
        return stringList;
    }

}
