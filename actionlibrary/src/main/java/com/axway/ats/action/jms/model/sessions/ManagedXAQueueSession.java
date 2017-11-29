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
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.XAQueueSession;
import javax.transaction.xa.XAResource;

public class ManagedXAQueueSession extends ManagedSession implements XAQueueSession {

    private final XAQueueSession xaQueueSession;

    public ManagedXAQueueSession( final XAQueueSession session ) {

        super( session );
        xaQueueSession = session;
    }

    @Override
    public Session getSession() throws JMSException {

        return addSession( xaQueueSession.getSession() );
    }

    @Override
    public XAResource getXAResource() {

        return xaQueueSession.getXAResource();
    }

    @Override
    public QueueSession getQueueSession() throws JMSException {

        return addSession( xaQueueSession.getQueueSession() );
    }
}
