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
import model.TireCompound;
import model.WeatherCondition;

import java.util.Map;

public final class RaceStrategyReadinessVerification {
    private static int checks;
    private static int nextId;

    public static void main(String[] args) {
        Context ready = fixture(true, true);
        double balance = ready.team.getBudget(UserRole.TEAM_PRINCIPAL);
        check(
                ready.strategy.markReady() && ready.strategy.getStatus() == StrategyStatus.READY,
                "A complete strategy must become ready.");
        check(
                !ready.car.isSessionActive()
                        && ready.car.getActiveStrategy() == null
                        && ready.team.getBudget(UserRole.TEAM_PRINCIPAL) == balance,
                "Readiness must not start sessions, reserve a car, or charge the budget.");
        check(ready.team.consumeParts("TIRES", 3), "Readiness alone must not reserve stock.");
        expectUnchanged(ready, InvalidStrategyException.class, ready.strategy::markReady);
        expectUnchanged(
                ready,
                InvalidStrategyException.class,
                () -> ready.strategy.selectTireCompound(TireCompound.SOFT));
        expectUnchanged(
                ready,
                InvalidStrategyException.class,
                () -> ready.strategy.adjustForWeather(WeatherCondition.RAIN));
        expectUnchanged(
                ready,
                InvalidStrategyException.class,
                () -> ready.strategy.addEnergyPlan("DEPLOY=1;RECOVER=0"));
        expectUnchanged(
                ready,
                InvalidStrategyException.class,
                () -> ready.strategy.addRequiredParts("TIRES", 1));

        Context missingPlan = fixture(false, true);
        expectUnchanged(
                missingPlan, InvalidStrategyException.class, missingPlan.strategy::markReady);
        Context missingTires = fixture(true, false);
        expectUnchanged(
                missingTires, InvalidStrategyException.class, missingTires.strategy::markReady);
        Context staleEstimate = fixture(true, true);
        staleEstimate.strategy.selectTireCompound(TireCompound.SOFT);
        expectUnchanged(
                staleEstimate, InvalidStrategyException.class, staleEstimate.strategy::markReady);
        Context weather = fixture(true, true);
        weather.circuit.updateConditions(WeatherCondition.RAIN);
        expectUnchanged(weather, InvalidStrategyException.class, weather.strategy::markReady);
        Context medical = fixture(true, true);
        medical.driver.setMedicalClearance(false, "Not cleared");
        expectUnchanged(medical, InvalidStrategyException.class, medical.strategy::markReady);
        Context engineer = fixture(true, true);
        engineer.team.removeEngineer(engineer.engineer);
        expectUnchanged(engineer, InvalidStrategyException.class, engineer.strategy::markReady);
        Context service = fixture(true, true);
        service.car.updateMileage(500);
        expectUnchanged(service, InvalidStrategyException.class, service.strategy::markReady);
        Context stock = fixture(true, true);
        stock.team.consumeParts("TIRES", 3);
        expectUnchanged(stock, InsufficientResourcesException.class, stock.strategy::markReady);
        Context unprepared = fixture(true, true);
        unprepared.car.performService("Inspection");
        expectUnchanged(unprepared, InvalidStrategyException.class, unprepared.strategy::markReady);
        Context practice = fixture(true, true);
        practice.car.performService("Inspection");
        practice.car.prepareForSession(SessionType.PRACTICE);
        expectUnchanged(practice, InvalidStrategyException.class, practice.strategy::markReady);

        Context parts = fixture(true, true);
        parts.strategy.addRequiredParts(" tires ", 2);
        check(
                parts.strategy.getRequiredParts().equals(Map.of("TIRES", 3))
                        && parts.strategy.getPartsUsed().size() == 1,
                "Required quantities must accumulate without duplicate keys.");
        expectUnchanged(
                parts,
                IllegalArgumentException.class,
                () -> parts.strategy.addRequiredParts("TIRES", 0));
        expectUnchanged(
                parts,
                IllegalArgumentException.class,
                () -> parts.strategy.addRequiredParts("TIRES", Integer.MAX_VALUE));
        expectUnchanged(
                parts,
                IllegalArgumentException.class,
                () -> parts.strategy.addRequiredParts("UNKNOWN", 1));
        expectUnchanged(
                parts,
                UnsupportedOperationException.class,
                () -> parts.strategy.getRequiredParts().put("TIRES", 0));
        check(parts.strategy.markReady(), "Exact required stock must support readiness.");
        System.out.println("PASS: Race strategy readiness (" + checks + " checks)");
    }

