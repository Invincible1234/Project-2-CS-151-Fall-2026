package test;

import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.SessionType;
import f1.model.Team;

import model.WeatherCondition;

final class StrategyCompletionFixture {
    static int checks;
    private static int nextId;

    static Context fixture(Team existing, boolean hybrid, double mileage) {
        String id = Integer.toString(nextId++);
        Team team = existing == null ? new Team("TEAM-" + id, "Apex", 2, 100000) : existing;
        F1Car car =
                hybrid
                        ? new HybridRaceCar("CAR-" + id, "Apex", "Hybrid", 2026, 4, 3)
                        : new ClassicRaceCar("CAR-" + id, "Apex", "Classic", 1980, 90, 2020, 250);
        car.updateMileage(mileage);
        Driver driver = new Driver("DRIVER-" + id, "Driver", "LICENSE-" + id, 25, "5551234567");
        driver.setMedicalClearance(true, "Cleared");
        Engineer engineer =
                new Engineer(
                        "ENGINEER-" + id,
                        "Engineer",
                        hybrid
                                ? EngineerSpecialty.HYBRID_POWER_UNIT
                                : EngineerSpecialty.CLASSIC_RESTORATION,
                        5,
                        "5551234567");
        team.addCar(car);
        team.addDriver(driver);
        team.addEngineer(engineer);
        team.assignDriver(driver, car);
        engineer.assignToCar(car);
        car.prepareForSession(SessionType.RACE);
        team.addParts("TIRES", 3);
        return new Context(team, car, driver, engineer, strategy(team, car, engineer));
    }

    static RaceStrategy strategy(Team team, F1Car car, Engineer engineer) {
        Circuit circuit =
                new Circuit(
                        "CIRCUIT-" + nextId++,
                        "Circuit",
                        5,
                        12,
                        true,
                        WeatherCondition.CLEAR,
                        "Location");
        RaceStrategy strategy = new RaceStrategy("PLAN-" + nextId++, car, engineer, circuit);
        team.addStrategy(strategy);
        strategy.adjustForWeather(WeatherCondition.CLEAR);
        if (car instanceof HybridRaceCar) {
            strategy.addEnergyPlan("DEPLOY=2;RECOVER=1");
        }
        strategy.addRequiredParts("TIRES", 1);
        strategy.finalizeLapTimeImpact(circuit);
        return strategy;
    }

    static void expectFailure(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName());
        } catch (RuntimeException error) {
            if (!expected.isInstance(error)) {
                throw new AssertionError("Unexpected rejection type", error);
            }
            check(
                    error.getMessage() != null && !error.getMessage().isBlank(),
                    "Rejection must explain the error.");
        }
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    record Context(Team team, F1Car car, Driver driver, Engineer engineer, RaceStrategy strategy) {}
}
