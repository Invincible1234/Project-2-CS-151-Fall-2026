package test;

import exception.CapacityExceededException;
import f1.model.ClassicRaceCar;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.Team;
import java.util.List;
import util.Constants;

public final class TeamAssignmentVerification {
    private static int checks;

    public static void main(String[] args) {
        verifyAssignments();
        verifyEligibility();
        verifyCarRemoval();
        verifyDriverRemoval();
        verifyEngineerRemoval();
        verifyPersonnelChanges();
        verifyAssignmentLimit();
        verifyInvalidInputs();
        System.out.println("PASS: Team assignments (" + checks + " checks)");
    }

    private static void verifyAssignments() {
        Team team = team();
        F1Car car = hybrid("CAR-1");
        Driver driver = clearedDriver("LICENSE-1");
        team.addCar(car);
        team.addDriver(driver);
        List<String> before = driver.getAssignedChassisIds();
        check(team.assignDriver(clearedDriver(" LICENSE-1 "), hybrid("CAR-1")),
                "Assignment must resolve registered objects by business identity.");
        check(car.getAssignedDriver() == driver && driver.getAssignedChassisIds().equals(List.of("CAR-1")),
                "Car and registered driver must agree on the assignment.");
        check(before.isEmpty(), "Assignment snapshots must not change later.");
        expectFailure(UnsupportedOperationException.class, () -> driver.getAssignedChassisIds().clear());
        expectFailure(IllegalStateException.class, () -> team.assignDriver(driver, car));
        Driver other = clearedDriver("LICENSE-2");
        team.addDriver(other);
        expectFailure(IllegalStateException.class, () -> team.assignDriver(other, car));
        check(car.getAssignedDriver() == driver && other.getAssignedChassisIds().isEmpty(),
                "Assignment conflicts must leave both drivers unchanged.");
        driver.unassignFromCar(car);
        check(car.getAssignedDriver() == null && driver.getAssignedChassisIds().isEmpty(),
                "Unassignment must update both sides.");
        check(team.assignDriver(other, car), "Released car must accept a different driver.");
    }

    private static void verifyEligibility() {
        Team team = team();
        F1Car car = hybrid("ELIGIBLE-CAR");
        Driver driver = driver("ELIGIBLE-LICENSE");
        team.addCar(car);
        team.addDriver(driver);
        expectFailure(IllegalStateException.class, () -> team.assignDriver(driver, car));
        driver.setMedicalClearance(true, "Cleared");
        driver.applyBan("Suspended");
        expectFailure(IllegalStateException.class, () -> team.assignDriver(driver, car));
        driver.clearBan();
        expectFailure(IllegalArgumentException.class,
                () -> team.assignDriver(clearedDriver("UNREGISTERED"), car));
        expectFailure(IllegalArgumentException.class,
                () -> team.assignDriver(driver, hybrid("UNREGISTERED")));
        check(car.getAssignedDriver() == null && driver.getAssignedChassisIds().isEmpty(),
                "Rejected eligibility and membership checks must not assign anything.");
        F1Car unavailable = hybrid("OUT-OF-SERVICE");
        unavailable.markOutOfService("Repairs required");
        team.addCar(unavailable);
        expectFailure(IllegalStateException.class, () -> team.assignDriver(driver, unavailable));
        car.scheduleStrategy("Reserved");
        expectFailure(IllegalStateException.class, () -> team.assignDriver(driver, car));
        check(driver.getAssignedChassisIds().isEmpty(), "Unavailable cars must not change the driver.");
    }

    private static void verifyCarRemoval() {
        Team team = team();
        F1Car car = hybrid("REMOVE-CAR");
        Driver driver = clearedDriver("REMOVE-DRIVER");
        team.addCar(car);
        team.addDriver(driver);
        team.assignDriver(driver, car);
        expectFailure(IllegalStateException.class, () -> team.removeCar(hybrid("REMOVE-CAR")));
        check(team.getCars().size() == 1 && car.getAssignedDriver() == driver,
                "Assigned-car removal must preserve membership and assignments.");
        driver.unassignFromCar(car);
        Engineer engineer = engineer("REMOVE-ENGINEER");
        engineer.assignToCar(car);
        expectFailure(IllegalStateException.class, () -> team.removeCar(car));
        engineer.releaseFromCar();
        check(team.removeCar(hybrid("REMOVE-CAR")), "Unassigned car must be removable by identity.");
        check(!team.removeCar(car), "Removing an unregistered car must return false.");
        check(team.addCar(hybrid("REPLACEMENT")), "Removal must free team capacity.");
        F1Car scheduled = team.getCars().get(0);
        scheduled.scheduleStrategy("Reserved");
        expectFailure(IllegalStateException.class, () -> team.removeCar(scheduled));
        check(team.getCars().size() == 1, "Scheduled-car removal must preserve membership.");
    }

