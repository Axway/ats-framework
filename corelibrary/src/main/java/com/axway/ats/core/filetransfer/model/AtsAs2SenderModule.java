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
package com.axway.ats.core.filetransfer.model;

import java.net.HttpURLConnection;

import org.apache.http.Header;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.log4j.Logger;
import org.openas2.message.Message;
import org.openas2.processor.sender.AS2SenderModule;

/**
 * Module which sends AS2 files.
 * We have extended the basic AS2 sender module, so it is able to do Basic HTTP Authentication
 */
public class AtsAs2SenderModule extends AS2SenderModule {

    private static final Logger log = Logger.getLogger( AtsAs2SenderModule.class );

    private String              httpUsername;
    private String              httpPassword;

    public AtsAs2SenderModule() {

    }

    /**
     * Set username and password for Basic HTTP Authentication
     * 
     * @param httpUsername 
     * @param httpPassword
     */
    public void setHttpBasicAuthentication(
                                            String httpUsername,
                                            String httpPassword ) {

        this.httpUsername = httpUsername;
        this.httpPassword = httpPassword;
    }

    @Override
    protected void updateHttpHeaders(
                                      HttpURLConnection conn,
                                      Message msg ) {

        // let the default AS2 module touch the headers
        super.updateHttpHeaders( conn, msg );

        // add the Basic Authentication header
        if( this.httpUsername != null && this.httpUsername.length() > 0 ) {
            log.debug( "Using Basic HTTP Authentication for user " + this.httpUsername + " and password "
                       + this.httpPassword );

            Header basicAuthenticationHeader = BasicScheme.authenticate( new UsernamePasswordCredentials( this.httpUsername,
                                                                                                          this.httpPassword ),
                                                                         "US-ASCII",
                                                                         false );
            conn.setRequestProperty( basicAuthenticationHeader.getName(),
                                     basicAuthenticationHeader.getValue() );
        }
    }
}
