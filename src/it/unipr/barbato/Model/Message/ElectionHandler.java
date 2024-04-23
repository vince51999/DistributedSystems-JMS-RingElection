package it.unipr.barbato.Model.Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.barbato.Interface.Message.Handler;
import it.unipr.barbato.Model.Utilities.Print;
import it.unipr.barbato.Model.Utilities.WaitResponse;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

/**
 * The {@code ElectionHandler} class implements the {@link Handler} and
 * {@link MessageHandler} interfaces and represents a handler for the election
 * process in the distributed system. It sends and receives election messages,
 * waits for responses, and coordinates the nodes in the system. The master node
 * is elected based on Ring Algorithm.
 * 
 * @author Vincenzo Barbato 345728
 */
public class ElectionHandler implements Handler, MessageListener {

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
	 * The master node flag.
	 */
	private Boolean master = null;

	/**
	 * Returns the master node flag.
	 * 
	 * @return the master node flag
	 */
	public Boolean getMaster() {
		return this.master;
	}

	/**
	 * The process ID of the master node.
	 */
	private Integer pidMaster = null;

	/**
	 * Returns the process ID of the master node.
	 * 
	 * @return the process ID of the master node
	 */
	public Integer getPidMaster() {
		return this.pidMaster;
	}

	/**
	 * The list of process IDs of the nodes in the system.
	 */
	private ArrayList<Integer> pids;

	/**
	 * Sets the list of process IDs of the nodes in the system.
	 * 
	 * @param pids the list of process IDs of the nodes in the system
	 */
	public void setPids(ArrayList<Integer> pids) {
		Collections.sort(pids);
		this.pids = new ArrayList<Integer>(pids);
	}

	/**
	 * The down flag.
	 */
	private boolean down = false;

	/**
	 * Sets the down flag.
	 * 
	 * @param down the down flag
	 */
	public void setDown(boolean down) {
		this.down = down;
	}

	/**
	 * The election flag.
	 */
	private boolean election = false;

	/**
	 * Returns the election flag.
	 * 
	 * @return the election flag
	 */
	public boolean isElection() {
		return election;
	}

	/**
	 * The wait response object.
	 */
	private WaitResponse waitResp = new WaitResponse(500);

	/**
	 * The message handler.
	 */
	private MessageHandlerImpl messageHandler = null;
	/**
	 * The coordination flag.
	 */
	private boolean coordination = false;

	/**
	 * Creates an {@code ElectionHandler} object with the specified ActiveMQ
	 * connection factory.
	 * 
	 * @param cf the ActiveMQ connection factory
	 * @throws JMSException if an error occurs during the execution
	 */
	public ElectionHandler(ActiveMQConnectionFactory cf) throws JMSException {
		this.messageHandler = new MessageHandlerImpl(cf);
		this.messageHandler.start();
	}

	/**
	 * Set up and check if is possible to start the election process. If is possible
	 * start the election process.
	 */
	public void election() {
		this.election = true;
		this.coordination = false;
		this.setMaster(false, null);

		if (this.down)
			return;
		if (this.pids == null)
			return;

		ArrayList<Integer> list = new ArrayList<>();
		list.add(this.pid);
		Integer index = this.findIndexOfNextPid(this.pids, this.pid);
		this.sendPidsList(list, index);
	}

	/**
	 * Send the list of process IDs to the next node in the list. If the next node
	 * doesn't respond, the list is propagated to the next of the next node. If the
	 * next node is the current node, the election process is stopped.
	 * 
	 * @param list  the list of process IDs
	 * @param index the index of the next node in the list
	 */
	private void sendPidsList(ArrayList<Integer> list, int index) {
		if (this.down)
			return;
		if (!this.election)
			return;

		int p = this.pids.get(index);
		if (p == this.pid)
			return;

		try {
			waitResp.setResponseReceived(false);// Reset flag
			this.messageHandler.send("election_" + p, list, RequestType.election);
			waitResp.waitForResponseOrTimeout();
		} catch (JMSException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// Process the response
		if (waitResp.isResponseReceived()) {
			waitResp.setResponseReceived(false); // Reset flag
		} else {
			if (this.pids.size() - 1 == index)
				this.sendPidsList(list, 0);
			else
				this.sendPidsList(list, index + 1);
		}
	}

	/**
	 * Set the master node and the process ID of the master node.
	 * 
	 * @param m         the master node flag
	 * @param pidMaster the process ID of the master node
	 */
	public void setMaster(Boolean m, Integer pidMaster) {
		if (down)
			return;
		Integer tmp = this.pidMaster;

		this.master = m;
		this.pidMaster = pidMaster;

		if (tmp == this.pidMaster)
			return;
		String text = " I'm not the master";
		if (this.master)
			text = " I am the master";
		Print.print("MyPid: " + this.pid + " MasterPid: " + pidMaster + text, Print.cyan);
	}

	/**
	 * Find the index of the next process ID in the list.
	 * 
	 * @param pids      the list of process IDs
	 * @param targetPid the target process ID
	 * @return the index of the next process ID in the list
	 */
	private int findIndexOfNextPid(ArrayList<Integer> pids, int targetPid) {
		for (int i = 0; i < pids.size(); i++) {
			if (pids.get(i) == targetPid) {
				if (pids.size() - 1 == i)
					return 0;
				else
					return i + 1;
			}
		}
		// Return -1 if the target PID is not found in the list
		return -1;
	}

	@Override
	public void onMessage(Message message) {
		if (this.down)
			return;
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Serializable rh = objectMessage.getObject();
				RequestType rt = (RequestType) ((RequestHandler) rh).type;

				switch (rt) {
				case election:
					// Response to election message
					String resPid = "election_" + message.getJMSCorrelationID().split("@")[0];
					this.messageHandler.send(resPid, this.pid, RequestType.ack);

					@SuppressWarnings("unchecked")
					ArrayList<Integer> list = (ArrayList<Integer>) ((RequestHandler) rh).obj;
					int maxOfList = Collections.max(list);
					int alreadyAdded = this.findIndexOfNextPid(list, this.pid);
					int index = this.findIndexOfNextPid(this.pids, this.pid);

					// If the node is already added is time to coordinate all node and set the new
					// master
					if (alreadyAdded != -1) {
						// If the node has end coordination and the list contain node pid, the node
						// doesn't propagate the list
						if (!this.coordination) {
							this.coordination = true;
							boolean master = false;
							if (this.pid == maxOfList)
								master = true;
							this.setMaster(master, maxOfList);
							this.sendPidsList(list, index);
						}
						return;
					}

					// If the node is not already added we add the node pid to the list and
					// propagate the list
					this.election = true;
					this.coordination = false;

					list.add(this.pid);
					this.sendPidsList(list, index);

					break;
				case ack:
					if (this.pidMaster != null) {
						// If the pidMaster is not null, the node is already coordinated
						this.election = false;
						this.coordination = false;
					}
					waitResp.setResponse();
					break;
				default:
					break;
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void start() throws JMSException {
		this.messageHandler.setOnReceive(this);
		this.messageHandler.receive("election_" + this.pid.toString());
	}

	@Override
	public void close() throws JMSException {
		this.messageHandler.close();
	}
}
