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
package com.axway.ats.action.mail.model;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;

/** *
 * A replacement for the standard SSLSocketFactory which allows specifying which TrustManager to use.
 * Our implementation uses the BasicTrustManager.
 *
 * The code is taken from the SSLNOTES.txt in the JavaMail 1.4.1 distribution.
 */
public class BasicSslSocketFactory extends SSLSocketFactory {

    private static BasicSslSocketFactory instance;

    private SSLSocketFactory             factory;

    /**
     *
     * @throws SecurityException if the {@link SSLSocketFactory} instantiation failed
     */
    public BasicSslSocketFactory() {

        try {
            SSLContext sslcontext = SSLContext.getInstance( "TLS" );
            sslcontext.init( null, new TrustManager[]{ new BasicTrustManager() }, null );
            factory = sslcontext.getSocketFactory();
        } catch( Exception e ) {
            throw new SecurityException( "Failed to instantiate SSLSocketFactory", e );
        }
    }

    /**
     *
     * @return an instance of the {@link BasicSslSocketFactory}
     *
     * @throws SecurityException if the {@link SSLSocketFactory} instantiation failed
     */
    public static synchronized SSLSocketFactory getDefault() {

        if( instance == null ) {
            instance = new BasicSslSocketFactory();
        }
        return instance;
    }

    @Override
    public Socket createSocket() throws IOException {

        return factory.createSocket();
    }

    @Override
    public Socket createSocket(
                                Socket socket,
                                String s,
                                int i,
                                boolean flag ) throws IOException {

        return factory.createSocket( socket, s, i, flag );
    }

    @Override
    public Socket createSocket(
                                InetAddress inaddr,
                                int i,
                                InetAddress inaddr1,
                                int j ) throws IOException {

        return factory.createSocket( inaddr, i, inaddr1, j );
    }

    @Override
    public Socket createSocket(
                                InetAddress inaddr,
                                int i ) throws IOException {

        return factory.createSocket( inaddr, i );
    }

    @Override
    public Socket createSocket(
                                String s,
                                int i,
                                InetAddress inaddr,
                                int j ) throws IOException {

        return factory.createSocket( s, i, inaddr, j );
    }

    @Override
    public Socket createSocket(
                                String s,
                                int i ) throws IOException {

        return factory.createSocket( s, i );
    }

    @Override
    public String[] getDefaultCipherSuites() {

        return factory.getDefaultCipherSuites();
    }

    @Override
    public String[] getSupportedCipherSuites() {

        return factory.getSupportedCipherSuites();
    }
}
