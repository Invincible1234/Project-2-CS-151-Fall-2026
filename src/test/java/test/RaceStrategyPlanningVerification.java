package test;

import exception.InvalidStrategyException;

import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.Engineer;
import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;

import model.StrategyStatus;
import model.TireCompound;
import model.WeatherCondition;

import java.util.List;

public final class RaceStrategyPlanningVerification {
    private static int checks;
    private static int nextId;

    public static void main(String[] args) {
        verifyLapTimeImpact();
        verifyEstimateInvalidation();
        verifyRejectedChanges();
        verifyNumericLimits();
        System.out.println("PASS: Race strategy planning (" + checks + " checks)");
    }

    private static void verifyLapTimeImpact() {
        RaceStrategy strategy = hybridStrategy();
        Circuit circuit = strategy.getCircuit();
        circuit.recordHistoricalLap(90);
        circuit.recordHistoricalLap(110);
        strategy.selectTireCompound(TireCompound.MEDIUM);
        double carDelta = strategy.getCar().calculateLapTimeDelta(circuit, WeatherCondition.CLEAR);
        double medium = strategy.finalizeLapTimeImpact(circuit);
        check(
                close(medium, carDelta),
                "Medium tires must preserve the car's computed lap-time impact.");
        strategy.selectTireCompound(TireCompound.SOFT);
        double soft = strategy.finalizeLapTimeImpact(circuit);
        strategy.selectTireCompound(TireCompound.HARD);
        double hard = strategy.finalizeLapTimeImpact(circuit);
        check(
                soft < medium && medium < hard,
                "Dry tire decisions must affect estimated performance.");
        check(
                close(medium - soft, 1.5) && close(hard - medium, 1),
                "Tire impact must scale with the circuit's historical baseline.");
        strategy.adjustForWeather(WeatherCondition.EXTREME_HEAT);
        double heatHard = strategy.finalizeLapTimeImpact(circuit);
        strategy.selectTireCompound(TireCompound.SOFT);
        check(
                strategy.finalizeLapTimeImpact(circuit) > heatHard,
                "Hard tires must be preferable to soft tires in extreme heat.");
        for (WeatherCondition weather : List.of(WeatherCondition.RAIN, WeatherCondition.WET)) {
            strategy.adjustForWeather(weather);
            check(
                    close(
                            strategy.finalizeLapTimeImpact(circuit),
                            strategy.getCar().calculateLapTimeDelta(circuit, weather)),
                    "Weather penalties must come from the car override without being counted"
                            + " twice.");
        }

        RaceStrategy classic =
                new RaceStrategy(
                        "CLASSIC-IMPACT",
                        new ClassicRaceCar(
                                "CLASSIC-IMPACT", "Apex", "Classic", 1980, 90, 2020, 300),
                        engineer(),
                        circuit());
        classic.adjustForWeather(WeatherCondition.CLEAR);
        check(
                close(
                        classic.finalizeLapTimeImpact(classic.getCircuit()),
                        classic.getCar()
                                .calculateLapTimeDelta(
                                        classic.getCircuit(), WeatherCondition.CLEAR)),
                "Classic strategies must use their overridden car behavior.");
        check(
                strategy.getCar().getTotalMileage() == 0
                        && !strategy.getCar().isSessionActive()
                        && strategy.getStatus() == StrategyStatus.DRAFT
                        && strategy.getEngineer().isAvailable(),
                "Estimating performance must not run sessions, consume resources, or advance"
                        + " lifecycle state.");
    }

    private static void verifyEstimateInvalidation() {
        RaceStrategy strategy = hybridStrategy();
        strategy.adjustForWeather(WeatherCondition.CLEAR);
        double estimate = strategy.finalizeLapTimeImpact(strategy.getCircuit());
        check(
                strategy.hasFinalizedLapTimeEstimate()
                        && close(strategy.getEstimatedLapTimeDelta(), estimate),
                "Finalization must store the calculated estimate.");
        strategy.selectTireCompound(TireCompound.MEDIUM);
        check(
                strategy.hasFinalizedLapTimeEstimate(),
                "Reselecting identical tires must preserve a valid estimate.");
        strategy.selectTireCompound(TireCompound.SOFT);
        check(
                !strategy.hasFinalizedLapTimeEstimate() && strategy.getEstimatedLapTimeDelta() == 0,
                "A different tire decision must invalidate the previous estimate.");
        strategy.finalizeLapTimeImpact(strategy.getCircuit());
        strategy.adjustForWeather(WeatherCondition.RAIN);
        check(
                !strategy.hasFinalizedLapTimeEstimate() && strategy.getEstimatedLapTimeDelta() == 0,
                "Weather changes must invalidate the previous estimate.");
        strategy.finalizeLapTimeImpact(strategy.getCircuit());
        check(
                strategy.hasFinalizedLapTimeEstimate(),
                "Updated decisions must support recalculation.");
    }

