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

import java.util.ArrayList;
import java.util.List;

import javax.jms.Connection;
import javax.jms.ConnectionConsumer;
import javax.jms.ConnectionMetaData;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.ServerSessionPool;
import javax.jms.Session;
import javax.jms.Topic;
import javax.jms.TopicConnection;
import javax.jms.XAQueueConnection;
import javax.jms.XATopicConnection;

import com.axway.ats.action.jms.model.sessions.ManagedSession;

public class ManagedConnection implements Connection {
    final Connection                       connection;
    private final List<ManagedSession>     sessions            = new ArrayList<ManagedSession>();
    private final List<ConnectionConsumer> connectionConsumers = new ArrayList<ConnectionConsumer>();

    public static ManagedConnection create(
                                            final Connection connection ) {

        if ( (connection instanceof XAQueueConnection) && (connection instanceof XATopicConnection)) {
            return new ManagedXAQueueTopicConnection(connection);
        } else if (connection instanceof XAQueueConnection) {
            return new ManagedXAQueueConnection((XAQueueConnection) connection);
        } else if (connection instanceof XATopicConnection) {
            return new ManagedXATopicConnection((XATopicConnection) connection);
        } else if ( (connection instanceof QueueConnection) && (connection instanceof TopicConnection)) {
            return new ManagedQueueTopicConnection(connection);
        } else if (connection instanceof QueueConnection) {
            return new ManagedQueueConnection((QueueConnection) connection);
        } else if (connection instanceof TopicConnection) {
            return new ManagedTopicConnection((TopicConnection) connection);
        } else {
            return new ManagedConnection(connection);
        }
    }

    public ManagedConnection( final Connection connection ) {

        this.connection = connection;
    }

    public void shutdown() {

        try {
            this.close();
        } catch (final Exception e) {
            // Ignored
        }
    }

    @SuppressWarnings( "unchecked")
    protected synchronized <T extends Session> T addSession(
                                                             final T session ) {

        final ManagedSession managedSession = ManagedSession.create(session);
        sessions.add(managedSession);
        return (T) managedSession;
    }

    protected synchronized ConnectionConsumer addConnectionConsumer(
                                                                     ConnectionConsumer connectionConsumer ) {

        connectionConsumers.add(connectionConsumer);
        return connectionConsumer;
    }

    // JMS override
    @Override
    public Session createSession(
                                  boolean transacted,
                                  int acknowledgeMode ) throws JMSException {

        return addSession(connection.createSession(transacted, acknowledgeMode));
    }

    @Override
    public String getClientID() throws JMSException {

        return connection.getClientID();
    }

    @Override
    public void setClientID(
                             String clientID ) throws JMSException {

        connection.setClientID(clientID);
    }

    @Override
    public ConnectionMetaData getMetaData() throws JMSException {

        return connection.getMetaData();
    }

    @Override
    public ExceptionListener getExceptionListener() throws JMSException {

        return connection.getExceptionListener();
    }

    @Override
    public void setExceptionListener(
                                      ExceptionListener listener ) throws JMSException {

        connection.setExceptionListener(listener);
    }

    @Override
    public void start() throws JMSException {

        connection.start();
    }

    @Override
    public void stop() throws JMSException {

        connection.stop();
    }

    @Override
    public synchronized void close() throws JMSException {

        for (final ConnectionConsumer c : connectionConsumers)
            try {
                c.close();
            } catch (final Exception e) {}
        connectionConsumers.clear();
        for (final ManagedSession session : sessions)
            session.shutdown();
        sessions.clear();
        try {
            this.stop();
        } catch (final Exception e) {}
        connection.close();
    }

    @Override
    public ConnectionConsumer createConnectionConsumer(
                                                        Destination destination,
                                                        String messageSelector,
                                                        ServerSessionPool sessionPool,
                                                        int maxMessages ) throws JMSException {

        return connection.createConnectionConsumer(destination, messageSelector, sessionPool, maxMessages);
    }

    @Override
    public ConnectionConsumer createDurableConnectionConsumer(
                                                               Topic topic,
                                                               String subscriptionName,
                                                               String messageSelector,
                                                               ServerSessionPool sessionPool,
                                                               int maxMessages ) throws JMSException {

        return connection.createDurableConnectionConsumer(topic,
                                                          subscriptionName,
                                                          messageSelector,
                                                          sessionPool,
                                                          maxMessages);
    }
}
