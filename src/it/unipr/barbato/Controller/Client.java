package it.unipr.barbato.Controller;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import org.apache.activemq.ActiveMQConnectionFactory;

import it.unipr.barbato.Model.Message.ElectionHandler;
import it.unipr.barbato.Model.Message.NodesHandler;
import it.unipr.barbato.Model.Message.ResourcesHandler;

/**
 * The {@code Client} class represents a client that interacts with the shop
 * system. It subscribes to the products list, creates client offers, and sends
 * them to the server. The client continues to make purchases until a specified
 * number of purchases is reached.
 * 
 * @author Vincenzo Barbato 345728
 */
public class Client {
	private static final int NODES = 5;
	private static final double downProb = 0.05;
	private static final double activeProb = 0.8;
	private static final int MAX_NUM_DOWN = 10;
	/**
	 * The broker URL.
	 */
	private static final String BROKER_URL = "tcp://localhost:61616";

	/**
	 * Runnable method to run client
	 * 
	 * @param args Arguments for main method
	 * @throws Exception If there is a problem
	 */
	public static void main(String[] args) throws Exception {
		ActiveMQConnectionFactory cf = new ActiveMQConnectionFactory(BROKER_URL);

		NodesHandler nodes = new NodesHandler(cf);
		nodes.start();

		// Client don't end to purchases
		while (nodes.getSize() < NODES) {
			Thread.sleep(1000);
		}
		// Create client offer
		ResourcesHandler rh = new ResourcesHandler(cf);
		rh.setPid(nodes.getPid());
		rh.start();

		ElectionHandler eh = new ElectionHandler(cf);
		eh.setPid(nodes.getPid());
		eh.setPids(nodes.getPids());
		eh.start();

		ArrayList<Integer> pids = new ArrayList<Integer>(nodes.getPids());
		Thread.sleep(1000);
		eh.election();

		boolean down = false;
		int countDown = 0;

		while (rh.endExecution()) {
			eh.setPids(nodes.getPids());
			ArrayList<Integer> list = new ArrayList<Integer>(nodes.getPids());
			if (pids.size() == 1) {
				System.out.println("I am the last node!");
				nodes.close();
				rh.close();
				eh.close();
				System.exit(0);
			}
			if (!nodes.areEqualLists(pids, list)) {
				pids = list;
				eh.election();
			}
			if (rh.isTimeoutOccured()) {
				rh.setTimeoutOccured(false);
				eh.election();
			}
			// Formatting of current time to check the nodes coordination
			LocalTime currentTime = LocalTime.now();
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
			String formattedTime = currentTime.format(formatter);

			if (down) {
				if (rh.returnTrueWithProbability(activeProb)) {
					down = false;
					eh.setDown(down);
					rh.setDown(down);
					System.out.println("Nodes up " + formattedTime);
					eh.election();
				}
			} else {
				rh.setPidMaster(eh.getPidMaster());
				rh.setMaster(eh.getMaster());
				rh.execute();
				if (countDown < MAX_NUM_DOWN && rh.returnTrueWithProbability(downProb)) {
					down = true;
					countDown++;
					eh.setDown(down);
					rh.setDown(down);
					System.out.println("Nodes down " + formattedTime);
					Thread.sleep(5000);
				}

				Thread.sleep(100);
			}
			Thread.sleep(500);
		}
		nodes.close();
		rh.close();
		eh.close();
		System.exit(0);
	}
}
