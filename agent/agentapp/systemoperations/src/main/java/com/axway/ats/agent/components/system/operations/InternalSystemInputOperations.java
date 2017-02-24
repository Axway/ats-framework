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
import com.axway.ats.agent.core.model.Parameter;
import com.axway.ats.core.system.LocalSystemOperations;

public class InternalSystemInputOperations {

    private LocalSystemOperations localSystemOperations = new LocalSystemOperations();

    @Action(name = "Internal System Input Operations click At")
    public void clickAt(
                         @Parameter(name = "x") int x,
                         @Parameter(name = "y") int y ) {

        localSystemOperations.getInputOperations().clickAt( x, y );
    }

    @Action(name = "Internal System Input Operations type")
    public void type(
                      @Parameter(name = "text") String text ) {

        localSystemOperations.getInputOperations().type( text );
    }

    @Action(name = "Internal System Input Operations type")
    public void type(
                      @Parameter(name = "keyCodes") int... keyCodes ) {

        localSystemOperations.getInputOperations().type( keyCodes );
    }

    @Action(name = "Internal System Input Operations type")
    public void type(
                      @Parameter(name = "text") String text,
                      @Parameter(name = "keyCodes") int... keyCodes ) {

        localSystemOperations.getInputOperations().type( text, keyCodes );
    }

    @Action(name = "Internal System Input Operations press Alt F4")
    public void pressAltF4() {

        localSystemOperations.getInputOperations().pressAltF4();
    }

    @Action(name = "Internal System Input Operations press Esc")
    public void pressEsc() {

        localSystemOperations.getInputOperations().pressEsc();
    }

    @Action(name = "Internal System Input Operations press Enter")
    public void pressEnter() {

        localSystemOperations.getInputOperations().pressEnter();
    }

    @Action(name = "Internal System Input Operations press Space")
    public void pressSpace() {

        localSystemOperations.getInputOperations().pressSpace();
    }

    @Action(name = "Internal System Input Operations press Tab")
    public void pressTab() {

        localSystemOperations.getInputOperations().pressTab();
    }

    @Action(name = "Internal System Input Operations key Press")
    public void keyPress(
                          @Parameter(name = "keyCode") int keyCode ) {

        localSystemOperations.getInputOperations().keyPress( keyCode );
    }

    @Action(name = "Internal System Input Operations key Release")
    public void keyRelease(
                            @Parameter(name = "keyCode") int keyCode ) {

        localSystemOperations.getInputOperations().keyRelease( keyCode );
    }
}
