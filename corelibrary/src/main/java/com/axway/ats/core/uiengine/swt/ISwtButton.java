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

public interface ISwtButton extends Remote {

    public void click(
                       String text,
                       String label,
                       String tooltip,
                       String inGroup,
                       int index ) throws RemoteException;

    public void contextMenuClick(
                                  String text,
                                  String label,
                                  String tooltip,
                                  String inGroup,
                                  int index,
                                  String... menuLabels ) throws RemoteException;

    public void focus(
                       String text,
                       String label,
                       String tooltip,
                       String inGroup,
                       int index ) throws RemoteException;

    public String getText(
                           String text,
                           String label,
                           String tooltip,
                           String inGroup,
                           int index ) throws RemoteException;

    public String getToolTipText(
                                  String text,
                                  String label,
                                  String tooltip,
                                  String inGroup,
                                  int index ) throws RemoteException;

    public String getId(
                         String text,
                         String label,
                         String tooltip,
                         String inGroup,
                         int index ) throws RemoteException;

    public boolean isEnabled(
                              String text,
                              String label,
                              String tooltip,
                              String inGroup,
                              int index ) throws RemoteException;

    public boolean isVisible(
                              String text,
                              String label,
                              String tooltip,
                              String inGroup,
                              int index ) throws RemoteException;

    public void waitForEnabled(
                                String text,
                                String label,
                                String tooltip,
                                String inGroup,
                                int index,
                                long timeout ) throws RemoteException;
}
