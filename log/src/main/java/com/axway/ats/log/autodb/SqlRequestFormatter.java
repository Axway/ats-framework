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
package com.axway.ats.log.autodb;

public class SqlRequestFormatter {

    StringBuilder msg;

    public SqlRequestFormatter() {

        StackTraceElement callingMethod = Thread.currentThread().getStackTrace()[2];
        String methodName = callingMethod.getMethodName();

        msg = new StringBuilder( "\ncalled " + methodName );
    }

    public SqlRequestFormatter add(
                                    String paramName,
                                    Object paramValue ) {

        msg.append( "\n\t" + paramName );

        if( paramName.length() < 6 ) {
            msg.append( ":\t\t" + paramValue );
        } else {
            msg.append( ":\t" + paramValue );
        }

        return this;
    }

    public String format() {

        return msg.toString();
    }
}
