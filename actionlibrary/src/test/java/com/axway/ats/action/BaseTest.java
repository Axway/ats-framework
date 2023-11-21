/*
 * Copyright 2017-2021 Axway Software
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
package com.axway.ats.action;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

public class BaseTest {

    static {
        ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%-5p %d{HH:MM:ss} %c{2}: %m%n"));

        //init log4j
        BasicConfigurator.configure(appender);
        // limit too verbose output as default severity is DEBUG
        Logger.getLogger(com.axway.ats.action.filesystem.snapshot.Test_FileSystemSnapshot.class).setLevel(Level.INFO);
        Logger.getLogger(com.axway.ats.action.objects.Test_MimePackage.class).setLevel(Level.INFO);
        Logger.getLogger(com.axway.ats.action.objects.MimePackage.class).setLevel(Level.INFO);
        Logger.getLogger(com.axway.ats.core.validation.Validator.class).setLevel(Level.INFO);

        // More strict limit for faster build by default
        Logger.getRootLogger().setLevel(Level.INFO);
    }

}