    private static void verifyRejectedChanges() {
        RaceStrategy strategy = hybridStrategy();
        expectFailure(
                InvalidStrategyException.class,
                () -> strategy.finalizeLapTimeImpact(strategy.getCircuit()));
        strategy.adjustForWeather(WeatherCondition.CLEAR);
        double before = strategy.finalizeLapTimeImpact(strategy.getCircuit());
        expectFailure(InvalidStrategyException.class, () -> strategy.selectTireCompound(null));
        expectFailure(
                InvalidStrategyException.class,
                () -> strategy.selectTireCompound(TireCompound.WET));
        expectFailure(IllegalArgumentException.class, () -> strategy.adjustForWeather(null));
        expectFailure(IllegalArgumentException.class, () -> strategy.finalizeLapTimeImpact(null));
        expectFailure(
                IllegalArgumentException.class, () -> strategy.finalizeLapTimeImpact(circuit()));
        Circuit sameId =
                new Circuit(
                        strategy.getCircuit().getCircuitId(),
                        "Different layout",
                        7,
                        18,
                        false,
                        WeatherCondition.CLEAR,
                        "Location");
        expectFailure(IllegalArgumentException.class, () -> strategy.finalizeLapTimeImpact(sameId));
        check(
                strategy.hasFinalizedLapTimeEstimate()
                        && close(strategy.getEstimatedLapTimeDelta(), before)
                        && strategy.getExpectedWeather() == WeatherCondition.CLEAR
                        && strategy.getTireCompound() == TireCompound.MEDIUM,
                "Rejected inputs must leave the existing planning state unchanged.");
        strategy.getCar().markOutOfService("Inspection required");
        expectFailure(
                IllegalStateException.class,
                () -> strategy.finalizeLapTimeImpact(strategy.getCircuit()));
        check(
                close(strategy.getEstimatedLapTimeDelta(), before),
                "A car-level failure must preserve the saved estimate.");
    }

    private static void verifyNumericLimits() {
        RaceStrategy strategy =
                new RaceStrategy(
                        "OVERFLOW",
                        new ClassicRaceCar("OVERFLOW-CAR", "Apex", "Classic", 1980, 90, 2020, 300),
                        engineer(),
                        circuit());
        strategy.adjustForWeather(WeatherCondition.CLEAR);
        double before = strategy.finalizeLapTimeImpact(strategy.getCircuit());
        strategy.getCircuit().recordHistoricalLap(Double.MAX_VALUE);
        expectFailure(
                IllegalArgumentException.class,
                () -> strategy.finalizeLapTimeImpact(strategy.getCircuit()));
        check(
                close(strategy.getEstimatedLapTimeDelta(), before),
                "An overflowing projected lap must not overwrite the previous estimate.");
    }

    private static RaceStrategy hybridStrategy() {
        return new RaceStrategy(
                "PLAN-" + nextId,
                new HybridRaceCar("CAR-" + nextId++, "Apex", "Hybrid", 2026, 4, 3),
                engineer(),
                circuit());
    }

    private static Engineer engineer() {
        return new Engineer(
                "ENGINEER",
                "Engineer",
                f1.model.EngineerSpecialty.HYBRID_POWER_UNIT,
                5,
                "5551234567");
    }

    private static Circuit circuit() {
        return new Circuit(
                "CIRCUIT-" + nextId++, "Circuit", 5, 12, true, WeatherCondition.CLEAR, "Location");
    }

    private static boolean close(double first, double second) {
        return Math.abs(first - second) < 1.0e-9;
    }

    private static void expectFailure(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName() + ".");
        } catch (RuntimeException failure) {
            if (!expected.isInstance(failure)) {
                throw new AssertionError("Expected " + expected.getSimpleName() + ".", failure);
            }
            check(
                    failure.getMessage() != null && !failure.getMessage().isBlank(),
                    "Rejected decisions must have a clear error message.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
