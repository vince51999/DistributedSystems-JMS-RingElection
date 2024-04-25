package it.unipr.barbato.Model.Message;

import java.io.Serializable;

import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.barbato.Interface.Message.Handler;
import it.unipr.barbato.Model.Utilities.Print;
import it.unipr.barbato.Model.Utilities.RandomProb;
import it.unipr.barbato.Model.Utilities.WaitResponse;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

/**
 * The {@code ResourcesHandler} class implements the {@link Handler} and
 * {@link MessageListener} interfaces and represents a resources handler in the
 * distributed system. It sends and receives resources messages and waits for
 * responses.
 * 
 * @author Vincenzo Barbato 345728
 */
public class ResourcesHandler implements Handler, MessageListener {
	/**
	 * The process ID of the node.
	 */
	private Integer pid = null;

	/**
	 * Sets the process ID of the node.
	 * 
	 * @param pid the process ID of the node
	 */
	public void setPid(Integer pid) {
		this.pid = pid;
	}

	/**
	 * The process ID of the master node.
	 */
	private Integer pidMaster = null;

	/**
	 * Sets the process ID of the master node.
	 * 
	 * @param pidMaster the process ID of the master node
	 */
	public void setPidMaster(Integer pidMaster) {
		this.pidMaster = pidMaster;
	}

	/**
	 * The timeout flag. If is true the timeout occurred, if is false the timeout
	 * not occurred.
	 */
	private boolean timeoutOccured = false;

	/**
	 * Returns the timeout flag.
	 * 
	 * @return the timeout flag
	 */
	public boolean isTimeoutOccured() {
		return timeoutOccured;
	}

	/**
	 * Sets the timeout flag.
	 * 
	 * @param timeoutOccured the timeout flag
	 */
	public void setTimeoutOccured(boolean timeoutOccured) {
		this.timeoutOccured = timeoutOccured;
	}

	/**
	 * The master node flag. If is true the node is the master node, if is false the
	 * node is not the master node.
	 */
	private Boolean master = null;

	/**
	 * Sets the master node flag.
	 * 
	 * @param master the master node flag
	 */
	public void setMaster(Boolean master) {
		this.master = master;
	}

	/**
	 * Down flag. If is true the node is down, if is false the node is up.
	 */
	private Boolean down = false;

	/**
	 * Sets the down flag.
	 * 
	 * @param down the down flag
	 */
	public void setDown(Boolean down) {
		this.down = down;
	}

	/**
	 * The random probability generator.
	 */
	private RandomProb random;
	/**
	 * The wait response object.
	 */
	private WaitResponse waitResp = new WaitResponse(500);
	/**
	 * The message handler.
	 */
	private MessageHandlerImpl messageHandler = null;
	/**
	 * The executed task count.
	 */
	private int executedTaskCount = 0;
	/**
	 * The number of tasks to execute.
	 */
	private static final int TASKS = 100;
	/**
	 * The probability of the master node to give resource.
	 */
	private static final double getResourceProb = 0.7;

	/**
	 * Constructs a {@code ResourcesHandler} object with the specified ActiveMQ
	 * connection
	 *
	 * @param cf the ActiveMQ connection factory
	 * @throws JMSException if there is an error with the JMS connection
	 */
	public ResourcesHandler(ActiveMQConnectionFactory cf) throws JMSException {
		this.messageHandler = new MessageHandlerImpl(cf);
		this.messageHandler.start();
	}

	/**
	 * Executes the resources handler. If the node is not down and is not the master
	 * node, sends a resource request to the master node.
	 * 
	 * @throws InterruptedException if there is an error in the execution
	 * @throws JMSException         if there is an error in the JMS connection
	 */
	public void execute() throws InterruptedException, JMSException {
		if (this.down)
			return;
		if (this.master == null || this.pidMaster == null)
			return;
		if (!this.master) {
			this.messageHandler.send("resource_" + this.pidMaster, this.pid, RequestType.resourceRequest);
			// Wait for the response or timeout
			waitResp.waitForResponseOrTimeout();

			// Process the response
			if (waitResp.isResponseReceived()) {
				waitResp.setResponseReceived(false); // Reset flag
			} else {
				Print.print("Timeout occurred. No response received. Start election.", Print.red);
				this.setMaster(null);
				this.setPidMaster(null);
				this.timeoutOccured = true;
			}
		}
	}

	/**
	 * Returns the end execution flag. Return True if the executed task count is
	 * less than the number of tasks to execute, False otherwise.
	 * 
	 * @return the end execution flag
	 */
	public boolean endExecution() {
		return this.executedTaskCount < ResourcesHandler.TASKS;
	}

	@Override
	public void start() throws JMSException {
		this.random = new RandomProb(this.pid.longValue());
		this.messageHandler.setOnReceive(this);
		this.messageHandler.receive("resource_" + this.pid);
	}

	@Override
	public void close() throws JMSException {
		this.messageHandler.close();
	}

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
					waitResp.setResponse();
					Boolean response = (Boolean) ((RequestHandler) rh).obj;
					Print.print("Master " + this.pidMaster + " response: " + response, Print.deft);
					if (response) {
						this.executedTaskCount++;
						Print.print("Executed task: " + this.executedTaskCount + "/" + ResourcesHandler.TASKS,
								Print.deft);
					} else {
						Thread.sleep(100);
					}
					break;
				default:
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
