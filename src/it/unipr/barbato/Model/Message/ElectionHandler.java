package it.unipr.barbato.Model.Message;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;

import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.barbato.Interface.Message.Handler;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;

public class ElectionHandler implements Handler, MessageListener {

	private Integer pid = null;

	public void setPid(Integer pid) {
		this.pid = pid;
	}

	private Boolean master = null;

	public Boolean getMaster() {
		return this.master;
	}

	private Integer pidMaster = null;

	public Integer getPidMaster() {
		return this.pidMaster;
	}

	private ArrayList<Integer> pids;

	public void setPids(ArrayList<Integer> pids) {
		Collections.sort(pids);
		this.pids = new ArrayList<Integer>(pids);
	}

	private static final int responseTimeout = 500;
	boolean responseReceived;

	private MessageHandlerImpl messageHandler = null;

	private boolean down = false;

	public void setDown(boolean down) {
		this.down = down;
	}

	private boolean election = false;

	public boolean isElection() {
		return election;
	}

	private boolean coordination = false;

	public ElectionHandler(ActiveMQConnectionFactory cf) throws JMSException {
		this.messageHandler = new MessageHandlerImpl(cf);
		this.messageHandler.start();
	}

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
		this.sendPidsList(list, index, RequestType.election);
	}

	private void sendPidsList(ArrayList<Integer> list, int index, RequestType rt) {
		if (this.down)
			return;
		if (!this.election)
			return;

		int p = this.pids.get(index);
		if (p == this.pid)
			return;

		try {
			this.responseReceived = false;
			this.messageHandler.send("election_" + p, list, rt);
			this.waitForResponseOrTimeout();

		} catch (JMSException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// Process the response
		if (this.responseReceived) {
			this.responseReceived = false; // Reset flag
		} else {
			if (this.pids.size() - 1 == index)
				this.sendPidsList(list, 0, rt);
			else
				this.sendPidsList(list, index + 1, rt);
		}
	}

	public void setMaster(Boolean m, Integer pidMaster) {
		if (down)
			return;
		Integer tmp = this.pidMaster;

		this.master = m;
		this.pidMaster = pidMaster;

		if (tmp == this.pidMaster)
			return;

		// Formatting of current time to check the nodes coordination
		LocalTime currentTime = LocalTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String formattedTime = currentTime.format(formatter);

		System.err.println("My pid: " + this.pid + " Set Master: " + pidMaster + " I am the master: " + this.master
				+ " " + formattedTime);
	}

	public synchronized void setResponse() {
		this.responseReceived = true;
		notify();
	}

	public synchronized void waitForResponseOrTimeout() throws InterruptedException {
		if (!this.responseReceived) {
			wait(responseTimeout);
		}
	}

	public int findIndexOfNextPid(ArrayList<Integer> pids, int targetPid) {
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

	@SuppressWarnings({ "unchecked", "incomplete-switch" })
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
							this.sendPidsList(list, index, RequestType.election);
						}
						return;
					}

					// If the node is not already added we add the node pid to the list and
					// propagate the list
					this.election = true;
					this.coordination = false;
					this.setMaster(false, null);

					list.add(this.pid);
					this.sendPidsList(list, index, RequestType.election);

					break;
				case ack:
					if (this.pidMaster != null) {
						this.election = false;
						this.coordination = false;
					}
					this.setResponse();
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
