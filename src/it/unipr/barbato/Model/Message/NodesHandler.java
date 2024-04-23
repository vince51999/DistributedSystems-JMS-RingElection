package it.unipr.barbato.Model.Message;

import java.io.Serializable;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.barbato.Interface.Message.Handler;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageListener;
import jakarta.jms.ObjectMessage;
import it.unipr.barbato.Model.Utilities.Print;

/**
 * The {@code NodesHandler} class implements the {@link Handler} and
 * {@link MessageListener} interfaces and represents a nodes handler in the
 * distributed system. It take updated all nodes on the system status 
 * (the number of nodes and the process IDs of all node in the distributed system).
 * 
 * @author Vincenzo Barbato 345728
 */
public class NodesHandler implements Handler, MessageListener {
	/**
	 * The process ID of the node.
	 */
	private Integer pid = null;
	/**
	 * Returns the process ID of the node.
	 * 
	 * @return the process ID of the node
	 */
	public Integer getPid() {
		return this.pid;
	}
	/**
	 * The list of process IDs of the nodes.
	 */
	public ArrayList<Integer> pids = new ArrayList<>();
	/**
	 * Returns the list of process IDs of the nodes.
	 * 
	 * @return the list of process IDs of the nodes
	 */
	public ArrayList<Integer> getPids() {
		return this.pids;
	}
	/**
	 * The message handler.
	 */
	private MessageHandlerImpl messageHandler = null;

	/**
	 * Creates an {@code NodesHandler} object with the specified ActiveMQ
	 * connection factory.
	 * 
	 * @param cf the ActiveMQ connection factory
	 * @throws JMSException if an error occurs during the execution
	 */
	public NodesHandler(ActiveMQConnectionFactory cf) throws JMSException {
		this.messageHandler = new MessageHandlerImpl(cf);
		messageHandler.start();
	}

	/**
	 * Gets the number of nodes.
	 *
	 * @return the number of nodes
	 */
	public int getSize() {
		return this.pids.size();
	}

	/**
	 * Gets the maximum process ID.
	 *
	 * @return the maximum process ID
	 */
	public Integer getMax() {
		// If the list is empty, return Long.MIN_VALUE or throw an exception
		if (this.pids.isEmpty()) 
			throw new IllegalArgumentException("List is empty");
		
		// Initialize max to the first element of the list
		int max = this.pids.get(0);

		for (int i = 1; i < this.pids.size(); i++) {
			if (this.pids.get(i) > max) 
				max = this.pids.get(i);
		}
		return max;
	}

	@Override
	public void start() throws JMSException {
		this.messageHandler.setOnSubscribe(this);
		this.messageHandler.subscribe("nodesList");

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		Print.print("Process Name: " + processName, Print.deft);
		// Extract the process ID from the name
		this.pid = Integer.parseInt(processName.split("@")[0]);
		this.pids.add(pid);
		this.messageHandler.publish("nodesList", (Serializable) pid, RequestType.subscribe);
	}

	@Override
	public void close() throws JMSException {
		this.messageHandler.publish("nodesList", (Serializable) this.pid, RequestType.unsubscribe);
		System.out.println();
		Print.print("Nodes out from Group.", Print.deft);
	}

	@Override
	public void onMessage(Message message) {
		try {
			if (message instanceof ObjectMessage) {
				ObjectMessage objectMessage = (ObjectMessage) message;
				Serializable rh = objectMessage.getObject();
				RequestType rt = (RequestType) ((RequestHandler) rh).type;
				switch (rt) {
				case subscribe:
					Integer pid = (Integer) ((RequestHandler) rh).obj;
					if (this.pids.contains(pid)) {
						Print.print("This pid is already added!", Print.deft);
					} else {
						this.pids.add(pid);
						System.out.println();
						Print.print("Pid List: " + this.pids, Print.deft);
						this.messageHandler.publish("nodesList", (Serializable) this.pids, RequestType.add_pid_to_list);
					}
					break;
				case unsubscribe:
					Integer pid1 = (Integer) ((RequestHandler) rh).obj;
					if (this.pids.contains(pid1)) {
						this.pids.remove(pid1);
						if (this.pids != null)
							Print.print("Pid List: " + this.pids, Print.deft);
					} else
						Print.print("This pid is already removed!", Print.deft);

					break;
				case add_pid_to_list:
					@SuppressWarnings("unchecked")
					ArrayList<Integer> pids = (ArrayList<Integer>) ((RequestHandler) rh).obj;
					if (this.pids.size() >= pids.size())
						Print.print("My list is already updated: " + this.pids, Print.deft);
					else {
						this.pids = new ArrayList<>(pids);
						Print.print("Update list: " + this.pids, Print.deft);
					}
					break;
				default:
					break;

				}
			}
		} catch (JMSException e) {
			e.printStackTrace();

		}
	}

	/**
	 * Checks if two lists are equal.
	 * 
	 * @param listA the first list
	 * @param listB the second list
	 * @return {@code true} if the lists are equal, {@code false} otherwise
	 */
	public boolean areEqualLists(ArrayList<Integer> listA, ArrayList<Integer> listB) {
		if (listA.size() > listB.size()) {
			ArrayList<Integer> tmp = new ArrayList<Integer>(listB);
			listB = new ArrayList<Integer>(listA);
			listA = new ArrayList<Integer>(tmp);
		}
		ArrayList<Integer> result = new ArrayList<Integer>(listB);
		result.removeAll(new HashSet<>(listA));
		if (result.size() == 0)
			return true;
		else
			return false;
	}
}
