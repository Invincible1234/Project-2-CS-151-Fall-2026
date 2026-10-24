package test;

import f1.model.ClassicRaceCar;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.Raceable;
import f1.model.SessionType;

import java.util.List;

public class CarSessionVerification {
    public static void main(String[] args) {
        List<F1Car> cars =
                List.of(
                        new HybridRaceCar("H1", "Team", "Hybrid", 2026, 4, 3),
                        new ClassicRaceCar("C1", "Team", "Classic", 1980, 90, 2020, 300));
        for (F1Car car : cars) {
            Raceable raceable = car;
            check(!raceable.isAvailableForRace(), "A new car must be unprepared.");
            expect(IllegalStateException.class, car::startSession);
            expect(IllegalStateException.class, raceable::completeSession);
            expect(IllegalArgumentException.class, () -> raceable.prepareForSession(null));
            check(car.getPreparedSessionType() == null, "Rejected preparation changed state.");

            var driver =
                    new f1.model.Driver(
                            "D-" + car.getChassisId(),
                            "Driver",
                            "LICENSE-" + car.getChassisId(),
                            25,
                            "5551234567");
            driver.setMedicalClearance(true, "Cleared");
            driver.assignToCar(car);
            var engineer =
                    new f1.model.Engineer(
                            "E-" + car.getChassisId(),
                            "Engineer",
                            car instanceof HybridRaceCar
                                    ? f1.model.EngineerSpecialty.HYBRID_POWER_UNIT
                                    : f1.model.EngineerSpecialty.CLASSIC_RESTORATION,
                            5,
                            "5551234567");
            engineer.assignToCar(car);
            for (SessionType type : SessionType.values()) {
                check(raceable.prepareForSession(type), "An eligible car could not prepare.");
                check(car.getPreparedSessionType() == type, "Prepared type was not saved.");
                check(raceable.isAvailableForRace(), "A prepared car must be available.");
                check(!raceable.prepareForSession(type), "Repeated preparation was accepted.");
                car.startSession();
                check(car.isSessionActive(), "Session did not start.");
                check(!raceable.isAvailableForRace(), "An active car cannot be entered again.");
                expect(IllegalStateException.class, car::startSession);
                check(!raceable.prepareForSession(type), "Preparation interrupted a session.");
                raceable.completeSession();
                check(!car.isSessionActive(), "Session stayed active after completion.");
                check(!car.isRaceReady(), "Completion must require fresh preparation.");
                check(car.getPreparedSessionType() == null, "Completed session type was retained.");
                check(car.getTotalMileage() == 0, "Session completion added mileage.");
                expect(IllegalStateException.class, raceable::completeSession);
            }

            check(raceable.prepareForSession(SessionType.RACE), "Preparation failed.");
            car.updateMileage(501);
            check(!raceable.isAvailableForRace(), "Maintenance-due car remained available.");
            expect(IllegalStateException.class, car::startSession);
            check(!raceable.prepareForSession(SessionType.RACE), "Maintenance was bypassed.");
            check(
                    raceable.getNextMaintenanceDue().contains("required"),
                    "Maintenance-due status was not reported.");
        }
        System.out.println("Car session verification passed for both car types.");
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
