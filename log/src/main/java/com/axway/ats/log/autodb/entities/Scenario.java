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

import java.io.Serializable;

public class Scenario implements Serializable {

    private static final long serialVersionUID = 1L;

    public String             scenarioId;
    public String             suiteId;
    public String             name;
    public String             description;

    public int                testcasesTotal;
    public int                testcasesFailed;
    public String             testcasesPassedPercent;
    public boolean            testcaseIsRunning;

    public String             dateStart;
    public String             dateEnd;
    public String             duration;
    public int                result;
    public String             state;

    public String             userNote;
}
