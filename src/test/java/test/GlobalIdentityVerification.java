package test;

import static test.StrategyCompletionFixture.check;
import static test.StrategyCompletionFixture.expectFailure;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;

import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.HybridRaceCar;
import f1.model.Team;
import f1.service.CircuitRegistry;
import f1.ui.ConsoleState;

import model.WeatherCondition;

public final class GlobalIdentityVerification {
    public static void main(String[] args) {
        Team first = new Team("FIRST", "First", 2, 0);
        Team second = new Team("SECOND", "Second", 2, 0);
        HybridRaceCar car = new HybridRaceCar("CAR", "First", "Hybrid", 2026, 4, 3);
        first.addCar(car);
        expectFailure(
                DuplicateEntityException.class,
                () ->
                        second.addCar(
                                new ClassicRaceCar(
                                        "CAR", "Second", "Classic", 1980, 90, 2020, 250)));
        check(
                second.getCars().isEmpty() && car.getRegisteredTeam() == first,
                "Cross-team chassis rejection must preserve both teams.");
        first.removeCar(car);
        second.addCar(car);
        check(
                car.getRegisteredTeam() == second,
                "Removal must free the car for legitimate registration.");

        Driver canonical = driver("DRIVER", "LICENSE");
        first.addDriver(canonical);
        expectFailure(
                DuplicateEntityException.class, () -> second.addDriver(driver("OTHER", "LICENSE")));
        expectFailure(
                DuplicateEntityException.class,
                () -> second.addDriver(driver("DRIVER", "OTHER-LICENSE")));
        Driver independent = driver("OTHER", "OTHER-LICENSE");
        second.addDriver(independent);
        check(
                second.getDrivers().equals(java.util.List.of(independent)),
                "A rejected composite identity must not leak its otherwise unused key.");
        second.addDriver(canonical);
        first.removeDriver(canonical);
        expectFailure(
                DuplicateEntityException.class, () -> first.addDriver(driver("DRIVER", "LICENSE")));
        second.removeDriver(canonical);
        check(
                first.addDriver(driver("DRIVER", "LICENSE")),
                "The identity is reusable only after all canonical memberships are removed.");

        Engineer engineer = engineer("EMPLOYEE");
        first.addEngineer(engineer);
        expectFailure(
                DuplicateEntityException.class, () -> second.addEngineer(engineer("EMPLOYEE")));
        second.addEngineer(engineer);
        first.removeEngineer(engineer);
        expectFailure(
                DuplicateEntityException.class, () -> first.addEngineer(engineer("EMPLOYEE")));
        second.removeEngineer(engineer);
        first.addEngineer(engineer("EMPLOYEE"));

        Team full = new Team("FULL", "Full", 1, 0);
        full.addCar(new HybridRaceCar("FULL-CAR", "Full", "Hybrid", 2026, 4, 3));
        HybridRaceCar spare = new HybridRaceCar("SPARE", "First", "Hybrid", 2026, 4, 3);
        expectFailure(CapacityExceededException.class, () -> full.addCar(spare));
        check(first.addCar(spare), "Capacity rejection must not claim an unregistered identity.");

        ClassicRaceCar classic =
                new ClassicRaceCar("TRANSFER", "First", "Classic", 1980, 90, 2020, 250);
        first.addCar(classic);
        classic.transferOwnershipRecord(second);
        Team third = new Team("THIRD", "Third", 2, 0);
        expectFailure(
                DuplicateEntityException.class,
                () ->
                        third.addCar(
                                new ClassicRaceCar(
                                        "TRANSFER", "Third", "Classic", 1980, 90, 2020, 250)));
        check(
                classic.getRegisteredTeam() == second && !first.getCars().contains(classic),
                "A canonical transfer must move membership while retaining global identity"
                        + " protection.");

        CircuitRegistry tracks = new CircuitRegistry();
        tracks.addCircuit(circuit("TRACK"));
        expectFailure(
                DuplicateEntityException.class,
                () -> new CircuitRegistry().addCircuit(circuit("TRACK")));
        ConsoleState state = new ConsoleState();
        state.addTeam(first);
        expectFailure(
                DuplicateEntityException.class,
                () -> new ConsoleState().addTeam(new Team("FIRST", "Duplicate", 2, 0)));
        System.out.println("Global identity registration verification passed.");
    }

    private static Driver driver(String id, String license) {
        return new Driver(id, "Driver", license, 25, "5551234567");
    }

    private static Engineer engineer(String id) {
        return new Engineer(id, "Engineer", EngineerSpecialty.HYBRID_POWER_UNIT, 5, "5551234567");
    }

    private static Circuit circuit(String id) {
        return new Circuit(id, "Circuit", 5, 12, true, WeatherCondition.CLEAR, "Venue");
    }
}
