package test;

import static test.StrategyCompletionFixture.check;

import f1.model.ClassicRaceCar;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.SessionType;

import java.util.List;

public final class CarRacePreparationVerification {
    public static void main(String[] args) {
        for (F1Car car :
                List.of(
                        new HybridRaceCar("HYBRID-PREP", "Apex", "Hybrid", 2026, 4, 3),
                        new ClassicRaceCar(
                                "CLASSIC-PREP", "Apex", "Classic", 1980, 90, 2020, 250))) {
            check(
                    !car.prepareForSession(SessionType.RACE)
                            && !car.isRaceReady()
                            && car.getPreparedSessionType() == null
                            && car.getLastSetupDate().isEmpty(),
                    "Unassigned race preparation must fail without changing setup state.");
            check(
                    car.prepareForSession(SessionType.PRACTICE),
                    "Practice preparation must remain available without race crew.");
            car.startSession();
            car.completeSession();
            Driver driver =
                    new Driver(
                            "D-" + car.getChassisId(),
                            "Driver",
                            "L-" + car.getChassisId(),
                            25,
                            "5551234567");
            driver.setMedicalClearance(true, "Cleared");
            driver.assignToCar(car);
            check(
                    !car.prepareForSession(SessionType.RACE),
                    "A driver alone must not satisfy race preparation.");
            Engineer engineer =
                    new Engineer(
                            "E-" + car.getChassisId(),
                            "Engineer",
                            car instanceof HybridRaceCar
                                    ? EngineerSpecialty.HYBRID_POWER_UNIT
                                    : EngineerSpecialty.CLASSIC_RESTORATION,
                            5,
                            "5551234567");
            engineer.assignToCar(car);
            check(
                    car.prepareForSession(SessionType.RACE),
                    "Eligible, assigned crew must permit race preparation.");
            driver.applyBan("Suspended");
            check(
                    !car.isRaceReady() && !car.prepareForSession(SessionType.RACE),
                    "Losing the driver must invalidate preparation and prevent another race"
                            + " setup.");
            driver.clearBan();
            driver.assignToCar(car);
            check(
                    car.prepareForSession(SessionType.RACE),
                    "Repaired eligibility and assignment must allow a new setup.");
            engineer.releaseFromCar();
            check(
                    !car.isRaceReady() && !car.prepareForSession(SessionType.RACE),
                    "Releasing the engineer must invalidate race setup.");
        }
        System.out.println("Car race preparation verification passed.");
    }
}
