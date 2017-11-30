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
package com.axway.ats.uiengine;

import com.axway.ats.common.PublicAtsApi;
import com.axway.ats.uiengine.engine.RobotEngine;

/**
 * A driver operating over Java Robot
 */
@PublicAtsApi
public class RobotDriver extends UiDriver {

    /**
     * To get RobotDriver instance use UiDriver.getRobotDriver()
     */
    protected RobotDriver() {

    }

    /**
     * 
     * @return instance of engine operating over Java Robot
     */
    @PublicAtsApi
    public RobotEngine getRobotEngine() {

        return new RobotEngine(this);
    }

    @Override
    @PublicAtsApi
    public void start() {

    }

    @Override
    @PublicAtsApi
    public void stop() {

    }
}
