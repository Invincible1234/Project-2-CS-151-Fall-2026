package test;

import exception.RegulatoryViolationException;
import f1.model.Engineer;
import f1.model.HybridRaceCar;
import f1.model.HybridUpgrade;
import f1.model.SessionType;

public class HybridSpecialistVerification {
    public static void main(String[] args) {
        verifyPower();
        verifySpecialists();
        verifyUpgrades();
        System.out.println("Hybrid specialist verification passed.");
    }

    private static void verifyPower() {
        check(car("DEFAULT").getMguKPowerKw() == 300, "Default power is incorrect.");
        check(new HybridRaceCar("LIMIT", "Team", "Hybrid", 2026, 4, 3, 350)
                .getMguKPowerKw() == 350, "The maximum legal power was rejected.");
        for (double invalid : new double[] {0, -1, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            expect(IllegalArgumentException.class,
                    () -> new HybridRaceCar("BAD", "Team", "Hybrid", 2026, 4, 3, invalid));
        }
        expect(RegulatoryViolationException.class,
                () -> new HybridRaceCar("OVER", "Team", "Hybrid", 2026, 4, 3, 350.1));
    }

    private static void verifySpecialists() {
        HybridRaceCar car = car("ASSIGN");
        Engineer specialist = engineer("E1", "HYBRID_POWER_UNIT");
        Engineer wrong = engineer("WRONG", "CLASSIC_RESTORATION");
        expect(IllegalArgumentException.class, () -> car.lockForSpecialist(null));
        expect(IllegalArgumentException.class, () -> car.lockForSpecialist(wrong));
        check(wrong.isAvailable() && car.getAssignedEngineer() == null,
                "Rejected assignment changed either participant.");
        car.prepareForSession(SessionType.PRACTICE);
        check(car.lockForSpecialist(specialist), "Specialist assignment failed.");
        check(car.getAssignedEngineer() == specialist && !specialist.isAvailable()
                && car.getAssignedEngineerId().equals("E1")
                && specialist.getAssignedChassisId().equals("ASSIGN"),
                "Assignment was not synchronized.");
        check(!car.isRaceReady(), "Assignment did not invalidate preparation.");
        expect(IllegalStateException.class, () -> car.lockForSpecialist(specialist));
        expect(IllegalStateException.class, () -> car("OTHER").lockForSpecialist(specialist));
        car.prepareForSession(SessionType.RACE);
        car.startSession();
        expect(IllegalStateException.class, car::releaseSpecialist);
        expect(IllegalStateException.class, specialist::releaseFromCar);
        check(car.getAssignedEngineer() == specialist && !specialist.isAvailable(),
                "Rejected release changed assignment state.");
        car.completeSession();
        car.releaseSpecialist();
        check(car.getAssignedEngineer() == null && car.getAssignedEngineerId().isEmpty()
                && specialist.isAvailable() && specialist.getAssignedChassisId().isEmpty(),
                "Release did not clear both sides.");
        expect(IllegalStateException.class, car::releaseSpecialist);

        car.markOutOfService("Battery fault");
        expect(IllegalStateException.class, () -> car.lockForSpecialist(specialist));
        check(specialist.isAvailable(), "An unavailable car reserved the specialist.");
        car.performService("Repaired battery");
        car.updateMileage(500);
        expect(IllegalStateException.class, () -> car.lockForSpecialist(specialist));
        car.performService("Scheduled service");
        car.lockForSpecialist(specialist);
        car.scheduleStrategy("Reserved strategy");
        expect(IllegalStateException.class, car::releaseSpecialist);
        expect(IllegalStateException.class, () -> car.applyHybridUpgrade("PERFORMANCE"));
    }

    private static void verifyUpgrades() {
        HybridRaceCar car = car("UPGRADES");
        expect(IllegalStateException.class, () -> car.applyHybridUpgrade("PERFORMANCE"));
        car.lockForSpecialist(engineer("E2", "HYBRID_POWER_UNIT"));
        car.prepareForSession(SessionType.PRACTICE);
        car.startSession();
        car.deployEnergyBoost(1);
        car.chargeBatteryFromRecovery(1);
        car.completeLap();
        car.completeSession();
        for (HybridUpgrade upgrade : HybridUpgrade.values()) {
            car.prepareForSession(SessionType.PRACTICE);
            check(car.applyHybridUpgrade(" " + upgrade.name().toLowerCase(java.util.Locale.ROOT) + " "),
                    "Supported upgrade was rejected.");
            check(car.getMguKPowerKw() == upgrade.getPowerKw()
                    && car.getHybridUpgrade().equals(upgrade.name()), "Upgrade state is incorrect.");
            check(!car.isRaceReady() && car.getPreparedSessionType() == null,
                    "Upgrade did not invalidate setup.");
            check(car.getCurrentEnergyMJ() == 3 && car.getDeployedThisLapMJ() == 1
                    && car.getRecoveredThisLapMJ() == 1, "Upgrade reset energy or bypassed lap limits.");
        }
        car.prepareForSession(SessionType.RACE);
        for (String invalid : new String[] {null, "", "  ", "UNKNOWN"}) {
            expect(IllegalArgumentException.class, () -> car.applyHybridUpgrade(invalid));
            check(car.getMguKPowerKw() == 350 && car.isRaceReady(),
                    "Invalid upgrade changed power or setup.");
        }
        car.startSession();
        expect(IllegalStateException.class, () -> car.applyHybridUpgrade("EFFICIENCY"));
        check(car.getMguKPowerKw() == 350 && car.isSessionActive(),
                "Upgrade interrupted an active session.");
        car.completeSession();
        car.markOutOfService("Electrical fault");
        expect(IllegalStateException.class, () -> car.applyHybridUpgrade("EFFICIENCY"));
        car.performService("Repaired electrical fault");
        car.updateMileage(500);
        expect(IllegalStateException.class, () -> car.applyHybridUpgrade("EFFICIENCY"));
        car.performService("Scheduled service");
        car.releaseSpecialist();
        expect(IllegalStateException.class, () -> car.applyHybridUpgrade("EFFICIENCY"));
    }

    private static HybridRaceCar car(String id) {
        return new HybridRaceCar(id, "Team", "Hybrid", 2026, 4, 3);
    }

    private static Engineer engineer(String id, String specialty) {
        return new Engineer(id, "Engineer", specialty, 5, "5551234567");
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
