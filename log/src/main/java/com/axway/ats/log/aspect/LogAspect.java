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
package com.axway.ats.log.aspect;

import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;

import com.axway.ats.core.log.AtsConsoleLogger;
import com.axway.ats.log.appenders.AbstractDbAppender;

@Aspect
public class LogAspect {

    @AfterReturning( value = "call(Thread.new(..))", returning = "thread")
    public void afterThreadConstructorExecuted( Thread thread ) {

        Thread parentThread = Thread.currentThread();
        new AtsConsoleLogger(getClass()).info("Thread created [Name: " + thread.getName() + ", ID: " + thread.getId()
                                              + "]. Its parent is [Name: " + parentThread.getName() + ", ID: "
                                              + parentThread.getId() + "]");
        AbstractDbAppender.childParentThreadsMap.put(thread.getId() + "", parentThread.getId() + "");
    }

}
