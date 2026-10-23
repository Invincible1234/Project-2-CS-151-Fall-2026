package test;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;
import f1.model.ClassicRaceCar;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.Team;
import java.util.List;
import util.Constants;
import util.InstanceLimit;

public final class TeamRegistrationVerification {
    private static int checks;

    public static void main(String[] args) {
        verifyCarRegistration();
        verifyPersonnelRegistration();
        InstanceLimit.resetForTesting();
        verifyLimits();
        verifyInvalidInputs();
        System.out.println("PASS: Team registration (" + checks + " checks)");
    }

    private static void verifyCarRegistration() {
        Team team = new Team("CARS", "Apex", 2, 0);
        F1Car first = hybrid("CHASSIS-1");
        List<F1Car> before = team.getCars();
        check(team.addCar(first), "First car registration must succeed.");
        check(before.isEmpty(), "Returned collections must remain snapshots.");
        expectFailure(DuplicateEntityException.class, () -> team.addCar(first));
        expectFailure(DuplicateEntityException.class, () -> team.addCar(classic(" CHASSIS-1 ")));
        check(team.getCars().size() == 1 && team.getCars().get(0) == first,
                "Duplicate registration must preserve the existing car.");
        F1Car second = classic("CHASSIS-2");
        check(team.addCar(second), "Both car types must be accepted.");
        expectFailure(CapacityExceededException.class, () -> team.addCar(hybrid("CHASSIS-3")));
        expectFailure(DuplicateEntityException.class, () -> team.addCar(first));
        check(team.getCars().size() == 2 && team.getCars().get(1) == second,
                "Full-team rejections must leave registered cars unchanged.");
        check(team.getDrivers().isEmpty() && team.getEngineers().isEmpty(),
                "Car registration must not modify personnel collections.");
    }

    private static void verifyPersonnelRegistration() {
        Team team = new Team("PEOPLE", "Apex", 1, 0);
        Driver driver = driver(" LICENSE-1 ");
        Engineer engineer = engineer(" EMPLOYEE-1 ");
        List<Driver> driversBefore = team.getDrivers();
        List<Engineer> engineersBefore = team.getEngineers();
        check(team.addDriver(driver), "Driver registration must succeed.");
        check(team.addEngineer(engineer), "Engineer registration must succeed.");
        check("LICENSE-1".equals(driver.getLicenseNumber()), "License must be stripped.");
        check("EMPLOYEE-1".equals(engineer.getEmployeeId()), "Employee ID must be stripped.");
        check(driversBefore.isEmpty() && engineersBefore.isEmpty(),
                "Personnel snapshots must not change after registration.");
        expectFailure(DuplicateEntityException.class, () -> team.addDriver(driver));
        expectFailure(DuplicateEntityException.class,
                () -> team.addDriver(new Driver("OTHER", "Other driver", "LICENSE-1", 31, "5551234567")));
        expectFailure(DuplicateEntityException.class, () -> team.addEngineer(engineer));
        expectFailure(DuplicateEntityException.class,
                () -> team.addEngineer(new Engineer("EMPLOYEE-1", "Other engineer",
                        EngineerSpecialty.AERODYNAMICS, 3, "5551234567")));
        check(team.getDrivers().size() == 1 && team.getDrivers().get(0) == driver,
                "Duplicate licenses must not replace or add drivers.");
        check(team.getEngineers().size() == 1 && team.getEngineers().get(0) == engineer,
                "Duplicate employee IDs must not replace or add engineers.");
        expectFailure(UnsupportedOperationException.class, () -> team.getDrivers().add(driver));
        expectFailure(UnsupportedOperationException.class, () -> team.getEngineers().add(engineer));
        check(team.getCars().isEmpty(), "Personnel registration must not add cars.");
    }

    private static void verifyLimits() {
        Team team = new Team("LIMITS", "Apex", Constants.MAXIMUM_INSTANCES, 0);
        for (int i = 0; i < Constants.MAXIMUM_INSTANCES; i++) {
            check(team.addCar(hybrid("CAR-" + i)), "Cars within capacity must be accepted.");
            check(team.addDriver(driver("LICENSE-" + i)), "Drivers within the limit must be accepted.");
            check(team.addEngineer(engineer("EMPLOYEE-" + i)),
                    "Engineers within the limit must be accepted.");
        }
        expectFailure(CapacityExceededException.class, () -> team.addCar(hybrid("OVERFLOW")));
        expectFailure(CapacityExceededException.class, () -> team.addDriver(driver("OVERFLOW")));
        expectFailure(CapacityExceededException.class, () -> team.addEngineer(engineer("OVERFLOW")));
        expectFailure(DuplicateEntityException.class, () -> team.addDriver(team.getDrivers().get(0)));
        expectFailure(DuplicateEntityException.class, () -> team.addEngineer(team.getEngineers().get(0)));
        check(team.getCars().size() == Constants.MAXIMUM_INSTANCES
                && team.getDrivers().size() == Constants.MAXIMUM_INSTANCES
                && team.getEngineers().size() == Constants.MAXIMUM_INSTANCES,
                "Limit and duplicate failures must preserve all collection sizes.");

        InstanceLimit.resetForTesting();
        Team smallTeam = new Team("SMALL", "Apex", 1, 0);
        check(smallTeam.addDriver(driver("A")) && smallTeam.addDriver(driver("B")),
                "Car capacity must not act as the personnel limit.");
        check(smallTeam.addEngineer(engineer("A")) && smallTeam.addEngineer(engineer("B")),
                "Personnel types must have independent limits.");
    }

    private static void verifyInvalidInputs() {
        Team team = new Team("INVALID", "Apex", 1, 0);
        expectFailure(IllegalArgumentException.class, () -> team.addCar(null));
        expectFailure(IllegalArgumentException.class, () -> team.addDriver(null));
        expectFailure(IllegalArgumentException.class, () -> team.addEngineer(null));
        for (String invalid : new String[] { null, "", " \t\n", "\u2003" }) {
            expectFailure(IllegalArgumentException.class, () -> driver(invalid));
            expectFailure(IllegalArgumentException.class, () -> engineer(invalid));
        }
        check(team.getCars().isEmpty() && team.getDrivers().isEmpty() && team.getEngineers().isEmpty(),
                "Invalid inputs must not mutate any collection.");
    }

    private static F1Car hybrid(String chassisId) {
        return new HybridRaceCar(chassisId, "Apex", "Hybrid", 2026, 4, 2);
    }

    private static F1Car classic(String chassisId) {
        return new ClassicRaceCar(chassisId, "Apex", "Classic", 1970, 90, 2020, 300);
    }

    private static Driver driver(String license) {
        return new Driver("DRIVER-" + license, "Driver", license, 25, "5551234567");
    }

    private static Engineer engineer(String employeeId) {
        return new Engineer(employeeId, "Engineer", EngineerSpecialty.HYBRID_POWER_UNIT, 5,
                "5551234567");
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
                    "Rejection must have a clear error message.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
