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

public interface ISwtTextBox extends Remote {

    public void setText(
                         String text,
                         String label,
                         String tooltip,
                         String message,
                         String inGroup,
                         int index,
                         String newText ) throws RemoteException;

    public void typeText(
                          String text,
                          String label,
                          String tooltip,
                          String message,
                          String inGroup,
                          int index,
                          String newText ) throws RemoteException;

    public String getText(
                           String text,
                           String label,
                           String tooltip,
                           String message,
                           String inGroup,
                           int index ) throws RemoteException;

    public boolean isEnabled(
                              String text,
                              String label,
                              String tooltip,
                              String message,
                              String inGroup,
                              int index ) throws RemoteException;

    public void waitForEnabled(
                                String text,
                                String label,
                                String tooltip,
                                String message,
                                String inGroup,
                                int index,
                                long timeout ) throws RemoteException;

}
