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
package com.axway.ats.log.autodb.entities;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Message extends DbEntity {

    private static final long       serialVersionUID = 1L;

    private static SimpleDateFormat dateFormat       = new SimpleDateFormat( "MMM dd" );
    private static SimpleDateFormat timeFormat       = new SimpleDateFormat( "HH:mm:ss:S" );

    public int                      messageId;
    public int                      parentMessageId;
    public String                   messageContent;
    public String                   messageType;

    public boolean                  escapeHtml;

    public String                   machineName;
    public String                   threadName;

    public String getDate() {

        return dateFormat.format( new Date( getStartTimestamp() ) );
    }

    public String getTime() {

        return timeFormat.format( new Date( getStartTimestamp() ) );
    }

}