    private static void verifyDriverRemoval() {
        Team team = team();
        F1Car first = hybrid("DRIVER-CAR-1");
        F1Car second = hybrid("DRIVER-CAR-2");
        Driver driver = clearedDriver("MULTI-LICENSE");
        team.addCar(first);
        team.addCar(second);
        team.addDriver(driver);
        team.assignDriver(driver, first);
        team.assignDriver(driver, second);
        second.scheduleStrategy("Reserved");
        expectFailure(IllegalStateException.class, () -> team.removeDriver(driver));
        check(team.getDrivers().size() == 1 && driver.getAssignedChassisIds().size() == 2
                && first.getAssignedDriver() == driver && second.getAssignedDriver() == driver,
                "All assignments must be checked before removing any of them.");
        expectFailure(IllegalStateException.class, () -> driver.unassignFromCar(second));

        Team removable = team();
        F1Car third = hybrid("DRIVER-CAR-3");
        F1Car fourth = hybrid("DRIVER-CAR-4");
        Driver removableDriver = clearedDriver("REMOVABLE-LICENSE");
        removable.addCar(third);
        removable.addCar(fourth);
        removable.addDriver(removableDriver);
        removable.assignDriver(removableDriver, third);
        removable.assignDriver(removableDriver, fourth);
        check(removable.removeDriver(driver(" REMOVABLE-LICENSE ")),
                "Removal must resolve the registered driver rather than the supplied copy.");
        check(removable.getDrivers().isEmpty() && removableDriver.getAssignedChassisIds().isEmpty()
                && third.getAssignedDriver() == null && fourth.getAssignedDriver() == null,
                "Removing a driver must release every assignment on both sides.");
        check(!removable.removeDriver(removableDriver), "Repeated driver removal must return false.");
    }

    private static void verifyEngineerRemoval() {
        Team team = team();
        F1Car car = hybrid("ENGINEER-CAR");
        F1Car otherCar = hybrid("OTHER-ENGINEER-CAR");
        Engineer engineer = engineer("EMPLOYEE-1");
        Engineer other = engineer("EMPLOYEE-2");
        team.addCar(car);
        team.addCar(otherCar);
        team.addEngineer(engineer);
        check(engineer.assignToCar(car), "Qualified engineer assignment must succeed.");
        check(car.getAssignedEngineer() == engineer && !engineer.isAvailable()
                && "ENGINEER-CAR".equals(engineer.getAssignedChassisId()),
                "Engineer assignment must update both sides and availability.");
        expectFailure(IllegalStateException.class, () -> engineer.assignToCar(otherCar));
        expectFailure(IllegalStateException.class, () -> other.assignToCar(car));
        check(other.isAvailable() && other.getAssignedChassisId().isEmpty()
                && otherCar.getAssignedEngineer() == null, "Conflicts must not partially assign engineers.");
        engineer.releaseFromCar();
        check(car.getAssignedEngineer() == null && engineer.isAvailable(),
                "Release must clear the car and restore engineer availability.");
        engineer.assignToCar(otherCar);
        check(team.removeEngineer(engineer(" EMPLOYEE-1 ")),
                "Removal must resolve the registered engineer by identity.");
        check(team.getEngineers().isEmpty() && otherCar.getAssignedEngineer() == null
                && engineer.isAvailable() && engineer.getAssignedChassisId().isEmpty(),
                "Engineer removal must clear all assignment state.");
        check(!team.removeEngineer(engineer), "Repeated engineer removal must return false.");

        team.addEngineer(other);
        other.assignToCar(car);
        car.scheduleStrategy("Reserved");
        expectFailure(IllegalStateException.class, () -> team.removeEngineer(other));
        check(team.getEngineers().size() == 1 && car.getAssignedEngineer() == other
                && !other.isAvailable(), "Scheduled-strategy rejection must preserve engineer state.");
        Engineer classicEngineer = new Engineer("CLASSIC", "Engineer",
                EngineerSpecialty.CLASSIC_RESTORATION, 5, "5551234567");
        expectFailure(IllegalArgumentException.class, () -> classicEngineer.assignToCar(otherCar));
        F1Car classic = new ClassicRaceCar("CLASSIC", "Apex", "Classic", 1970, 90, 2020, 300);
        check(classicEngineer.assignToCar(classic), "Restoration engineer must be eligible for classic cars.");
    }

