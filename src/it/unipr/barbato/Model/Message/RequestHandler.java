package it.unipr.barbato.Model.Message;

import java.io.Serializable;

/**
 * The {@code RequestHandler} class represents a request handler object.
 * It is used to encapsulate a serializable object and its corresponding request type.
 * 
 *  @author Vincenzo Barbato 345728
 */
public class RequestHandler implements Serializable {

	/**
	 * The serial version UID.
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * The serializable object to be encapsulated.
	 */
	public Serializable obj = null;
	/**
	 * The request type associated with the object.
	 */
	public RequestType type = null;

	/**
	 * Constructs a RequestHandler object with the specified serializable object and request type.
	 *
	 * @param obj  the serializable object to be encapsulated
	 * @param type the request type associated with the object
	 */
	public RequestHandler(Serializable obj, RequestType type) {
		this.obj = obj;
		this.type = type;
	}
}


