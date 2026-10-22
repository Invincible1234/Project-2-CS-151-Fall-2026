package f1.model;

import exception.InvalidStrategyException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import model.StrategyStatus;
import model.TireCompound;
import model.WeatherCondition;
import util.InstanceLimit;
import util.Validation;

public final class RaceStrategy {
    private final String strategyId;
    private final String chassisId;
    private final String engineerId;
    private final F1Car car;
    private final Engineer engineer;
    private final Circuit circuit;
    private Team registeredTeam;
    private StrategyStatus status;
    private double estimatedLapTimeDelta;
    private TireCompound tireCompound;
    private String energyDeploymentPlan;
    private final List<String> partsUsed;
    private WeatherCondition expectedWeather;
    private String cancellationReason;

    public RaceStrategy(String strategyId, F1Car car, Engineer engineer, Circuit circuit) {
        this.strategyId = Validation.requireText(strategyId, "Strategy ID");
        if (car == null || engineer == null || circuit == null) {
            throw new IllegalArgumentException("Strategy car, engineer, and circuit are required.");
        }
        this.chassisId = Validation.requireText(car.getChassisId(), "Chassis ID");
        this.engineerId = Validation.requireText(engineer.getEmployeeId(), "Engineer ID");
        Validation.requireText(circuit.getCircuitId(), "Circuit ID");
        if (circuit.getTypicalWeather() == null) {
            throw new IllegalArgumentException("Circuit weather is required.");
        }
        this.car = car;
        this.engineer = engineer;
        this.circuit = circuit;
        this.status = StrategyStatus.DRAFT;
        this.estimatedLapTimeDelta = 0;
        this.tireCompound = null;
        this.energyDeploymentPlan = "";
        this.partsUsed = new ArrayList<>();
        this.expectedWeather = circuit.getTypicalWeather();
        this.cancellationReason = "";
        InstanceLimit.checkAndCount(RaceStrategy.class);
    }

    public String getStrategyId() {
        return strategyId;
    }

    public String getChassisId() {
        return chassisId;
    }

    public String getEngineerId() {
        return engineerId;
    }

    public F1Car getCar() {
        return car;
    }

    public Engineer getEngineer() {
        return engineer;
    }

    public Circuit getCircuit() {
        return circuit;
    }

    public Team getRegisteredTeam() {
        return registeredTeam;
    }

    public StrategyStatus getStatus() {
        return status;
    }

    public double getEstimatedLapTimeDelta() {
        return estimatedLapTimeDelta;
    }

    public TireCompound getTireCompound() {
        return tireCompound;
    }

    public String getEnergyDeploymentPlan() {
        return energyDeploymentPlan;
    }

    public List<String> getPartsUsed() {
        return List.copyOf(partsUsed);
    }

    public WeatherCondition getExpectedWeather() {
        return expectedWeather;
    }

    public double getRecommendedEnergyDeploymentMj() {
        if (!(car instanceof HybridRaceCar hybrid)) {
            return 0;
        }
        double recommendation = switch (expectedWeather) {
            case CLEAR, CLOUDY -> 4.0;
            case RAIN -> 1.0;
            case WET -> 0.5;
            case EXTREME_HEAT -> 2.0;
        };
        double remainingDeployment = hybrid.isSessionActive() && !hybrid.isLapCompleted()
                ? Math.max(0, 4.0 - hybrid.getDeployedThisLapMJ()) : 4.0;
        return Math.min(recommendation, Math.min(remainingDeployment, hybrid.getCurrentEnergyMJ()));
    }

    public void selectTireCompound(TireCompound compound) {
        requireEditableDraft();
        requireCompatibleTires(compound, expectedWeather);
        if (tireCompound != compound) {
            tireCompound = compound;
        }
    }

    public void adjustForWeather(WeatherCondition weather) {
        requireEditableDraft();
        if (weather == null) {
            throw new IllegalArgumentException("Weather is required.");
        }
        TireCompound recommendedTires = switch (weather) {
            case CLEAR, CLOUDY -> TireCompound.MEDIUM;
            case RAIN -> TireCompound.INTERMEDIATE;
            case WET -> TireCompound.WET;
            case EXTREME_HEAT -> TireCompound.HARD;
        };
        if (expectedWeather != weather || tireCompound != recommendedTires) {
            expectedWeather = weather;
            tireCompound = recommendedTires;
        }
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    private void requireEditableDraft() {
        if (status != StrategyStatus.DRAFT) {
            throw new InvalidStrategyException("Only a draft strategy can change planning decisions.");
        }
    }

    private static void requireCompatibleTires(TireCompound compound, WeatherCondition weather) {
        if (compound == null) {
            throw new InvalidStrategyException("Select a tire compound before finalizing the strategy.");
        }
        boolean compatible = switch (weather) {
            case CLEAR, CLOUDY, EXTREME_HEAT -> compound == TireCompound.SOFT
                    || compound == TireCompound.MEDIUM || compound == TireCompound.HARD;
            case RAIN -> compound == TireCompound.INTERMEDIATE || compound == TireCompound.WET;
            case WET -> compound == TireCompound.WET;
        };
        if (!compatible) {
            throw new InvalidStrategyException("Tire compound is incompatible with the expected weather.");
        }
    }

    void registerWithTeam(Team team) {
        if (team == null) {
            throw new IllegalArgumentException("Team is required.");
        }
        if (registeredTeam != null) {
            throw new IllegalStateException("Strategy is already registered with a team.");
        }
        registeredTeam = team;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof RaceStrategy strategy
                && strategyId.equals(strategy.strategyId) && chassisId.equals(strategy.chassisId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(strategyId, chassisId);
    }

    @Override
    public String toString() {
        return "RaceStrategy{strategyId='" + strategyId + "', chassisId='" + chassisId
                + "', engineerId='" + engineerId + "', circuitId='" + circuit.getCircuitId()
                + "', status=" + status + "}";
    }
}
