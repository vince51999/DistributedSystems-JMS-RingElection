package it.unipr.barbato.Model.Message;

import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.MessageProducer;
import jakarta.jms.ObjectMessage;
import jakarta.jms.Queue;
import jakarta.jms.QueueReceiver;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.Topic;
import jakarta.jms.TopicPublisher;
import jakarta.jms.TopicSession;
import jakarta.jms.TopicSubscriber;

import java.io.Serializable;
import java.lang.management.ManagementFactory;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.barbato.Interface.Message.Handler;
import it.unipr.barbato.Interface.Message.MessageHandler;

/**
 * The {@code MessageHandlerImpl} class implements the {@link Handler} and
 * {@link MessageHandler} interfaces and provides functionality for handling JMS
 * messages.
 * 
 * @author Vincenzo Barbato 345728
 */
public class MessageHandlerImpl implements Handler, MessageHandler {
	/**
	 * The message listener for the onSubscribe event.
	 */
	private MessageListener onSubscribe = null;
	/**
	 * The message listener for the onReceive event.
	 */
	private MessageListener onReceive = null;
	/**
	 * The message listener for the onSend event.
	 */
	private MessageListener onSend = null;
	/**
	 * The queue session.
	 */
	private QueueSession queueSession = null;
	/**
	 * The topic session.
	 */
	private TopicSession topicSession = null;
	/**
	 * The JMS connection.
	 */
	private ActiveMQConnection connection = null;

	/**
	 * Constructs a new MessageHandlerImpl object with the specified ActiveMQ
	 * 
	 * @param cf the ActiveMQ connection factory
	 * @throws JMSException if there is an error in creating or starting the JMS
	 *                      connection.
	 */
	public MessageHandlerImpl(ActiveMQConnectionFactory cf) throws JMSException {
		cf.setTrustAllPackages(true);
		connection = (ActiveMQConnection) cf.createConnection();
	}

	@Override
	public void start() throws JMSException {
		connection.start();
		queueSession = connection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
		topicSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	@Override
	public void close() throws JMSException {
		if (connection != null) {
			connection.close();
		}
	}

	@Override
	public QueueSession getQueueSession() {
		return this.queueSession;
	}

	@Override
	public void publish(String topic_name, Serializable obj, RequestType type) throws JMSException {
		topicSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

		Topic topic = topicSession.createTopic(topic_name);

		TopicPublisher publisher = topicSession.createPublisher(topic);

		ObjectMessage objectMessage = topicSession.createObjectMessage();
		objectMessage.setObject(new RequestHandler(obj, type));

		publisher.publish(objectMessage);

		publisher.publish(topicSession.createMessage());
	}

	@Override
	public void subscribe(String topic_name) throws JMSException {

		topicSession = connection.createTopicSession(false, Session.AUTO_ACKNOWLEDGE);

		Topic topic = topicSession.createTopic(topic_name);

		TopicSubscriber subscriber = topicSession.createSubscriber(topic);

		if (this.onSubscribe != null)
			subscriber.setMessageListener(onSubscribe);
	}

	@Override
	public void receive(String queue_name) throws JMSException {

		Queue queue = queueSession.createQueue(queue_name);

		QueueReceiver receiver = queueSession.createReceiver(queue);

		MessageConsumer consumer = queueSession.createConsumer(queue);
		if (this.onReceive != null)
			consumer.setMessageListener(onReceive);
		receiver.close();
	}

	@Override
	public void send(String queue_name, Serializable obj, RequestType type) throws JMSException {
		Destination serverQueue = queueSession.createQueue(queue_name);
		MessageProducer producer = queueSession.createProducer(serverQueue);

		Destination tempDest = queueSession.createTemporaryQueue();

		MessageConsumer consumer = queueSession.createConsumer(tempDest);

		ObjectMessage request = queueSession.createObjectMessage();

		request.setObject(new RequestHandler(obj, type));
		request.setJMSReplyTo(tempDest);
		String processName = ManagementFactory.getRuntimeMXBean().getName();
		request.setJMSCorrelationID(processName);

		producer.send(request);

		if (this.onSend != null)
			consumer.setMessageListener(onSend);

		producer.close();
	}

	@Override
	public void setOnSubscribe(MessageListener messageListener) {
		this.onSubscribe = messageListener;
	}

	@Override
	public void setOnReceive(MessageListener customOnMessage) {
		this.onReceive = customOnMessage;
	}

	@Override
	public void setOnSend(MessageListener customOnMessage) {
		this.onSend = customOnMessage;
	}
}
