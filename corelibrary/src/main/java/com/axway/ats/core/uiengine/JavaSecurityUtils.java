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

import java.io.File;
import java.io.FileWriter;

import com.axway.ats.common.systemproperties.AtsSystemProperties;
import com.axway.ats.core.utils.IoUtils;

public class JavaSecurityUtils {

    public static void unlockJavaSecurity() {

        try {
            String userTempDir = AtsSystemProperties.SYSTEM_USER_TEMP_DIR;
            File tempPolicyFile = new File( userTempDir + "/ats_java_security_file.policy" );
            if( !tempPolicyFile.exists() ) {
                FileWriter fileWriter = null;
                try {
                    fileWriter = new FileWriter( tempPolicyFile );
                    fileWriter.append( "grant{\n" + "\t// Allow everything\n"
                                       + "\tpermission java.security.AllPermission;\n" + "};\n" );
                } finally {
                    IoUtils.closeStream( fileWriter );
                }
            }

            System.setProperty( "java.security.policy", tempPolicyFile.getAbsolutePath() );
        } catch( Exception e ) {
            throw new RuntimeException( "Error unlocking java security", e );
        }
    }
}
