package test;

import exception.RegulatoryViolationException;

import f1.model.HybridRaceCar;
import f1.model.SessionType;

public class HybridLifecycleVerification {
    public static void main(String[] args) {
        HybridRaceCar car = new HybridRaceCar("LAPS", "Team", "Hybrid", 2026, 20, 10);
        expect(IllegalStateException.class, () -> car.deployEnergyBoost(1));
        expect(IllegalStateException.class, () -> car.chargeBatteryFromRecovery(1));
        expect(IllegalStateException.class, car::startNextLap);
        expect(IllegalStateException.class, car::completeLap);
        car.prepareForSession(SessionType.PRACTICE);
        car.startSession();
        check(car.getCurrentLapNumber() == 1, "Session must begin on lap one.");
        car.deployEnergyBoost(2);
        car.deployEnergyBoost(2);
        car.chargeBatteryFromRecovery(4);
        car.chargeBatteryFromRecovery(5);
        checkEnergy(car, 15, 4, 9);

        for (double invalid :
                new double[] {
                    0, -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
                }) {
            expect(IllegalArgumentException.class, () -> car.deployEnergyBoost(invalid));
            expect(IllegalArgumentException.class, () -> car.chargeBatteryFromRecovery(invalid));
            checkEnergy(car, 15, 4, 9);
        }
        expect(RegulatoryViolationException.class, () -> car.deployEnergyBoost(0.5));
        expect(RegulatoryViolationException.class, () -> car.chargeBatteryFromRecovery(0.5));
        expect(IllegalStateException.class, car::resetEnergyState);
        expect(IllegalStateException.class, car::startNextLap);
        expect(IllegalStateException.class, car::startSession);
        check(
                !car.prepareForSession(SessionType.PRACTICE),
                "Preparation bypassed active lap limits.");
        checkEnergy(car, 15, 4, 9);

        car.completeLap();
        expect(IllegalStateException.class, car::completeLap);
        expect(IllegalStateException.class, () -> car.deployEnergyBoost(1));
        expect(IllegalStateException.class, () -> car.chargeBatteryFromRecovery(1));
        expect(IllegalStateException.class, car::resetEnergyState);
        check(car.isLapCompleted(), "Rejected actions reopened a finished lap.");
        checkEnergy(car, 15, 4, 9);
        car.startNextLap();
        check(car.getCurrentLapNumber() == 2 && !car.isLapCompleted(), "Next lap did not start.");
        checkEnergy(car, 15, 0, 0);
        expect(IllegalStateException.class, car::startNextLap);
        car.deployEnergyBoost(4);
        car.chargeBatteryFromRecovery(9);
        checkEnergy(car, 20, 4, 9);
        expect(IllegalArgumentException.class, () -> car.chargeBatteryFromRecovery(0.5));
        checkEnergy(car, 20, 4, 9);
        car.completeLap();
        car.completeSession();
        expect(IllegalStateException.class, car::completeSession);
        expect(IllegalStateException.class, car::startNextLap);
        expect(IllegalStateException.class, () -> car.deployEnergyBoost(1));
        car.resetEnergyState();
        checkEnergy(car, 20, 0, 0);
        check(car.getCurrentLapNumber() == 0, "Idle reset changed the session state incorrectly.");
        car.prepareForSession(SessionType.PRACTICE);
        car.startSession();
        check(car.getCurrentLapNumber() == 1, "A new session did not begin on lap one.");
        checkEnergy(car, 20, 0, 0);
        car.deployEnergyBoost(4);
        car.completeLap();
        car.updateMileage(500);
        expect(IllegalStateException.class, car::startNextLap);
        checkEnergy(car, 16, 4, 0);
        check(car.isLapCompleted(), "Rejected lap advancement changed lap status.");
        car.completeSession();

        HybridRaceCar empty = new HybridRaceCar("EMPTY", "Team", "Hybrid", 2026, 4, 0);
        empty.prepareForSession(SessionType.PRACTICE);
        empty.startSession();
        expect(IllegalArgumentException.class, () -> empty.deployEnergyBoost(1));
        empty.chargeBatteryFromRecovery(4);
        empty.deployEnergyBoost(4);
        checkEnergy(empty, 0, 4, 4);
        empty.completeSession();
        empty.resetEnergyState();
        checkEnergy(empty, 0, 0, 0);
        System.out.println("Hybrid lifecycle verification passed.");
    }

    private static void checkEnergy(
            HybridRaceCar car, double stored, double deployed, double recovered) {
        check(
                car.getCurrentEnergyMJ() == stored
                        && car.getDeployedThisLapMJ() == deployed
                        && car.getRecoveredThisLapMJ() == recovered,
                "Unexpected energy state.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("Unexpected exception type.", error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
