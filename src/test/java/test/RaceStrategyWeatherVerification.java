package test;

import exception.InvalidStrategyException;
import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.Engineer;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.SessionType;
import java.util.List;
import model.TireCompound;
import model.WeatherCondition;

public final class RaceStrategyWeatherVerification {
    private static int checks;
    private static int nextId;

    public static void main(String[] args) {
        verifyTireCompatibility();
        verifyWeatherRecommendations();
        verifyRejectedWeatherInputs();
        System.out.println("PASS: Race strategy tire and weather (" + checks + " checks)");
    }

    private static void verifyTireCompatibility() {
        RaceStrategy strategy = hybridStrategy();
        for (WeatherCondition weather : WeatherCondition.values()) {
            strategy.adjustForWeather(weather);
            for (TireCompound compound : TireCompound.values()) {
                boolean dry = weather == WeatherCondition.CLEAR || weather == WeatherCondition.CLOUDY
                        || weather == WeatherCondition.EXTREME_HEAT;
                boolean compatible = dry && List.of(TireCompound.SOFT, TireCompound.MEDIUM,
                        TireCompound.HARD).contains(compound)
                        || weather == WeatherCondition.RAIN
                        && List.of(TireCompound.INTERMEDIATE, TireCompound.WET).contains(compound)
                        || weather == WeatherCondition.WET && compound == TireCompound.WET;
                if (compatible) {
                    strategy.selectTireCompound(compound);
                    check(strategy.getTireCompound() == compound, "Compatible tires must be selectable.");
                } else {
                    TireCompound before = strategy.getTireCompound();
                    expectFailure(InvalidStrategyException.class, () -> strategy.selectTireCompound(compound));
                    check(strategy.getTireCompound() == before, "Rejected tires must preserve the selection.");
                }
            }
        }
    }

    private static void verifyWeatherRecommendations() {
        RaceStrategy hybrid = hybridStrategy();
        Circuit circuit = hybrid.getCircuit();
        Circuit classicCircuit = circuit();
        F1Car classicCar = new ClassicRaceCar("CLASSIC", "Apex", "Classic", 1980, 90, 2020, 300);
        RaceStrategy classic = new RaceStrategy("CLASSIC-PLAN", classicCar, engineer(), classicCircuit);
        for (WeatherCondition weather : WeatherCondition.values()) {
            hybrid.adjustForWeather(weather);
            classic.adjustForWeather(weather);
            TireCompound expected = switch (weather) {
                case CLEAR, CLOUDY -> TireCompound.MEDIUM;
                case RAIN -> TireCompound.INTERMEDIATE;
                case WET -> TireCompound.WET;
                case EXTREME_HEAT -> TireCompound.HARD;
            };
            check(hybrid.getExpectedWeather() == weather && hybrid.getTireCompound() == expected,
                    "Weather adjustment must update both weather and appropriate tires.");
            check(classic.getRecommendedEnergyDeploymentMj() == 0,
                    "Classic strategies must never recommend hybrid energy deployment.");
            double recommendation = hybrid.getRecommendedEnergyDeploymentMj();
            check(recommendation >= 0 && recommendation <= 4
                    && recommendation <= ((HybridRaceCar) hybrid.getCar()).getCurrentEnergyMJ(),
                    "Recommendations must respect deployment and current charge limits.");
        }
        hybrid.adjustForWeather(WeatherCondition.CLEAR);
        double dryRecommendation = hybrid.getRecommendedEnergyDeploymentMj();
        hybrid.adjustForWeather(WeatherCondition.RAIN);
        double rainRecommendation = hybrid.getRecommendedEnergyDeploymentMj();
        hybrid.adjustForWeather(WeatherCondition.WET);
        check(dryRecommendation > rainRecommendation
                && rainRecommendation > hybrid.getRecommendedEnergyDeploymentMj(),
                "Wet-weather recommendations must become more conservative.");
        check(circuit.getTypicalWeather() == WeatherCondition.CLEAR
                && hybrid.getEnergyDeploymentPlan().isEmpty() && classic.getEnergyDeploymentPlan().isEmpty(),
                "Recommendations must not change circuit weather or create an actual energy plan.");
        HybridRaceCar emptyCar = new HybridRaceCar("EMPTY", "Apex", "Hybrid", 2026, 4, 0);
        RaceStrategy empty = new RaceStrategy("EMPTY-PLAN", emptyCar, engineer(), circuit());
        check(empty.getRecommendedEnergyDeploymentMj() == 0, "Empty batteries must not receive boost recommendations.");
        HybridRaceCar activeCar = new HybridRaceCar("ACTIVE", "Apex", "Hybrid", 2026, 8, 8);
        RaceStrategy activeCarDraft = new RaceStrategy("ACTIVE-CAR-DRAFT", activeCar, engineer(), circuit());
        activeCar.prepareForSession(SessionType.RACE);
        activeCar.startSession();
        activeCar.deployEnergyBoost(3);
        check(activeCarDraft.getRecommendedEnergyDeploymentMj() == 1,
                "Recommendations must respect deployment already used in the current lap.");
        activeCar.completeLap();
        check(activeCarDraft.getRecommendedEnergyDeploymentMj() == 4 && activeCar.getCurrentEnergyMJ() == 5,
                "A next-lap recommendation must not consume energy or retain the previous lap's allowance.");
    }

    private static void verifyRejectedWeatherInputs() {
        RaceStrategy strategy = hybridStrategy();
        strategy.adjustForWeather(WeatherCondition.CLEAR);
        strategy.selectTireCompound(TireCompound.SOFT);
        expectFailure(InvalidStrategyException.class, () -> strategy.selectTireCompound(null));
        expectFailure(InvalidStrategyException.class, () -> strategy.selectTireCompound(TireCompound.WET));
        expectFailure(IllegalArgumentException.class, () -> strategy.adjustForWeather(null));
        check(strategy.getExpectedWeather() == WeatherCondition.CLEAR
                && strategy.getTireCompound() == TireCompound.SOFT,
                "Rejected weather and tire changes must preserve the existing decisions.");
    }

    private static RaceStrategy hybridStrategy() {
        return new RaceStrategy("PLAN-" + nextId,
                new HybridRaceCar("CAR-" + nextId++, "Apex", "Hybrid", 2026, 4, 3), engineer(), circuit());
    }

    private static Engineer engineer() {
        return new Engineer("ENGINEER", "Engineer", "HYBRID_POWER_UNIT", 5, "5551234567");
    }

    private static Circuit circuit() {
        return new Circuit("CIRCUIT-" + nextId++, "Circuit", 5, 12, true, WeatherCondition.CLEAR, "Location");
    }

    private static void expectFailure(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName() + ".");
        } catch (RuntimeException failure) {
            if (!expected.isInstance(failure)) {
                throw new AssertionError("Expected " + expected.getSimpleName() + ".", failure);
            }
            check(failure.getMessage() != null && !failure.getMessage().isBlank(),
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
