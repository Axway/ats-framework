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
package com.axway.ats.core.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Formatting exceptions to plain String
 */
public class ExceptionUtils {

    /**
    * @param e the exception to parse
    * @return a formatted string representing the provided exception
    */
    public static String getExceptionMsg( Throwable e ) {

        return getExceptionMsg(e, null);
    }

    /**
    * @param e the exception to parse
    * @param usrMsg an additional message passed by user
    * @return a formatted string representing the provided exception
    */
    public static String getExceptionMsg( Throwable e, String usrMsg ) {

        StringBuilder msg = new StringBuilder();
        if (e != null) {
            msg.append("EXCEPTION\n");

            if (!StringUtils.isNullOrEmpty(usrMsg)) {
                msg.append("USER message:\n\t");
                msg.append(usrMsg);
                msg.append("\n");
            }

            if (null != e.getCause()) {
                msg.append("Cause:\n\t");
                msg.append(e.getCause().toString());
                msg.append("\n");
            }

            if (null != e.getMessage()) {
                msg.append("Message:\n\t");
                msg.append(getMsgLines(e.getMessage()));
                msg.append("\n");
            } else if (null != e.getLocalizedMessage()) {
                msg.append("Message:\n\t");
                msg.append(getMsgLines(e.getLocalizedMessage()));
                msg.append("\n");
            } else {
                msg.append("Message: ");
                msg.append("null");
                msg.append("\n");
            }

            msg.append("TYPE:\n\t");
            msg.append(e.getClass().toString());

            msg.append("\nCALL STACK:\n");
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            msg.append(sw.toString());
        }
        return msg.toString();
    }

    private static StringBuilder getMsgLines( String msg ) {

        StringBuilder resMsg = new StringBuilder();
        String[] msgLines = msg.split("\n");
        for (int i = 0; i < msgLines.length; i++) {
            resMsg.append(msgLines[i]);
            resMsg.append("\n\t");
        }
        return resMsg;
    }

    /**
     * Check if exception contains certain message
     * @param message the exception message as java.lang.String
     * @param exception the exception
     * */
    public static boolean containsMessage( String message, Exception exception ) {

        return containsMessage(message, exception, true);
    }

    /**
     * Check if exception contains certain message
     * @param message the exception message as java.lang.String
     * @param exception the exception
     * @param deepSearch whether to search for the message in the entire stack trace (true) or just the top most exception (false)
     * */
    public static boolean containsMessage( String message, Exception exception, boolean deepSearch ) {

        Throwable th = exception;
        while (th != null) {
            String errMsg = th.getMessage();
            if (!StringUtils.isNullOrEmpty(errMsg)) {
                if (errMsg.contains(message)) {
                    return true;
                }
            }
            if (!deepSearch) {
                return false;
            }
            th = th.getCause();
        }
        return false;
    }
}
