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
package com.axway.ats.agent.components.system.operations;

import com.axway.ats.agent.core.model.Action;
import com.axway.ats.agent.core.model.ActionRequestInfo;
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.core.system.LocalSystemOperations;

public class InternalSystemInputOperations {

    private LocalSystemOperations localSystemOperations = null;

    @Action(
            name = "Internal System Input Operations initialize")
    @ActionRequestInfo(
            requestMethod = "PUT",
            requestUrl = "system/input")
    public void initialize() {

        localSystemOperations = new LocalSystemOperations();
    }

    @Action(
            name = "Internal System Input Operations click At")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/mouse/click")
    public void clickAt(
                         @Parameter(
                                 name = "x") int x,
                         @Parameter(
                                 name = "y") int y ) {

        localSystemOperations.getInputOperations().clickAt(x, y);
    }

    @Action(
            name = "Internal System Input Operations type")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/type")
    public void type(
                      @Parameter(
                              name = "text") String text ) {

        localSystemOperations.getInputOperations().type(text);
    }

    @Action(
            name = "Internal System Input Operations type")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/type")
    public void type(
                      @Parameter(
                              name = "keyCodes") int... keyCodes ) {

        localSystemOperations.getInputOperations().type(keyCodes);
    }

    @Action(
            name = "Internal System Input Operations type")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/type")
    public void type(
                      @Parameter(
                              name = "text") String text,
                      @Parameter(
                              name = "keyCodes") int... keyCodes ) {

        localSystemOperations.getInputOperations().type(text, keyCodes);
    }

    @Action(
            name = "Internal System Input Operations press Alt F4")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/key/alt/with/functional/4/press")
    public void pressAltF4() {

        localSystemOperations.getInputOperations().pressAltF4();
    }

    @Action(
            name = "Internal System Input Operations press Esc")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/key/escape/press")
    public void pressEsc() {

        localSystemOperations.getInputOperations().pressEsc();
    }

    @Action(
            name = "Internal System Input Operations press Enter")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/key/enter/press")
    public void pressEnter() {

        localSystemOperations.getInputOperations().pressEnter();
    }

    @Action(
            name = "Internal System Input Operations press Space")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/key/space/press")
    public void pressSpace() {

        localSystemOperations.getInputOperations().pressSpace();
    }

    @Action(
            name = "Internal System Input Operations press Tab")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/key/tab/press")
    public void pressTab() {

        localSystemOperations.getInputOperations().pressTab();
    }

    @Action(
            name = "Internal System Input Operations key Press")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/key/press")
    public void keyPress(
                          @Parameter(
                                  name = "keyCode") int keyCode ) {

        localSystemOperations.getInputOperations().keyPress(keyCode);
    }

    @Action(
            name = "Internal System Input Operations key Release")
    @ActionRequestInfo(
            requestMethod = "POST",
            requestUrl = "system/input/keyboard/key/release")
    public void keyRelease(
                            @Parameter(
                                    name = "keyCode") int keyCode ) {

        localSystemOperations.getInputOperations().keyRelease(keyCode);
    }
}
