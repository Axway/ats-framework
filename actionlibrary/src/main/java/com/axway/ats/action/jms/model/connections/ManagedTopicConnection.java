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
import javax.jms.ServerSessionPool;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.TopicSession;

public class ManagedTopicConnection extends ManagedConnection implements TopicConnection {

    private final TopicConnection topicConnection;

    public ManagedTopicConnection( TopicConnection connection ) {

        super( connection );
        this.topicConnection = connection;
    }

    @Override
    public TopicSession createTopicSession(
                                            boolean transacted,
                                            int acknowledgeMode ) throws JMSException {

        return addSession( topicConnection.createTopicSession( transacted, acknowledgeMode ) );
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(
                                                        Topic topic,
                                                        String messageSelector,
                                                        ServerSessionPool sessionPool,
                                                        int maxMessages ) throws JMSException {

        return addConnectionConsumer( topicConnection.createConnectionConsumer( topic,
                                                                                messageSelector,
                                                                                sessionPool,
                                                                                maxMessages ) );
    }
}
