package test;

import exception.InsufficientResourcesException;
import exception.InvalidStrategyException;

import f1.model.Circuit;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.SessionType;
import f1.model.Team;
import f1.model.UserRole;

import model.StrategyStatus;
import model.WeatherCondition;

public final class RaceStrategyStartVerification {
    private static int checks;
    private static int nextId;

    public static void main(String[] args) {
        verifySuccessfulStart();
        verifyCompetingStock();
        verifySharedDriver();
        verifyStaleReadiness();
        verifyRollback();
        System.out.println("PASS: Race strategy start (" + checks + " checks)");
    }

    private static void verifySuccessfulStart() {
        Context context = fixture(new Team("START", "Apex", 2, 100000), null, null);
        expectFailure(InvalidStrategyException.class, context.strategy::startStrategy);
        context.strategy.markReady();
        double balance = context.team.getBudget(UserRole.TEAM_PRINCIPAL);
        check(
                context.car.scheduleStrategy(context.strategy),
                "Typed scheduling must start a ready strategy.");
        check(
                context.strategy.getStatus() == StrategyStatus.ACTIVE
                        && context.car.isSessionActive()
                        && context.car.getActiveStrategy() == context.strategy
                        && context.car
                                .getScheduledStrategy()
                                .equals(context.strategy.getStrategyId()),
                "Strategy and car must share the active commitment.");
        check(
                context.team.getPartsInventory(UserRole.TEAM_PRINCIPAL).get("TIRES") == 3
                        && context.team.getBudget(UserRole.TEAM_PRINCIPAL) == balance
                        && context.car.getCurrentEnergyMJ() == 3
                        && context.car.getTotalMileage() == 0,
                "Start must reserve existing resources without consuming parts, money, energy, or"
                        + " mileage.");
        check(context.team.consumeParts("TIRES", 2), "Unreserved stock must remain usable.");
        expectFailure(
                InsufficientResourcesException.class, () -> context.team.consumeParts("TIRES", 1));
        expectFailure(InvalidStrategyException.class, context.strategy::startStrategy);
        expectFailure(
                InvalidStrategyException.class,
                () -> context.strategy.addEnergyPlan("DEPLOY=1;RECOVER=0"));
        expectFailure(IllegalStateException.class, () -> context.team.removeCar(context.car));
        expectFailure(IllegalStateException.class, () -> context.team.removeDriver(context.driver));
        expectFailure(
                IllegalStateException.class, () -> context.team.removeEngineer(context.engineer));
        expectFailure(IllegalStateException.class, context.engineer::releaseFromCar);
        expectFailure(
                IllegalStateException.class, () -> context.driver.unassignFromCar(context.car));
        expectFailure(IllegalStateException.class, context.car::completeSession);
        expectFailure(
                IllegalStateException.class, () -> context.car.scheduleStrategy("Replacement"));
        check(
                context.car.getActiveStrategy() == context.strategy
                        && context.car.isSessionActive()
                        && context.team.getPartsInventory(UserRole.TEAM_PRINCIPAL).get("TIRES")
                                == 1,
                "Rejected actions must preserve active reservations and assignments.");
    }

    private static void verifyCompetingStock() {
        Team team = new Team("STOCK", "Apex", 2, 100000);
        Context first = fixture(team, null, null);
        Context second = fixture(team, null, null);
        team.consumeParts("TIRES", 3);
        first.strategy.addRequiredParts("TIRES", 1);
        second.strategy.addRequiredParts("TIRES", 1);
        first.strategy.markReady();
        second.strategy.markReady();
        first.strategy.startStrategy();
        expectFailure(InsufficientResourcesException.class, second.strategy::startStrategy);
        check(
                second.strategy.getStatus() == StrategyStatus.READY
                        && !second.car.isSessionActive()
                        && second.car.getActiveStrategy() == null,
                "Stock conflict must not partially start the second car.");
        check(
                team.consumeParts("TIRES", 1),
                "A failed start must not reserve any of the remaining stock.");
        team.addParts("TIRES", 2);
        check(
                second.strategy.startStrategy(),
                "A ready strategy may retry after stock is replenished.");
        expectFailure(InsufficientResourcesException.class, () -> team.consumeParts("TIRES", 1));
    }

