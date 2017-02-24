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

public interface ISwtComboBox extends Remote {

    public void setSelection(
                              String text,
                              String label,
                              String inGroup,
                              int index,
                              int selectionIndex ) throws RemoteException;

    public void setSelection(
                              String text,
                              String label,
                              String inGroup,
                              int index,
                              String selectionText ) throws RemoteException;

    public void setText(
                         String text,
                         String label,
                         String inGroup,
                         int index,
                         String textToSet ) throws RemoteException;

    public String getSelection(
                                String text,
                                String label,
                                String inGroup,
                                int index ) throws RemoteException;

    public int getSelectionIndex(
                                  String text,
                                  String label,
                                  String inGroup,
                                  int index ) throws RemoteException;

}
