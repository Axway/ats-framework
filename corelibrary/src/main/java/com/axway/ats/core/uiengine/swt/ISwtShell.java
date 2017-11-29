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
package com.axway.ats.core.uiengine.swt;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ISwtShell extends Remote {

    /**
     * 
     * @return the active Shell title
     * @throws RemoteException
     */
    public String getActiveShell() throws RemoteException;

    /**
     * Set active Shell title
     * 
     * @param shellTitle the Shell title
     * @throws RemoteException
     */
    public void setActiveShell(
                                String shellTitle ) throws RemoteException;

    /**
     * Waiting for Shell to appear
     * 
     * @param shellTitle the shell title
     * @param timeout the time to wait. Measured in milliseconds
     * @throws RemoteException
     */
    public void waitForShellToAppear(
                                      String shellTitle,
                                      long timeout ) throws RemoteException;
}
