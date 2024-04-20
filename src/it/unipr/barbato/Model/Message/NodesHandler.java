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

/**
 * The {@code NodesHandler} class implements the {@link Handler} and
 * {@link MessageListener} interfaces to handle server-side offers for products.
 * It keeps track of the number of connected clients and response to client
 * offers.
 * 
 * @author Vincenzo Barbato 345728
 */
public class NodesHandler implements Handler, MessageListener {
	private Integer pid = null;
	/**
	 * The list of product offers.
	 */
	public ArrayList<Integer> pids = new ArrayList<>();
	/**
	 * The message handler.
	 */
	private MessageHandlerImpl messageHandler = null;

	/**
	 * Constructs a ServerOffersHandler object with the specified MessageHandler.
	 *
	 * @param messageHandler the MessageHandler to be used for handling messages
	 * @throws JMSException if there is an error with the JMS connection
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

	public Integer getPid() {
		return this.pid;
	}

	public ArrayList<Integer> getPids() {
		return this.pids;
	}

	public Integer getMax() {
		// If the list is empty, return Long.MIN_VALUE or throw an exception
		if (this.pids.isEmpty()) {
			// You can choose to handle this case based on your requirements
			throw new IllegalArgumentException("List is empty");
			// return Long.MIN_VALUE; // Or return the minimum value of long
		}
		// Initialize max to the first element of the list
		int max = this.pids.get(0);

		// Iterate through the list starting from the second element
		for (int i = 1; i < this.pids.size(); i++) {
			// Update max if the current element is greater
			if (this.pids.get(i) > max) {
				max = this.pids.get(i);
			}
		}
		return max;
	}

	@Override
	public void start() throws JMSException {
		this.messageHandler.setOnSubscribe(this);
		this.messageHandler.subscribe("nodesList");

		String processName = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println("Process Name: " + processName);
		// Extract the process ID from the name
		this.pid = Integer.parseInt(processName.split("@")[0]);
		this.pids.add(pid);
		this.messageHandler.publish("nodesList", (Serializable) pid, RequestType.subscribe);
	}

	@Override
	public void close() throws JMSException {
		this.messageHandler.publish("nodesList", (Serializable) this.pid, RequestType.unsubscribe);
		System.out.println("Nodes out from Group.");
	}

	@SuppressWarnings("incomplete-switch")
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
						System.out.println("This pid is already added!");
					} else {
						this.pids.add(pid);
						System.out.println("Pid List: " + this.pids);
						this.messageHandler.publish("nodesList", (Serializable) this.pids, RequestType.add_pid_to_list);
					}
					break;
				case unsubscribe:
					Integer pid1 = (Integer) ((RequestHandler) rh).obj;
					if (this.pids.contains(pid1)) {
						this.pids.remove(pid1);
						if (this.pids != null)
							System.out.println("Pid List: " + this.pids);
					} else {
						System.out.println("This pid is already removed!");
					}
					break;
				case add_pid_to_list:
					@SuppressWarnings("unchecked")
					ArrayList<Integer> pids = (ArrayList<Integer>) ((RequestHandler) rh).obj;
					if (this.pids.size() >= pids.size()) {
						System.out.println("My list is already updated: " + this.pids);
					} else {
						this.pids = new ArrayList<>(pids);
						System.out.println("Update list: " + this.pids);
					}
					break;
				}
			}
		} catch (JMSException e) {
			e.printStackTrace();

		}
	}

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
