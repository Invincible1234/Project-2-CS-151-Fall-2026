package test;

import exception.CapacityExceededException;

import f1.model.Circuit;

import model.WeatherCondition;

import util.Constants;

public class CircuitCapacityVerification {
    public static void main(String[] args) {
        for (int i = 0; i < Constants.MAXIMUM_INSTANCES; i++) {
            new Circuit("C" + i, "Track " + i, 5, 10, true, WeatherCondition.CLEAR, "Location");
        }
        try {
            new Circuit("EXTRA", "Extra", 5, 10, true, WeatherCondition.CLEAR, "Location");
            throw new AssertionError("The 101st circuit was accepted.");
        } catch (CapacityExceededException expected) {
            System.out.println("Circuit capacity verification passed.");
        }
    }
}
