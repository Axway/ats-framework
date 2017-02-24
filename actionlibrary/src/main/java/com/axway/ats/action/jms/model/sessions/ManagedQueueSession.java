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
package com.axway.ats.action.jms.model.sessions;

import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueReceiver;
import javax.jms.QueueSender;
import javax.jms.QueueSession;

public class ManagedQueueSession extends ManagedSession implements QueueSession {

    private final QueueSession queueSession;

    public ManagedQueueSession( final QueueSession session ) {

        super( session );
        queueSession = session;
    }

    @Override
    public QueueReceiver createReceiver(
                                         Queue queue ) throws JMSException {

        return addConsumer( queueSession.createReceiver( queue ) );
    }

    @Override
    public QueueReceiver createReceiver(
                                         Queue queue,
                                         String messageSelector ) throws JMSException {

        return addConsumer( queueSession.createReceiver( queue, messageSelector ) );
    }

    @Override
    public QueueSender createSender(
                                     Queue queue ) throws JMSException {

        return addProducer( queueSession.createSender( queue ) );
    }
}
