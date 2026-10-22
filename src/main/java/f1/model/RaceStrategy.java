package f1.model;

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

    public String getCancellationReason() {
        return cancellationReason;
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
