package it.unipr.barbato.Model.Utilities;

import java.util.Random;

/**
 * The {@code RandomProb} class provides a method to generate a random boolean
 * value based on a specified probability.
 * 
 * @author Vincenzo Barbato 345728
 */
public class RandomProb {

	/**
	 * The random number generator.
	 */
	Random random = new Random();

	/**
	 * Constructs a new random probability generator.
	 * 
	 * @param seed the seed for the random number generator
	 */
	public RandomProb(Long seed) {
		random.setSeed(seed);
	}

	/**
	 * Generates a random boolean value based on the specified probability.
	 * 
	 * @param probability the probability of the boolean value
	 * @return the random boolean value
	 */
	public boolean exec(double probability) {
		if (probability < 0 || probability > 1) {
			throw new IllegalArgumentException("Probability must be between 0 and 1.");
		}
		double randomValue = random.nextDouble();
		return randomValue < probability;
	}
}
