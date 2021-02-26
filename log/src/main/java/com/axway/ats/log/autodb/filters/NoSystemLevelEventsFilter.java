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
package com.axway.ats.log.autodb.filters;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.filter.AbstractFilter;

import com.axway.ats.log.model.SystemLogLevel;

/**
 * Finer logging of ATS logging DB &quot;SYSTEM level&quot; events
*/
@Plugin( name = "NoSystemLevelEventsFilter", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE, printObject = true)
public class NoSystemLevelEventsFilter extends AbstractFilter {

    public NoSystemLevelEventsFilter() {

    }

    @Override
    public Filter.Result filter( LogEvent event ) {

        return (event.getLevel().intLevel() == SystemLogLevel.SYSTEM_INT)
                                                                          ? Result.DENY
                                                                          : Result.NEUTRAL;

    }

    @PluginFactory
    public static NoSystemLevelEventsFilter createFilter() {

        return new NoSystemLevelEventsFilter();
    }

}
