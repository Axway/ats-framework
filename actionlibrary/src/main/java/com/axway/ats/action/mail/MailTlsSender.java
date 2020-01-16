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

import com.axway.ats.action.mail.model.BasicSslSocketFactory;
import com.axway.ats.common.PublicAtsApi;

/**
 * TLS Implementation for sending MIME packages
 */
@PublicAtsApi
public class MailTlsSender extends MailSender {

    /**
     * A TLS mail sender which accepts any SSL certificate<br>
     * Uses default SSLSocketFactory provided by ATS: {@link BasicSslSocketFactory}
     *
     */
    @PublicAtsApi
    public MailTlsSender() {

        this(BasicSslSocketFactory.class);
    }

    /**
     * A TLS mail sender which uses a user provided SSL socket factory class
     * in order to set the JavaMail TLS settings
     *
     * @param sslSocketFactoryClass the SSL socket factory class
     */
    @PublicAtsApi
    public MailTlsSender( Class<?> sslSocketFactoryClass ) {

        super();

        /*
         * Mail session properties SMTP reference:
         *  https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html
         */
        mailProperties.put("mail.smtp.starttls.enable", "true");
        // mailProperties.put("mail.smtp.ssl.protocols","TLSv1.2");
        mailProperties.put("mail.smtp.socketFactory.fallback", "false");
        mailProperties.put("mail.smtp.ssl.socketFactory.class", sslSocketFactoryClass.getCanonicalName());
    }
}
