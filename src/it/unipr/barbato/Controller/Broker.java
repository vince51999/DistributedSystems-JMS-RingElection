package it.unipr.barbato.Controller;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.broker.BrokerFactory;
import org.apache.activemq.broker.BrokerService;

/**
 * The {@code Broker} class represents a message broker that handles the communication between nodes of distributed system.
 * It creates and starts an ActiveMQ broker, establishes a connection, and starts the communication.
 * 
 * @author Vincenzo Barbato 345728
 */
public class Broker {
	/**
	 * The broker URL.
	 */
	private static final String BROKER_URL = "tcp://localhost:61616";
	/**
	 * The broker properties.
	 */
	private static final String BROKER_PROPS = "persistent=false&useJmx=false";

	/**
	 * The main method of the Broker class.
	 * It creates and starts an ActiveMQ broker, establishes a connection, and starts the communication.
	 *
	 * @param args the command-line arguments
	 * @throws Exception if an error occurs during the execution
	 */
	public static void main(String[] args) throws Exception {

		BrokerService broker = BrokerFactory.createBroker("broker:(" + BROKER_URL + ")?" + BROKER_PROPS);

		broker.start();

		ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);
		cf.setTrustAllPackages(true);

		ActiveMQConnection connection = (ActiveMQConnection) cf.createConnection();

		connection.start();
	}

}
