package test;

import exception.InvalidStrategyException;
import exception.RegulatoryViolationException;

import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.Engineer;
import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.SessionType;

import model.StrategyStatus;
import model.WeatherCondition;

public final class RaceStrategyEnergyVerification {
    private static int checks;
    private static int nextId;

    public static void main(String[] args) {
        verifyPlans();
        verifyRejectedPlans();
        verifyLapAllowances();
        verifyForecasts();
        System.out.println("PASS: Race strategy energy (" + checks + " checks)");
    }

    private static void verifyPlans() {
        HybridRaceCar car = car(4, 3);
        RaceStrategy strategy = strategy(car);
        check(!strategy.hasEnergyPlan(), "New drafts must not claim an energy plan.");
        check(
                strategy.addEnergyPlan(" deploy = 2 ; recover = 1 "),
                "Valid hybrid plans must be accepted.");
        check(
                strategy.hasEnergyPlan()
                        && "DEPLOY=2.0;RECOVER=1.0".equals(strategy.getEnergyDeploymentPlan())
                        && strategy.getPlannedDeploymentMj() == 2
                        && strategy.getPlannedRecoveryMj() == 1,
                "Stored plans must expose their normalized decisions.");
        check(
                car.getCurrentEnergyMJ() == 3
                        && car.getDeployedThisLapMJ() == 0
                        && car.getRecoveredThisLapMJ() == 0
                        && !car.isSessionActive()
                        && strategy.getStatus() == StrategyStatus.DRAFT
                        && strategy.getEngineer().isAvailable(),
                "Planning must not execute energy actions, reserve personnel, or advance strategy"
                        + " state.");
        RaceStrategy maximum = strategy(car(12, 4));
        check(
                maximum.addEnergyPlan("DEPLOY=4;RECOVER=9"),
                "Exact deployment and recovery limits must be accepted.");
        check(
                strategy(car(4, 0)).addEnergyPlan("DEPLOY=0;RECOVER=4"),
                "An empty battery may plan recovery without deployment.");
        check(
                strategy(car(4, 4)).addEnergyPlan("DEPLOY=0;RECOVER=0"),
                "An explicit coasting plan must be valid.");
        RaceStrategy classic =
                new RaceStrategy(
                        "CLASSIC-ENERGY",
                        new ClassicRaceCar(
                                "CLASSIC-ENERGY", "Apex", "Classic", 1980, 90, 2020, 300),
                        engineer(),
                        circuit());
        expectFailure(
                InvalidStrategyException.class, () -> classic.addEnergyPlan("DEPLOY=0;RECOVER=0"));
        check(
                !classic.hasEnergyPlan() && classic.getEnergyDeploymentPlan().isEmpty(),
                "Classic cars must reject even zero-value hybrid plans.");
    }

    private static void verifyRejectedPlans() {
        HybridRaceCar car = car(4, 3);
        RaceStrategy strategy = strategy(car);
        strategy.adjustForWeather(WeatherCondition.CLEAR);
        strategy.addEnergyPlan("DEPLOY=1;RECOVER=1");
        double estimate = strategy.finalizeLapTimeImpact(strategy.getCircuit());
        for (String invalid : new String[] {null, "", "DEPLOY=1", "DEPLOY=-1;RECOVER=0"}) {
            expectFailure(IllegalArgumentException.class, () -> strategy.addEnergyPlan(invalid));
        }
        expectFailure(
                RegulatoryViolationException.class,
                () -> strategy.addEnergyPlan("DEPLOY=5;RECOVER=0"));
        expectFailure(
                RegulatoryViolationException.class,
                () -> strategy.addEnergyPlan("DEPLOY=0;RECOVER=10"));
        expectFailure(
                InvalidStrategyException.class, () -> strategy.addEnergyPlan("DEPLOY=4;RECOVER=0"));
        expectFailure(
                InvalidStrategyException.class, () -> strategy.addEnergyPlan("DEPLOY=0;RECOVER=2"));
        check(
                "DEPLOY=1.0;RECOVER=1.0".equals(strategy.getEnergyDeploymentPlan())
                        && strategy.hasFinalizedLapTimeEstimate()
                        && close(strategy.getEstimatedLapTimeDelta(), estimate)
                        && car.getCurrentEnergyMJ() == 3
                        && car.getDeployedThisLapMJ() == 0
                        && car.getRecoveredThisLapMJ() == 0,
                "Rejected plans must preserve the existing plan, estimate, and car energy state.");
        check(
                strategy.addEnergyPlan("DEPLOY=2;RECOVER=1")
                        && !strategy.hasFinalizedLapTimeEstimate()
                        && strategy.getEstimatedLapTimeDelta() == 0,
                "Replacing a plan must invalidate its forecast.");
        strategy.adjustForWeather(WeatherCondition.RAIN);
        check(
                strategy.getPlannedDeploymentMj() == 2
                        && strategy.getRecommendedEnergyDeploymentMj() == 1,
                "Weather advice must not silently overwrite an explicit energy plan.");
    }