    private static void verifySharedDriver() {
        Team team = new Team("DRIVER", "Apex", 2, 100000);
        Context first = fixture(team, null, null);
        Context second = fixture(team, null, first.driver);
        first.strategy.markReady();
        second.strategy.markReady();
        first.strategy.startStrategy();
        expectFailure(IllegalStateException.class, second.strategy::startStrategy);
        check(
                second.strategy.getStatus() == StrategyStatus.READY
                        && second.car.isRaceReady()
                        && !second.car.isSessionActive(),
                "A shared driver cannot be reserved for two races.");
    }

    private static void verifyStaleReadiness() {
        Context context = fixture(new Team("WEATHER", "Apex", 2, 100000), null, null);
        context.strategy.markReady();
        context.circuit.updateConditions(WeatherCondition.RAIN);
        expectFailure(InvalidStrategyException.class, context.strategy::startStrategy);
        check(
                context.strategy.getStatus() == StrategyStatus.READY
                        && !context.car.isSessionActive()
                        && context.car.getActiveStrategy() == null,
                "Readiness must be revalidated before reserving anything.");
        check(
                context.team.consumeParts("TIRES", 3),
                "Rejected weather validation must not reserve stock.");
    }

    private static void verifyRollback() {
        Context context = fixture(new Team("ROLLBACK", "Apex", 2, 100000), new RefusingCar(), null);
        context.strategy.markReady();
        double balance = context.team.getBudget(UserRole.TEAM_PRINCIPAL);
        expectFailure(IllegalStateException.class, context.strategy::startStrategy);
        check(
                context.strategy.getStatus() == StrategyStatus.READY
                        && context.car.isRaceReady()
                        && !context.car.isSessionActive()
                        && context.car.getActiveStrategy() == null
                        && context.team.getBudget(UserRole.TEAM_PRINCIPAL) == balance,
                "A refused session must preserve readiness, budget, and car state.");
        check(
                context.team.consumeParts("TIRES", 3),
                "A refused session must release all provisional stock reservations.");
    }

    private static Context fixture(Team team, HybridRaceCar suppliedCar, Driver suppliedDriver) {
        String id = Integer.toString(nextId++);
        HybridRaceCar car =
                suppliedCar == null
                        ? new HybridRaceCar("START-CAR-" + id, "Apex", "Hybrid", 2026, 4, 3)
                        : suppliedCar;
        Driver driver =
                suppliedDriver == null
                        ? new Driver(
                                "START-DRIVER-" + id,
                                "Driver",
                                "START-LICENSE-" + id,
                                25,
                                "5551234567")
                        : suppliedDriver;
        driver.setMedicalClearance(true, "Cleared");
        Engineer engineer =
                new Engineer(
                        "START-ENGINEER-" + id,
                        "Engineer",
                        f1.model.EngineerSpecialty.HYBRID_POWER_UNIT,
                        5,
                        "5551234567");
        team.addCar(car);
        if (suppliedDriver == null) {
            team.addDriver(driver);
        }
        team.addEngineer(engineer);
        team.assignDriver(driver, car);
        engineer.assignToCar(car);
        car.prepareForSession(SessionType.RACE);
        Circuit circuit =
                new Circuit(
                        "START-CIRCUIT-" + id,
                        "Circuit",
                        5,
                        12,
                        true,
                        WeatherCondition.CLEAR,
                        "Location");
        RaceStrategy strategy = new RaceStrategy("START-PLAN-" + id, car, engineer, circuit);
        team.addStrategy(strategy);
        strategy.adjustForWeather(WeatherCondition.CLEAR);
        strategy.addEnergyPlan("DEPLOY=2;RECOVER=1");
        strategy.addRequiredParts("TIRES", 1);
        strategy.finalizeLapTimeImpact(circuit);
        team.addParts("TIRES", 3);
        return new Context(team, car, driver, engineer, circuit, strategy);
    }

    private static void expectFailure(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName() + ".");
        } catch (RuntimeException error) {
            if (!expected.isInstance(error)) {
                throw new AssertionError("Unexpected rejection type.", error);
            }
            check(
                    error.getMessage() != null && !error.getMessage().isBlank(),
                    "Rejection must explain the error.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private static final class RefusingCar extends HybridRaceCar {
        private RefusingCar() {
            super("REFUSING-CAR", "Apex", "Hybrid", 2026, 4, 3);
        }

        @Override
        public void startSession() {
            throw new IllegalStateException("Session refused.");
        }
    }

    private record Context(
            Team team,
            HybridRaceCar car,
            Driver driver,
            Engineer engineer,
            Circuit circuit,
            RaceStrategy strategy) {}
}
