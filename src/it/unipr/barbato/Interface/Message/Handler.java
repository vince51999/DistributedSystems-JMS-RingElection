
package it.unipr.barbato.Interface.Message;

import jakarta.jms.JMSException;

/**
 * The {@code Handler} interface represents a message handler that can start and close a process.
 * 
 * @author Vincenzo Barbato 345728
 */
public interface Handler {
	/**
	 * Starts the process.
	 *
	 * @throws JMSException if there is an error starting the process
	 */
	public void start() throws JMSException;

	/**
	 * Closes the process.
	 *
	 * @throws JMSException if there is an error closing the process
	 */
	public void close() throws JMSException;
}
