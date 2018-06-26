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
package com.axway.ats.agent.webapp.restservice.api.listeners;

import javax.servlet.ServletRequestEvent;
import javax.servlet.ServletRequestListener;

import com.axway.ats.agent.core.MainComponentLoader;

/**
 * This listener will check if Agent is currently in the process of deploying
 * actions and will hold the request until all fixtures have been deployed
 */
public class AgentRequestListener implements ServletRequestListener {

    /* (non-Javadoc)
     * @see javax.servlet.ServletRequestListener#requestDestroyed(javax.servlet.ServletRequestEvent)
     */
    @Override
    public void requestDestroyed( ServletRequestEvent servletRequestEvent ) {

        //nothing to do here
    }

    /* (non-Javadoc)
     * @see javax.servlet.ServletRequestListener#requestInitialized(javax.servlet.ServletRequestEvent)
     */
    @Override
    public void requestInitialized( ServletRequestEvent servletRequestEvent ) {

        //do not let the request go if currently loading Ageng components
        MainComponentLoader.getInstance().blockIfLoading();
    }
}
