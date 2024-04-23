package it.unipr.barbato.Model.Utilities;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * The {@code Print} class provides methods to print messages with different colors
 * in the console.
 * 
 * @author Vincenzo Barbato 345728
 */
public class Print {
	
	/**
	 * The default color.
	 */
	public static String deft = "";
	/**
	 * The red color.
	 */
	public static String red = "\u001B[31m";
	/**
	 * The green color.
	 */
	public static String green = "\u001B[32m";
	/**
	 * The cyan color.
	 */
	public static String cyan = "\u001B[36m";
	/**
	 * The reset color.
	 */
	private static String reset = "\u001B[0m";

	/**
	 * Prints a message with the specified color.
	 * 
	 * @param text the message to print
	 * @param color the color of the message
	 */
	public static void print(String text, String color) {
		LocalTime currentTime = LocalTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
		String formattedTime = currentTime.format(formatter);
		System.out.println(color + formattedTime + " -> " + text + reset);
	}
}
