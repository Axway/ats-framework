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
import javax.jms.Topic;
import javax.jms.TopicPublisher;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;

public class ManagedTopicSession extends ManagedSession implements TopicSession {

    private final TopicSession topicSession;

    public ManagedTopicSession( final TopicSession session ) {

        super( session );
        topicSession = session;
    }

    @Override
    public TopicSubscriber createSubscriber(
                                             Topic topic ) throws JMSException {

        return addConsumer( topicSession.createSubscriber( topic ) );
    }

    @Override
    public TopicSubscriber createSubscriber(
                                             Topic topic,
                                             String messageSelector,
                                             boolean noLocal ) throws JMSException {

        return addConsumer( topicSession.createSubscriber( topic, messageSelector, noLocal ) );
    }

    @Override
    public TopicPublisher createPublisher(
                                           Topic topic ) throws JMSException {

        return addProducer( topicSession.createPublisher( topic ) );
    }
}
