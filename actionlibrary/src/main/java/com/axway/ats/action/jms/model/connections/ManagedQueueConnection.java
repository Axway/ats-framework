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

import javax.jms.ConnectionConsumer;
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueSession;
import javax.jms.ServerSessionPool;

public class ManagedQueueConnection extends ManagedConnection implements QueueConnection {
    private final QueueConnection queueConnection;

    public ManagedQueueConnection( QueueConnection connection ) {

        super(connection);
        this.queueConnection = connection;
    }

    @Override
    public QueueSession createQueueSession(
                                            boolean transacted,
                                            int acknowledgeMode ) throws JMSException {

        return addSession(queueConnection.createQueueSession(transacted, acknowledgeMode));
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(
                                                        Queue queue,
                                                        String messageSelector,
                                                        ServerSessionPool sessionPool,
                                                        int maxMessages ) throws JMSException {

        return addConnectionConsumer(queueConnection.createConnectionConsumer(queue,
                                                                              messageSelector,
                                                                              sessionPool,
                                                                              maxMessages));
    }

}
