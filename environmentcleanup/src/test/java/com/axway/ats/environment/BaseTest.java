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
package com.axway.ats.environment;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.axway.ats.environment.EnvironmentUnit;

public class BaseTest {
    private final static Logger LOG = Logger.getLogger(BaseTest.class);

    static {
        ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%-5p %d{HH:MM:ss} %c{2}: %m%n"));

        //init log4j
        BasicConfigurator.configure(appender);
    }

    protected String getTempBackupDir(
                                       EnvironmentUnit env ) throws Exception {

        Field privateField = EnvironmentUnit.class.getDeclaredField("tempBackupDir");
        privateField.setAccessible(true);
        String tempBackupDir = (String) privateField.get(env);
        privateField.setAccessible(false);

        return tempBackupDir;
    }

    protected void deleteFolder(
                                 File path ) throws IOException {

        LOG.debug("Trying to delete entry " + path.getAbsolutePath());
        if (path.isFile()) {
            if (!path.delete()) {
                LOG.error("Could not delete file entry " + path.getAbsolutePath());
            }
        } else {
            File[] files = path.listFiles();
            if (files != null) {
                for (int i = 0; i < files.length; ++i) {
                    if (files[i].isDirectory()) {
                        deleteFolder(files[i]);
                    }
                    files[i].delete();
                }
                if (!path.delete()) {
                    LOG.error("Could not delete dir. entry " + path.getAbsolutePath());
                }
            }
        }
    }

}
