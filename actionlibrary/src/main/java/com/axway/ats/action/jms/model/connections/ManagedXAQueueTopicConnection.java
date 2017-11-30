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
package com.axway.ats.action.jms.model.connections;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.TopicConnection;
import javax.jms.XAConnection;
import javax.jms.XAQueueConnection;
import javax.jms.XAQueueSession;
import javax.jms.XASession;
import javax.jms.XATopicConnection;
import javax.jms.XATopicSession;

public class ManagedXAQueueTopicConnection extends ManagedQueueTopicConnection implements QueueConnection,
        TopicConnection, Connection, XAConnection, XAQueueConnection, XATopicConnection {

    public ManagedXAQueueTopicConnection( final Connection connection ) {

        super(connection);
    }

    @Override
    public XASession createXASession() throws JMSException {

        return addSession( ((XAConnection) connection).createXASession());
    }

    @Override
    public XAQueueSession createXAQueueSession() throws JMSException {

        return addSession( ((XAQueueConnection) connection).createXAQueueSession());
    }

    @Override
    public XATopicSession createXATopicSession() throws JMSException {

        return addSession( ((XATopicConnection) connection).createXATopicSession());
    }
}
