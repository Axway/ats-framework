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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.StreamMessage;
import javax.jms.TemporaryQueue;
import javax.jms.TemporaryTopic;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSession;
import javax.jms.TopicSubscriber;
import javax.jms.XAQueueSession;
import javax.jms.XATopicSession;

public class ManagedSession implements Session {
    final Session session;
    private List<ManagedSession> sessions = new ArrayList<ManagedSession>();
    private List<MessageConsumer> consumers = new ArrayList<MessageConsumer>();
    private List<MessageProducer> producers = new ArrayList<MessageProducer>();
    private List<QueueBrowser> browsers = new ArrayList<QueueBrowser>();

    public static ManagedSession create(Session session) {
        if ((session instanceof XAQueueSession) && (session instanceof XATopicSession))
            return new ManagedXAQueueTopicSession(session);
        if (session instanceof XAQueueSession)
            return new ManagedXAQueueSession((XAQueueSession) session);
        if (session instanceof XATopicSession)
            return new ManagedXATopicSession((XATopicSession) session);
        if ((session instanceof QueueSession) && (session instanceof TopicSession))
            return new ManagedQueueTopicSession(session);
        if (session instanceof QueueSession)
            return new ManagedQueueSession((QueueSession) session);
        if (session instanceof TopicSession)
            return new ManagedTopicSession((TopicSession) session);

        return new ManagedSession(session);
    }

    public ManagedSession(final Session session) {
        this.session = session;
    }

    protected synchronized <T extends MessageConsumer> T addConsumer(T cnsumer) {
        consumers.add(cnsumer);
        return cnsumer;
    }

    protected synchronized <T extends MessageProducer> T addProducer(T producer) {
        producers.add(producer);
        return producer;
    }

    protected synchronized <T extends Session> T addSession(T session) {
        if (session != this)
            sessions.add(ManagedSession.create(session));
        return session;
    }

    protected synchronized QueueBrowser addBrowser(QueueBrowser browser) {
        browsers.add(browser);
        return browser;
    }

    public void shutdown() {
        try {
            this.close();
        } catch (final Exception e) { // ignored
        }
    }

    @Override
    public BytesMessage createBytesMessage() throws JMSException {
        return session.createBytesMessage();
    }

    @Override
    public MapMessage createMapMessage() throws JMSException {
        return session.createMapMessage();
    }

    @Override
    public Message createMessage() throws JMSException {
        return session.createMessage();
    }

    @Override
    public ObjectMessage createObjectMessage() throws JMSException {
        return session.createObjectMessage();
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
        return session.createObjectMessage(object);
    }

    @Override
    public StreamMessage createStreamMessage() throws JMSException {
        return session.createStreamMessage();
    }

    @Override
    public TextMessage createTextMessage() throws JMSException {
        return session.createTextMessage();
    }

    @Override
    public TextMessage createTextMessage(String text) throws JMSException {
        return session.createTextMessage(text);
    }

    @Override
    public boolean getTransacted() throws JMSException {
        return session.getTransacted();
    }

    @Override
    public int getAcknowledgeMode() throws JMSException {
        return session.getAcknowledgeMode();
    }

    @Override
    public void commit() throws JMSException {
        session.commit();
    }

    @Override
    public void rollback() throws JMSException {
        session.rollback();
    }

    @Override
    public synchronized void close() throws JMSException {
        for (final QueueBrowser b : browsers)
            try {
                b.close();
            } catch (final Exception e) {
            }
        browsers.clear();
        for (final MessageConsumer b : consumers)
            try {
                b.close();
            } catch (final Exception e) {
            }
        for (final MessageProducer b : producers)
            try {
                b.close();
            } catch (final Exception e) {
            }
        for (final ManagedSession b : sessions)
            b.shutdown();
        session.close();
    }

    @Override
    public void recover() throws JMSException {
        session.recover();
    }

    @Override
    public MessageListener getMessageListener() throws JMSException {
        return session.getMessageListener();
    }

    @Override
    public void setMessageListener(MessageListener listener) throws JMSException {
        session.setMessageListener(listener);
    }

    @Override
    public void run() {
        session.run();
    }

    @Override
    public MessageProducer createProducer(Destination destination) throws JMSException {
        return addProducer(session.createProducer(destination));
    }

    @Override
    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        return addConsumer(session.createConsumer(destination));
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector) throws JMSException {
        return addConsumer(session.createConsumer(destination, messageSelector));
    }

    @Override
    public MessageConsumer createConsumer(Destination destination, String messageSelector, boolean NoLocal) throws JMSException {
        return addConsumer(session.createConsumer(destination, messageSelector, NoLocal));
    }

    @Override
    public Queue createQueue(String queueName) throws JMSException {
        return session.createQueue(queueName);
    }

    @Override
    public Topic createTopic(String topicName) throws JMSException {
        return session.createTopic(topicName);
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name) throws JMSException {
        return addConsumer(session.createDurableSubscriber(topic, name));
    }

    @Override
    public TopicSubscriber createDurableSubscriber(Topic topic, String name, String messageSelector, boolean noLocal) throws JMSException {
        return addConsumer(session.createDurableSubscriber(topic, name, messageSelector, noLocal));
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        return addBrowser(session.createBrowser(queue));
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) throws JMSException {
        return addBrowser(session.createBrowser(queue, messageSelector));
    }

    @Override
    public TemporaryQueue createTemporaryQueue() throws JMSException {
        return session.createTemporaryQueue();
    }

    @Override
    public TemporaryTopic createTemporaryTopic() throws JMSException {
        return session.createTemporaryTopic();
    }

    @Override
    public void unsubscribe(String name) throws JMSException {
        session.unsubscribe(name);
    }

}
