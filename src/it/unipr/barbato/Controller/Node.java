package it.unipr.barbato.Controller;

import java.util.ArrayList;

import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.barbato.Model.Message.ElectionHandler;
import it.unipr.barbato.Model.Message.NodesHandler;
import it.unipr.barbato.Model.Message.ResourcesHandler;
import it.unipr.barbato.Model.Utilities.RandomProb;
import it.unipr.barbato.Model.Utilities.Print;

/**
 * The {@code Node} class represents a node that interacts with other nodes in
 * the distributed system. It subscribes to the nodes list, requests resources,
 * response to resources requests if is the master, and participates in the
 * election process.
 * 
 * @author Vincenzo Barbato 345728
 */
public class Node {
	/**
	 * The number of nodes in the system.
	 */
	private static final int NODES = 3;
	/**
	 * The probability of a node going down.
	 */
	private static final double downProb = 0.1;
	/**
	 * The probability of a node going active.
	 */
	private static final double activeProb = 0.8;
	
	/**
	 * The broker URL.
	 */
	private static final String BROKER_URL = "tcp://localhost:61616";

	/**
	 * Runnable method to run node
	 * 
	 * @param args Arguments for main method
	 * @throws Exception If there is a problem
	 */
	public static void main(String[] args) throws Exception {
		ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);

		// Create nodes handler
		NodesHandler nh = new NodesHandler(cf);
		nh.start();

		// All nodes wait a predefined number of nodes before starting
		while (nh.getSize() < NODES) {
			Thread.sleep(1000);
		}
		
		// Create random probability generator
		RandomProb random = new RandomProb(nh.getPid().longValue());

		// Create resource handler to request resources or give resources if this node
		// is the master
		ResourcesHandler rh = new ResourcesHandler(cf);
		rh.setPid(nh.getPid());
		rh.start();

		// Create election handler to manage the election process (Nodes use ring
		// algorithm to elect a master node)
		ElectionHandler eh = new ElectionHandler(cf);
		eh.setPid(nh.getPid());
		eh.setPids(nh.getPids());
		eh.start();

		ArrayList<Integer> pids = new ArrayList<Integer>(nh.getPids());
		Thread.sleep(1000);
		eh.election();

		// Variable to check if the node is down
		boolean down = false;

		// All node execute the following code until n-1 nodes are end m tasks
		// executions
		while (rh.endExecution()) {
			eh.setPids(nh.getPids());
			ArrayList<Integer> list = new ArrayList<Integer>(nh.getPids());

			// If the node is the last node, the system goes down
			if (pids.size() == 1) {
				Print.print("I am the last node! System goes down!", Print.deft);
				nh.close();
				rh.close();
				eh.close();
				System.exit(0);
			}

			// If the nodes list has changed, the election process is started
			if (!nh.areEqualLists(pids, list)) {
				pids = list;
				eh.election();
			}

			// If a timeout occurs, the election process is started
			if (rh.isTimeoutOccured()) {
				rh.setTimeoutOccured(false);
				eh.election();
			}

			if (down) {
				// If the node is down, it goes up with a certain probability and starts the
				// election process
				if (random.exec(activeProb)) {
					down = false;
					eh.setDown(down);
					rh.setDown(down);
					Print.print("Nodes up", Print.green);
					eh.election();
				}
			} else {
				// If the node is up, it requests resources or gives resources if it is the
				// master
				rh.setPidMaster(eh.getPidMaster());
				rh.setMaster(eh.getMaster());
				rh.execute();
				
				Thread.sleep(100);
				// If the node is up, it goes down with a certain probability
				if (random.exec(downProb)) {
					down = true;
					eh.setDown(down);
					rh.setDown(down);
					Print.print("Nodes down", Print.red);
					Thread.sleep(5000);
				}
				Thread.sleep(100);
			}
			Thread.sleep(500);
		}
		nh.close();
		rh.close();
		eh.close();
		System.exit(0);
	}
}
