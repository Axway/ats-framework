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
package com.axway.ats.uiengine.internal.driver;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.axway.ats.core.uiengine.JavaSecurityUtils;
import com.axway.ats.uiengine.UiDriver;
import com.axway.ats.uiengine.exceptions.RmiException;

/**
 * An abstraction of drivers connecting remotely to ATS Eclipse plugins.
 * <br><b>NOTE: </b>Users should directly use this driver in their tests,
 * but use its concrete implementations instead
 */
public abstract class AbstractRMIDriver extends UiDriver {

    private Logger   log                   = LogManager.getLogger( this.getClass() );

    private Registry registry;

    private String   ip;

    private int      rmiPort;

    private String   connectionDescription = "RMI server at " + this.ip + ":" + this.rmiPort;

    /**
     * @param port 
     * @param ip 
     */
    protected AbstractRMIDriver( String ip, int rmiPort ) {

        this.ip = ip;
        this.rmiPort = rmiPort;
        this.connectionDescription = "RMI server at " + this.ip + ":" + this.rmiPort;
    }

    public Registry getRegistry() {

        if( this.registry == null ) {
            throw new RmiException( "No connection available to " + this.connectionDescription );
        } else {
            return this.registry;
        }
    }

    @Override
    public void start() {

        try {
            JavaSecurityUtils.unlockJavaSecurity();

            if( System.getSecurityManager() == null ) {
                System.setSecurityManager( new SecurityManager() );
            }

            log.info( "Connecting to " + connectionDescription );
            this.registry = LocateRegistry.getRegistry( this.ip, this.rmiPort );
            log.info( "Successfully connected to " + connectionDescription );
        } catch( Exception e ) {
            throw new RmiException( "Error connecting to " + connectionDescription, e );
        }
    }

    @Override
    public void stop() {

        this.registry = null;
        log.info( "Disconnected from " + connectionDescription );
    }
}