    private static void verifyLapAllowances() {
        HybridRaceCar car = car(20, 10);
        RaceStrategy strategy = strategy(car);
        car.prepareForSession(SessionType.PRACTICE);
        car.startSession();
        car.deployEnergyBoost(3);
        car.chargeBatteryFromRecovery(8);
        expectFailure(
                RegulatoryViolationException.class,
                () -> strategy.addEnergyPlan("DEPLOY=2;RECOVER=0"));
        expectFailure(
                RegulatoryViolationException.class,
                () -> strategy.addEnergyPlan("DEPLOY=1;RECOVER=2"));
        check(
                strategy.addEnergyPlan("DEPLOY=1;RECOVER=1"),
                "Plans must fit the remaining lap allowances.");
        check(
                car.getCurrentEnergyMJ() == 15
                        && car.getDeployedThisLapMJ() == 3
                        && car.getRecoveredThisLapMJ() == 8,
                "Plan validation must not change actual lap counters.");
        car.completeLap();
        check(
                strategy.addEnergyPlan("DEPLOY=4;RECOVER=9"),
                "Next-lap plans must use fresh allowances while preserving the current charge.");
        check(
                car.getCurrentEnergyMJ() == 15,
                "Next-lap planning must not apply recovery in advance.");
    }

    private static void verifyForecasts() {
        HybridRaceCar car = car(8, 4);
        RaceStrategy strategy = strategy(car);
        strategy.adjustForWeather(WeatherCondition.CLEAR);
        strategy.addEnergyPlan("DEPLOY=0;RECOVER=0");
        double coasting = strategy.finalizeLapTimeImpact(strategy.getCircuit());
        strategy.addEnergyPlan("DEPLOY=4;RECOVER=0");
        double boost = strategy.finalizeLapTimeImpact(strategy.getCircuit());
        check(
                boost < coasting,
                "A real planned boost must improve the forecast compared with coasting.");
        check(
                close(
                        boost,
                        car.calculateLapTimeDelta(strategy.getCircuit(), WeatherCondition.CLEAR)),
                "Maximum planned deployment must agree with the existing car forecast.");
        check(
                car.getCurrentEnergyMJ() == 4,
                "Forecasting must not consume the planned deployment.");
        car.prepareForSession(SessionType.PRACTICE);
        car.startSession();
        car.deployEnergyBoost(1);
        expectFailure(
                RegulatoryViolationException.class,
                () -> strategy.finalizeLapTimeImpact(strategy.getCircuit()));
        check(
                close(strategy.getEstimatedLapTimeDelta(), boost),
                "A stale plan rejected during revalidation must not overwrite its saved forecast.");
        strategy.addEnergyPlan("DEPLOY=1;RECOVER=0");
        double revised = strategy.finalizeLapTimeImpact(strategy.getCircuit());
        check(
                Double.isFinite(revised)
                        && car.getCurrentEnergyMJ() == 3
                        && car.getDeployedThisLapMJ() == 1,
                "Revised planning must use current state without deploying additional energy.");
        expectFailure(
                IllegalArgumentException.class,
                () ->
                        car.calculateLapTimeDelta(
                                strategy.getCircuit(), WeatherCondition.CLEAR, Double.NaN));
        expectFailure(
                RegulatoryViolationException.class,
                () -> car.calculateLapTimeDelta(strategy.getCircuit(), WeatherCondition.CLEAR, 4));
    }

    private static HybridRaceCar car(double capacity, double charge) {
        return new HybridRaceCar(
                "ENERGY-CAR-" + nextId++, "Apex", "Hybrid", 2026, capacity, charge, 350);
    }

    private static RaceStrategy strategy(HybridRaceCar car) {
        return new RaceStrategy("ENERGY-PLAN-" + nextId++, car, engineer(), circuit());
    }

    private static Engineer engineer() {
        return new Engineer(
                "ENERGY-ENGINEER",
                "Engineer",
                f1.model.EngineerSpecialty.HYBRID_POWER_UNIT,
                5,
                "5551234567");
    }

    private static Circuit circuit() {
        return new Circuit(
                "ENERGY-CIRCUIT-" + nextId++,
                "Circuit",
                5,
                12,
                true,
                WeatherCondition.CLEAR,
                "Location");
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) < 1.0e-9;
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
}
