package test;

import exception.CapacityExceededException;

import f1.model.RaceResult;

import util.Constants;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class RaceResultCapacityVerification {
    public static void main(String[] args) {
        Clock clock = Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC);
        for (int i = 0; i < Constants.MAXIMUM_INSTANCES; i++) {
            new RaceResult("R" + i, "RACE" + i, "CAR", "DRIVER", "CIRCUIT", 1, "2026-10-22", clock);
        }
        try {
            new RaceResult(
                    "EXTRA", "EXTRA-RACE", "CAR", "DRIVER", "CIRCUIT", 1, "2026-10-22", clock);
            throw new AssertionError("The 101st race result was accepted.");
        } catch (CapacityExceededException expected) {
            System.out.println("Race result capacity verification passed.");
        }
    }
}
