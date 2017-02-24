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
package com.axway.ats.agent.core.model;

import com.axway.ats.agent.core.exceptions.InternalComponentException;

/**
 * This interface may be implemented by the component
 * under test, it will be used when component is loaded by
 * the framework server
 */
public interface InitializationHandler {

    /**
     * This method is called at the time the component 
     * is loaded. Any component specific initialization 
     * should be implemented here.
     */
    public void initializeComponent() throws InternalComponentException;
}
