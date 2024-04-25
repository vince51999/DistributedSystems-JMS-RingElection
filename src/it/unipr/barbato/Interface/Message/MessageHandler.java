package it.unipr.barbato.Interface.Message;

import java.io.Serializable;
import it.unipr.barbato.Model.Message.RequestType;
import jakarta.jms.JMSException;
import jakarta.jms.MessageListener;
import jakarta.jms.QueueSession;

/**
 * The {@code MessageHandler} interface provides methods for interacting with
 * JMS messages.
 * 
 * @author Vincenzo Barbato 345728
 */
public interface MessageHandler {
	/**
	 * Returns the QueueSession associated with this MessageHandler.
	 *
	 * @return the QueueSession object.
	 */
	public QueueSession getQueueSession();

	/**
	 * Publishes a message to the specified topic.
	 *
	 * @param topic_name the name of the topic to publish the message to.
	 * @param obj        the Serializable object to be published.
	 * @param type       the type of the request.
	 * @throws JMSException if there is an error while publishing the message.
	 */
	public void publish(String topic_name, Serializable obj, RequestType type) throws JMSException;

	/**
	 * Subscribes to the specified topic and sets the MessageListener for receiving
	 * messages.
	 *
	 * @param topic_name the name of the topic to subscribe to.
	 * @throws JMSException if there is an error while subscribing to the topic.
	 */
	public void subscribe(String topic_name) throws JMSException;

	/**
	 * Receives messages from the specified queue and sets the MessageListener for
	 * processing received messages.
	 *
	 * @param queue_name the name of the queue to receive messages from.
	 * @throws JMSException if there is an error while receiving messages from the
	 *                      queue.
	 */
	public void receive(String queue_name) throws JMSException;

	/**
	 * Sends a message to the specified queue and sets the MessageListener for
	 * processing the response.
	 *
	 * @param queue_name the name of the queue to send the message to.
	 * @param obj        the Serializable object to be sent.
	 * @param type       the type of the request.
	 * @throws JMSException if there is an error while sending the message.
	 */
	public void send(String queue_name, Serializable obj, RequestType type) throws JMSException;

	/**
	 * Sets the MessageListener for subscribing to messages.
	 *
	 * @param messageListener the MessageListener object.
	 */
	public void setOnSubscribe(MessageListener messageListener);

	/**
	 * Sets the MessageListener for receiving messages.
	 *
	 * @param customOnMessage the MessageListener object.
	 */
	public void setOnReceive(MessageListener customOnMessage);

	/**
	 * Sets the MessageListener for sending messages.
	 *
	 * @param customOnMessage the MessageListener object.
	 */
	public void setOnSend(MessageListener customOnMessage);
}
