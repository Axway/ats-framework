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
package com.axway.ats.agent.core.action;

import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.agent.core.model.TemplateAction;

public class SampleTemplateActionClass {

    @TemplateAction(name = "template action 1")
    public Integer templateAction1(
                                    @Parameter(name = "arg0") int arg0,
                                    @Parameter(name = "arg1") Byte arg1,
                                    @Parameter(name = "arg2") String arg2 ) {

        return 3;
    }
}
