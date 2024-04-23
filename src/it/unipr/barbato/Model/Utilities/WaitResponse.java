package it.unipr.barbato.Model.Utilities;

/**
 * The {@code WaitResponse} class represents a wait response object.
 * It is used to wait for a response or a timeout.
 * 
 * @author Vincenzo Barbato 345728
 */
public class WaitResponse {
	/**
	 * The timeout value.
	 */
	private  int timeout;

	/**
	 * The response received flag.
	 */
	private boolean responseReceived;
	
	/**
	 * Returns the timeout value.
	 * 
	 * @return the timeout value
	 */
	public boolean isResponseReceived() {
		return responseReceived;
	}
	/**
	 * Set the response received flag.
	 * 
	 * @param responseReceived the response received flag
	 */
	public void setResponseReceived(boolean responseReceived) {
		this.responseReceived = responseReceived;
	}

	/**
	 * Constructs a new wait response with the specified timeout.
	 * 
	 * @param timeout the timeout value
	 */
	public WaitResponse(int timeout) {
		this.timeout = timeout;
	}
	
	/**
	 * Set the response flag.
	 */
	public synchronized void setResponse() {
		this.responseReceived = true;
		notify();
	}

	/**
	 * Wait for the response or timeout.
	 * 
	 * @throws InterruptedException if an error occurs during the execution
	 */
	public synchronized void waitForResponseOrTimeout() throws InterruptedException {
		if (!this.responseReceived) {
			wait(timeout);
		}
	}
}
