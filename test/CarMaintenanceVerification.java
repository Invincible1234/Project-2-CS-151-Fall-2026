package test;

import f1.model.ClassicRaceCar;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.SessionType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

public class CarMaintenanceVerification {
    public static void main(String[] args) {
        for (F1Car car :
                List.of(
                        new HybridRaceCar("H2", "Team", "Hybrid", 2026, 4, 3),
                        new ClassicRaceCar("C2", "Team", "Classic", 1980, 90, 2020, 300))) {
            car.updateMileage(499);
            check(!car.isDueForMaintenance(), "Service became due too early.");
            check(car.prepareForSession(SessionType.PRACTICE), "Healthy car could not prepare.");
            for (double invalid :
                    new double[] {
                        -1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
                    }) {
                expect(IllegalArgumentException.class, () -> car.updateMileage(invalid));
                check(
                        car.getTotalMileage() == 499 && car.getMileageSinceService() == 499,
                        "Rejected mileage changed state.");
                check(car.isRaceReady(), "Rejected mileage changed readiness.");
            }
            car.updateMileage(1);
            check(
                    car.isDueForMaintenance() && !car.isAvailableForRace(),
                    "The exact service threshold did not block racing.");
            check(!car.prepareForSession(SessionType.PRACTICE), "Maintenance was bypassed.");
            expect(IllegalArgumentException.class, () -> car.performService("  "));
            check(car.isDueForMaintenance(), "Invalid service reset the interval.");
            car.performService("  Inspected brakes and tires  ");
            check(
                    car.getTotalMileage() == 500 && car.getMileageSinceService() == 0,
                    "Service did not preserve lifetime mileage and reset its own interval.");
            check(!car.isRaceReady(), "Service should require fresh preparation.");
            check(
                    car.getLastServiceNotes().equals("Inspected brakes and tires"),
                    "Service notes were not trimmed.");
            check(car.prepareForSession(SessionType.PRACTICE), "Serviced car could not prepare.");
            expect(IllegalArgumentException.class, () -> car.markOutOfService(null));
            check(car.isRaceReady(), "Invalid service reason changed readiness.");
            car.markOutOfService("  Brake fault  ");
            check(car.getOutOfServiceReason().equals("Brake fault"), "Reason was not retained.");
            check(
                    car.getPreparedSessionType() == null && !car.isAvailableForRace(),
                    "Out-of-service car remained prepared.");
            check(!car.prepareForSession(SessionType.PRACTICE), "Unrepaired car could prepare.");
            car.performService("Repaired brakes");
            check(car.getOutOfServiceReason().isEmpty(), "Repair did not clear the fault.");
            check(car.prepareForSession(SessionType.PRACTICE), "Repaired car could not prepare.");
            car.startSession();
            expect(IllegalStateException.class, () -> car.performService("Unsafe service"));
            expect(IllegalStateException.class, () -> car.markOutOfService("Unsafe change"));
            check(
                    car.isSessionActive() && car.getOutOfServiceReason().isEmpty(),
                    "Rejected service operation interrupted the session.");
            car.completeSession();
            car.updateMileage(500);
            check(
                    car.isDueForMaintenance() && car.getTotalMileage() == 1000,
                    "A second service interval was not tracked independently.");
        }

        F1Car dated = new DatedCar();
        dated.prepareForSession(SessionType.PRACTICE);
        check(dated.getLastSetupDate().equals("2026-10-22"), "Setup date ignored the clock.");
        dated.performService("Scheduled service");
        check(dated.getLastServiceDate().equals("2026-10-22"), "Service date ignored the clock.");
        dated.updateMileage(Double.MAX_VALUE);
        expect(IllegalArgumentException.class, () -> dated.updateMileage(Double.MAX_VALUE));
        check(
                dated.getTotalMileage() == Double.MAX_VALUE
                        && dated.getMileageSinceService() == Double.MAX_VALUE,
                "Overflow corrupted mileage.");
        System.out.println("Car maintenance verification passed.");
    }

    private static class DatedCar extends ClassicRaceCar {
        private DatedCar() {
            super(
                    "D1",
                    "Team",
                    "Test",
                    1980,
                    90,
                    2020,
                    300,
                    Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC));
        }
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
