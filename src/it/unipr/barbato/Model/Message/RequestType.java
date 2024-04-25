
package it.unipr.barbato.Model.Message;

/**
 * The {@code RequestType} enum represents the types of requests that can be
 * made.
 * 
 * @author Vincenzo Barbato 345728
 */
public enum RequestType {
	/**
	 * The subscribe request type.
	 */
	subscribe,
	/**
	 * The un-subscribe request type.
	 */
	unsubscribe,
	/**
	 * The add PID to list request type.
	 */
	add_pid_to_list,
	/**
	 * The resourse request type. This type is used to request a resource to master
	 * node.
	 */
	resourceRequest,
	/**
	 * The resource answer request type. This type is used to send a resource answer
	 * to a node.
	 */
	resourceAnswer,
	/**
	 * The election request type. This type is used to start an election or update
	 * other nodes about the election.
	 */
	election,
	/**
	 * This type is used to send an acknowledgment.
	 */
	ack,
}
