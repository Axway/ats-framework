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
package com.axway.ats.action.mail;

import org.junit.Test;

import com.axway.ats.action.BaseTest;
import com.axway.ats.action.mail.MailTlsSender;
import com.axway.ats.action.model.ActionException;

public class Test_SmtpTLSSender extends BaseTest {

    @Test
    public void sendAndGetDelivered() throws ActionException {

        new MailTlsSender( Test_SmtpTLSSender.class );
    }
}
