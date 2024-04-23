package it.unipr.barbato.Model.Message;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.barbato.Interface.Message.Handler;
import it.unipr.barbato.Model.Utilities.RandomProb;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

public class ResourcesHandler implements Handler, MessageListener {
	private Integer pid = null;

	public void setPid(Integer pid) {
		this.pid = pid;
	}

	private Integer pidMaster = null;

	public void setPidMaster(Integer pidMaster) {
		this.pidMaster = pidMaster;
	}

	private boolean timeoutOccured = false;

	public boolean isTimeoutOccured() {
		return timeoutOccured;
	}

	public void setTimeoutOccured(boolean timeoutOccured) {
		this.timeoutOccured = timeoutOccured;
	}

	private Boolean master = null;

	public void setMaster(Boolean master) {
		this.master = master;
	}

	private Boolean down = false;

	public void setDown(Boolean down) {
		this.down = down;
	}
	
	private RandomProb random;
	/**
	 * The message handler.
	 */
	private MessageHandlerImpl messageHandler = null;

	private int executedTaskCount = 0;
	private static final int TASKS = 100;

	private static final double getResourceProb = 0.7;

	private static final int timeout = 7000;
	private boolean responseReceived;

	/**
	 * Constructs a ResourcesHandler object with the specified MessageHandler.
	 *
	 * @param messageHandler the MessageHandler to use for sending and receiving
	 *                       messages
	 * @throws JMSException if there is an error with the JMS connection
	 */
	public ResourcesHandler(ActiveMQConnectionFactory cf) throws JMSException {
		this.messageHandler = new MessageHandlerImpl(cf);
		this.messageHandler.start();
	}

	@Override
	public void start() throws JMSException {
		this.random = new RandomProb(this.pid.longValue());
		this.messageHandler.setOnReceive(this);
		this.messageHandler.receive("resource_" + this.pid);
	}

	public void execute() throws InterruptedException, JMSException {
		if (this.down)
			return;
		if (this.master == null || this.pidMaster == null)
			return;
		if (!this.master) {
			this.messageHandler.send("resource_" + this.pidMaster, this.pid, RequestType.resourceRequest);
			// Wait for the response or timeout
			waitForResponseOrTimeout();

			// Process the response
			if (this.responseReceived) {
				this.responseReceived = false; // Reset flag
			} else {
				System.out.println("Timeout occurred. No response received. Start election.");
				this.setMaster(null);
				this.setPidMaster(null);
				this.timeoutOccured = true;
			}
		}
	}

	@Override
	public void close() throws JMSException {
		this.messageHandler.close();
	}

	private synchronized void setResponse() {
		this.responseReceived = true;
		notify();
	}

	private synchronized void waitForResponseOrTimeout() throws InterruptedException {
		if (!this.responseReceived) {
			wait(timeout);
		}
	}

	public boolean endExecution() {
		return this.executedTaskCount < ResourcesHandler.TASKS;
	}

	@SuppressWarnings("incomplete-switch")
	@Override
	public void onMessage(Message message) {
		if (this.down)
			return;
		if (this.master == null || this.pidMaster == null)
			return;
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Serializable rh = objectMessage.getObject();
				RequestType rt = (RequestType) ((RequestHandler) rh).type;
				switch (rt) {
				case resourceRequest:
					if (this.master) {
						Integer pid = (Integer) ((RequestHandler) rh).obj;
						String resPid = "resource_" + pid;
						Boolean response = this.random.exec(ResourcesHandler.getResourceProb);
						this.messageHandler.send(resPid, response, RequestType.resourceAnswer);
					}
					break;
				case resourceAnswer:
					this.setResponse();
					// Formatting of current time to check the nodes coordination
					LocalTime currentTime = LocalTime.now();
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
					String formattedTime = currentTime.format(formatter);
					Boolean response = (Boolean) ((RequestHandler) rh).obj;
					System.out.println("Master " + this.pidMaster + " response: " + response + " " + formattedTime);
					if (response) {
						this.executedTaskCount++;
						System.out.println("Executed task: " + this.executedTaskCount + "/" + ResourcesHandler.TASKS);
					} else {
						Thread.sleep(100);
					}
					break;
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
