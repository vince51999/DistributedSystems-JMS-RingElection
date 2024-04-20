
package it.unipr.barbato.Model.Message;

/**
 * The {@code RequestType} enum represents the types of requests that can be made.
 * It includes the options for subscribing and offering.
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
	add_pid_to_list,
	resourceRequest,
	resourceAnswer,
	election,
	ack,
}
