package test;

import f1.model.ClassicRaceCar;
import f1.model.SessionType;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class ClassicRestorationVerification {
    public static void main(String[] args) {
        verifyOriginality();
        verifyRestoration();
        verifyCommitmentsAndDates();
        System.out.println("Classic restoration verification passed.");
    }

    private static void verifyOriginality() {
        ClassicRaceCar car = new DatedClassic();
        car.prepareForSession(SessionType.PRACTICE);
        for (int delta : new int[] {11, -91, Integer.MAX_VALUE, Integer.MIN_VALUE}) {
            expect(IllegalArgumentException.class, () -> car.updateOriginalityScore(delta));
            check(
                    car.getOriginalityScore() == 90 && car.isRaceReady(),
                    "Rejected originality change altered score or preparation.");
        }
        car.updateOriginalityScore(10);
        check(
                car.getOriginalityScore() == 100 && !car.isRaceReady(),
                "A valid authenticity change did not invalidate setup.");
        car.updateOriginalityScore(-100);
        check(car.getOriginalityScore() == 0, "The lower score boundary was rejected.");
        car.updateOriginalityScore(90);
        car.prepareForSession(SessionType.PRACTICE);
        car.startSession();
        expect(IllegalStateException.class, () -> car.updateOriginalityScore(1));
        expect(IllegalStateException.class, () -> car.scheduleRestoration(2026));
        check(
                car.isSessionActive() && car.getOriginalityScore() == 90,
                "Rejected work interrupted the active session.");
    }

    private static void verifyRestoration() {
        ClassicRaceCar car = new DatedClassic();
        car.updateMileage(500);
        expect(IllegalStateException.class, () -> car.completeRestoration(0, "No restoration"));
        expect(IllegalArgumentException.class, () -> car.scheduleRestoration(2025));
        expect(IllegalArgumentException.class, () -> car.scheduleRestoration(10000));
        check(
                !car.isRestorationInProgress() && car.getRestorationYear() == 2020,
                "Invalid scheduling changed restoration history.");
        check(car.scheduleRestoration(2026), "Scheduling failed.");
        check(
                car.isRestorationInProgress() && car.getScheduledRestorationYear() == 2026,
                "Scheduled restoration was not retained.");
        check(
                car.getRestorationYear() == 2020 && !car.isAvailableForRace(),
                "Scheduling prematurely completed restoration or left the car available.");
        expect(IllegalStateException.class, () -> car.scheduleRestoration(2026));
        expect(IllegalStateException.class, () -> car.performService("Bypass restoration"));
        expect(IllegalStateException.class, () -> car.updateOriginalityScore(1));
        expect(IllegalStateException.class, () -> car.scheduleStrategy("Race"));
        expect(IllegalStateException.class, () -> car.applyAeroUpgrade("BALANCED"));
        check(!car.prepareForSession(SessionType.PRACTICE), "Restoration allowed preparation.");
        expect(IllegalArgumentException.class, () -> car.completeRestoration(11, "Invalid score"));
        expect(IllegalArgumentException.class, () -> car.completeRestoration(5, "  "));
        check(
                car.isRestorationInProgress()
                        && car.getOriginalityScore() == 90
                        && car.getMileageSinceService() == 500
                        && car.getLastServiceDate().isEmpty(),
                "Rejected completion partially changed the car.");
        car.completeRestoration(5, "Rebuilt and inspected original components");
        check(
                !car.isRestorationInProgress()
                        && car.getScheduledRestorationYear() == 0
                        && car.getRestorationYear() == 2026
                        && car.getOriginalityScore() == 95,
                "Restoration completion did not update history and authenticity.");
        check(
                car.getTotalMileage() == 500
                        && car.getMileageSinceService() == 0
                        && car.getOutOfServiceReason().isEmpty()
                        && car.getLastServiceDate().equals("2026-10-22"),
                "Restoration servicing lost mileage or failed to clear the service restriction.");
        check(!car.isRaceReady(), "Restoration must require fresh preparation.");
        check(car.prepareForSession(SessionType.PRACTICE), "Restored car could not prepare.");
        expect(
                IllegalStateException.class,
                () -> car.completeRestoration(5, "Repeated completion"));
        check(
                car.getOriginalityScore() == 95 && car.isRaceReady(),
                "Repeated completion changed the car.");
    }

    private static void verifyCommitmentsAndDates() {
        ClassicRaceCar future = new DatedClassic();
        future.scheduleRestoration(2027);
        expect(IllegalStateException.class, () -> future.completeRestoration(0, "Too early"));
        check(
                future.getRestorationYear() == 2020 && future.isRestorationInProgress(),
                "Future restoration changed completed history.");
        ClassicRaceCar reserved = new DatedClassic();
        reserved.prepareForSession(SessionType.PRACTICE);
        reserved.startSession();
        expect(IllegalStateException.class, () -> reserved.scheduleRestoration(2026));
        expect(IllegalStateException.class, () -> reserved.updateOriginalityScore(1));
        check(
                !reserved.isRestorationInProgress() && reserved.getOriginalityScore() == 90,
                "Reserved-car rejection changed state.");
        expect(IllegalArgumentException.class, () -> new DatedClassic(1979));
        expect(IllegalArgumentException.class, () -> new DatedClassic(2027));
    }

    private static class DatedClassic extends ClassicRaceCar {
        private DatedClassic() {
            this(2020);
        }

        private DatedClassic(int restorationYear) {
            super(
                    "C-RESTORE",
                    "Team",
                    "Classic",
                    1980,
                    90,
                    restorationYear,
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
