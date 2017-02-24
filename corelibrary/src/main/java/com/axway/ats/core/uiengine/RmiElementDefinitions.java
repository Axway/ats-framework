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
package com.axway.ats.core.uiengine;

/**
 * Each RMI element must have a unique name as this name is used when
 * registering with the RMI system
 */
public class RmiElementDefinitions {

    public static final String RMI_SWT_BUTTON            = "RMI_SWT_BUTTON";

    public static final String RMI_SWT_LINK              = "RMI_SWT_LINK";

    public static final String RMI_SWT_TOOLBAR_BUTTON    = "RMI_SWT_TOOLBAR_BUTTON";

    public static final String RMI_SWT_MENU              = "RMI_SWT_MENU";

    public static final String RMI_SWT_TEXT_BOX          = "RMI_SWT_TEXT_BOX";

    public static final String RMI_SWT_LIST              = "RMI_SWT_LIST";

    public static final String RMI_SWT_LABEL             = "RMI_SWT_LABEL";

    public static final String RMI_SWT_TREE              = "RMI_SWT_TREE";

    public static final String RMI_SWT_TABLE             = "RMI_SWT_TABLE";

    public static final String RMI_SWT_CHECKBOX          = "RMI_SWT_CHECKBOX";

    public static final String RMI_SWT_COMBOBOX          = "RMI_SWT_COMBOBOX";

    public static final String RMI_SWT_SHELL             = "RMI_SWT_SHELL";

    public static final String RMI_SWT_FTD_PLUGIN        = "RMI_SWT_FTD_PLUGIN";

    public static final String RMI_SWT_GENERAL_ELEMENT   = "RMI_SWT_GENERAL_ELEMENT";

    public static final String RMI_LOTUS_MAIL            = "RMI_LOTUS_MAIL";

    public static final String RMI_LOTUS_MAIL_SERVER     = "RMI_LOTUS_MAIL_SERVER";

    public static final int    RMI_PORT_FOR_LOTUS_PLUGIN = 1100;
    public static final int    RMI_PORT_FOR_SWT_PLUGIN   = 1101;
}