    private static Context fixture(boolean energyPlan, boolean tires) {
        String id = Integer.toString(nextId++);
        Team team = new Team("TEAM-" + id, "Apex", 2, 100000);
        HybridRaceCar car = new HybridRaceCar("CAR-" + id, "Apex", "Hybrid", 2026, 4, 3);
        Driver driver = new Driver("DRIVER-" + id, "Driver", "LICENSE-" + id, 25, "5551234567");
        driver.setMedicalClearance(true, "Cleared");
        Engineer engineer =
                new Engineer(
                        "ENGINEER-" + id,
                        "Engineer",
                        f1.model.EngineerSpecialty.HYBRID_POWER_UNIT,
                        5,
                        "5551234567");
        team.addCar(car);
        team.addDriver(driver);
        team.addEngineer(engineer);
        team.assignDriver(driver, car);
        engineer.assignToCar(car);
        car.prepareForSession(SessionType.RACE);
        Circuit circuit =
                new Circuit(
                        "CIRCUIT-" + id,
                        "Circuit",
                        5,
                        12,
                        true,
                        WeatherCondition.CLEAR,
                        "Location");
        RaceStrategy strategy = new RaceStrategy("PLAN-" + id, car, engineer, circuit);
        team.addStrategy(strategy);
        strategy.adjustForWeather(WeatherCondition.CLEAR);
        if (energyPlan) {
            strategy.addEnergyPlan("DEPLOY=2;RECOVER=1");
        }
        if (tires) {
            strategy.addRequiredParts("TIRES", 1);
        }
        strategy.finalizeLapTimeImpact(circuit);
        team.addParts("TIRES", 3);
        return new Context(team, car, driver, engineer, circuit, strategy);
    }

    private static void expectUnchanged(
            Context context, Class<? extends RuntimeException> expected, Runnable action) {
        StrategyStatus status = context.strategy.getStatus();
        double estimate = context.strategy.getEstimatedLapTimeDelta();
        boolean finalized = context.strategy.hasFinalizedLapTimeEstimate();
        boolean prepared = context.car.isRaceReady();
        double balance = context.team.getBudget(UserRole.TEAM_PRINCIPAL);
        Map<String, Integer> stock = context.team.getPartsInventory(UserRole.TEAM_PRINCIPAL);
        Map<String, Integer> requirements = context.strategy.getRequiredParts();
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName() + ".");
        } catch (RuntimeException error) {
            if (!expected.isInstance(error)) {
                throw new AssertionError("Unexpected rejection type.", error);
            }
            checks++;
        }
        check(
                context.strategy.getStatus() == status
                        && context.strategy.getEstimatedLapTimeDelta() == estimate
                        && context.strategy.hasFinalizedLapTimeEstimate() == finalized
                        && context.car.isRaceReady() == prepared
                        && !context.car.isSessionActive()
                        && context.team.getBudget(UserRole.TEAM_PRINCIPAL) == balance
                        && context.team.getPartsInventory(UserRole.TEAM_PRINCIPAL).equals(stock)
                        && context.strategy.getRequiredParts().equals(requirements),
                "Rejected readiness or editing must leave planning and resources unchanged.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }

    private record Context(
            Team team,
            HybridRaceCar car,
            Driver driver,
            Engineer engineer,
            Circuit circuit,
            RaceStrategy strategy) {}
}