    private static void verifyPersonnelChanges() {
        Team team = team();
        F1Car car = hybrid("PERSONNEL-CAR");
        Driver driver = clearedDriver("PERSONNEL-LICENSE");
        team.addCar(car);
        team.addDriver(driver);
        team.assignDriver(driver, car);
        expectFailure(IllegalArgumentException.class, () -> driver.applyBan(" "));
        check(car.getAssignedDriver() == driver, "Invalid ban must preserve assignments.");
        driver.applyBan("Suspended");
        check(car.getAssignedDriver() == null && driver.getAssignedChassisIds().isEmpty()
                && !driver.isEligibleForAssignment(), "Ban must release assignments and prevent reassignment.");
        driver.clearBan();
        team.assignDriver(driver, car);
        driver.setMedicalClearance(false, "Not cleared");
        check(car.getAssignedDriver() == null && driver.getAssignedChassisIds().isEmpty()
                && !driver.isEligibleForAssignment(), "Revoking clearance must release assignments.");
    }

    private static void verifyAssignmentLimit() {
        Driver driver = clearedDriver("LIMIT-LICENSE");
        for (int i = 0; i < Constants.MAXIMUM_INSTANCES; i++) {
            driver.assignToCar(hybrid("LIMIT-" + i));
        }
        F1Car extra = hybrid("LIMIT-EXTRA");
        expectFailure(CapacityExceededException.class, () -> driver.assignToCar(extra));
        check(driver.getAssignedChassisIds().size() == Constants.MAXIMUM_INSTANCES
                && extra.getAssignedDriver() == null, "Assignment overflow must leave both sides unchanged.");
    }

    private static void verifyInvalidInputs() {
        Team team = team();
        F1Car car = hybrid("NULL-CAR");
        Driver driver = clearedDriver("NULL-LICENSE");
        Engineer engineer = engineer("NULL-EMPLOYEE");
        expectFailure(IllegalArgumentException.class, () -> team.assignDriver(null, car));
        expectFailure(IllegalArgumentException.class, () -> team.assignDriver(driver, null));
        expectFailure(IllegalArgumentException.class, () -> team.removeCar(null));
        expectFailure(IllegalArgumentException.class, () -> team.removeDriver(null));
        expectFailure(IllegalArgumentException.class, () -> team.removeEngineer(null));
        expectFailure(IllegalArgumentException.class, () -> driver.assignToCar(null));
        expectFailure(IllegalArgumentException.class, () -> driver.unassignFromCar(null));
        expectFailure(IllegalArgumentException.class, () -> driver.unassignFromCar(car));
        expectFailure(IllegalArgumentException.class, () -> engineer.assignToCar(null));
        check(team.getCars().isEmpty() && team.getDrivers().isEmpty() && team.getEngineers().isEmpty(),
                "Invalid operations must not register entities.");
    }

    private static Team team() {
        return new Team("TEAM", "Apex", 2, 0);
    }

    private static F1Car hybrid(String id) {
        return new HybridRaceCar(id, "Apex", "Hybrid", 2026, 4, 2);
    }

    private static Driver driver(String license) {
        return new Driver("DRIVER-" + license, "Driver", license, 25, "5551234567");
    }

    private static Driver clearedDriver(String license) {
        Driver driver = driver(license);
        driver.setMedicalClearance(true, "Cleared");
        return driver;
    }

    private static Engineer engineer(String id) {
        return new Engineer(id, "Engineer", EngineerSpecialty.HYBRID_POWER_UNIT, 5, "5551234567");
    }

    private static void expectFailure(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName() + ".");
        } catch (RuntimeException failure) {
            if (!expected.isInstance(failure)) {
                throw new AssertionError("Expected " + expected.getSimpleName() + ".", failure);
            }
            check(expected == UnsupportedOperationException.class
                    || failure.getMessage() != null && !failure.getMessage().isBlank(),
                    "Rejected business actions must have a clear error message.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
