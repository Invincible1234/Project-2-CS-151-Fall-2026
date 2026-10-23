package f1.model;

import exception.InvalidStrategyException;
import exception.RegulatoryViolationException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import model.StrategyStatus;
import model.TireCompound;
import model.WeatherCondition;
import util.InstanceLimit;
import util.PartsCatalog;
import util.Validation;

public final class RaceStrategy {
    private final String strategyId;
    private final String chassisId;
    private final String engineerId;
    private final F1Car car;
    private final Engineer engineer;
    private final Circuit circuit;
    private Team registeredTeam;
    private Driver raceDriver;
    private StrategyStatus status;
    private double estimatedLapTimeDelta;
    private boolean lapTimeEstimateFinalized;
    private TireCompound tireCompound;
    private String energyDeploymentPlan;
    private HybridEnergyPlan hybridEnergyPlan;
    private final List<String> partsUsed;
    private final Map<String, Integer> requiredParts = new HashMap<>();
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

    public Driver getRaceDriver() {
        return raceDriver;
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

    public boolean hasFinalizedLapTimeEstimate() {
        return lapTimeEstimateFinalized;
    }

    public TireCompound getTireCompound() {
        return tireCompound;
    }

    public String getEnergyDeploymentPlan() {
        return energyDeploymentPlan;
    }

    public boolean hasEnergyPlan() {
        return hybridEnergyPlan != null;
    }

    public double getPlannedDeploymentMj() {
        return hybridEnergyPlan == null ? 0 : hybridEnergyPlan.getDeploymentMj();
    }

    public double getPlannedRecoveryMj() {
        return hybridEnergyPlan == null ? 0 : hybridEnergyPlan.getRecoveryMj();
    }

    public boolean addEnergyPlan(String plan) {
        requireEditableDraft();
        if (!(car instanceof HybridRaceCar hybrid)) {
            throw new InvalidStrategyException("Energy deployment plans require a hybrid car.");
        }
        HybridEnergyPlan candidate = HybridEnergyPlan.parse(plan);
        validateEnergyPlan(candidate, hybrid);
        hybridEnergyPlan = candidate;
        energyDeploymentPlan = candidate.toString();
        invalidateLapTimeEstimate();
        return true;
    }

    public List<String> getPartsUsed() {
        return List.copyOf(partsUsed);
    }

    public Map<String, Integer> getRequiredParts() {
        return Map.copyOf(requiredParts);
    }

    public void addRequiredParts(String partType, int quantity) {
        requireEditableDraft();
        String part = PartsCatalog.normalizePartType(partType);
        int current = requiredParts.getOrDefault(part, 0);
        if (quantity <= 0 || quantity > Integer.MAX_VALUE - current) {
            throw new IllegalArgumentException("Required part quantity must be positive and fit the supported range.");
        }
        requiredParts.put(part, current + quantity);
        if (current == 0) {
            partsUsed.add(part);
        }
    }

    public boolean markReady() {
        requireEditableDraft();
        double estimate = validateReadiness();
        estimatedLapTimeDelta = estimate;
        lapTimeEstimateFinalized = true;
        status = StrategyStatus.READY;
        return true;
    }

    public boolean startStrategy() {
        if (status != StrategyStatus.READY) {
            throw new InvalidStrategyException("Only a ready strategy can start.");
        }
        double estimate = validateReadiness();
        registeredTeam.reserveStrategyResources(this, requiredParts, car.getAssignedDriver());
        try {
            car.beginStrategySession(this);
        } catch (RuntimeException error) {
            registeredTeam.releaseStrategyResources(this);
            throw error;
        }
        estimatedLapTimeDelta = estimate;
        lapTimeEstimateFinalized = true;
        raceDriver = car.getAssignedDriver();
        status = StrategyStatus.ACTIVE;
        return true;
    }

    public boolean markComplete() {
        if (status != StrategyStatus.ACTIVE) {
            throw new InvalidStrategyException("Only an active strategy can complete.");
        }
        double distanceKm = circuit.getLengthKm();
        car.validateStrategyCompletion(this, distanceKm);
        Driver driver = car.getAssignedDriver();
        if (registeredTeam == null || car.getRegisteredTeam() != registeredTeam
                || driver == null || driver != raceDriver || !driver.getAssignedChassisIds().contains(chassisId)
                || car.getAssignedEngineer() != engineer
                || !chassisId.equals(engineer.getAssignedChassisId())) {
            throw new InvalidStrategyException("Repair strategy ownership and assignments before completion.");
        }
        registeredTeam.validateReservedParts(this);
        if (car instanceof HybridRaceCar hybrid) {
            hybrid.validateStrategyEnergyCompletion(hybridEnergyPlan);
            hybrid.finishStrategyEnergy(hybridEnergyPlan);
        }
        car.finishStrategySession(this, distanceKm);
        registeredTeam.consumeReservedParts(this);
        engineer.releaseFromCar();
        driver.unassignFromCar(car);
        status = StrategyStatus.COMPLETED;
        return true;
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
            invalidateLapTimeEstimate();
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
            invalidateLapTimeEstimate();
        }
    }

    public double finalizeLapTimeImpact(Circuit circuit) {
        requireEditableDraft();
        if (circuit == null || circuit != this.circuit) {
            throw new IllegalArgumentException("Use the circuit linked to this strategy.");
        }
        double delta = calculateLapTimeImpact();
        estimatedLapTimeDelta = delta;
        lapTimeEstimateFinalized = true;
        return delta;
    }

    private double calculateLapTimeImpact() {
        requireCompatibleTires(tireCompound, expectedWeather);
        double baseline = circuit.estimateBaseLapTime();
        if (!Double.isFinite(baseline) || baseline <= 0) {
            throw new InvalidStrategyException("Circuit baseline must be finite and positive.");
        }
        double carDelta;
        if (car instanceof HybridRaceCar hybrid && hybridEnergyPlan != null) {
            validateEnergyPlan(hybridEnergyPlan, hybrid);
            carDelta = hybrid.calculateLapTimeDelta(circuit, expectedWeather, hybridEnergyPlan.getDeploymentMj());
        } else {
            carDelta = car.calculateLapTimeDelta(circuit, expectedWeather);
        }
        double tireFraction = switch (tireCompound) {
            case SOFT -> expectedWeather == WeatherCondition.EXTREME_HEAT ? 0.03 : -0.015;
            case MEDIUM -> expectedWeather == WeatherCondition.EXTREME_HEAT ? 0.01 : 0;
            case HARD -> expectedWeather == WeatherCondition.EXTREME_HEAT ? 0 : 0.01;
            case INTERMEDIATE -> 0;
            case WET -> expectedWeather == WeatherCondition.RAIN ? 0.02 : 0;
        };
        double delta = carDelta + baseline * tireFraction;
        double projectedLapTime = baseline + delta;
        if (!Double.isFinite(carDelta) || !Double.isFinite(delta)
                || !Double.isFinite(projectedLapTime) || projectedLapTime <= 0) {
            throw new InvalidStrategyException("Estimated lap time must be finite and positive.");
        }
        return delta;
    }

    private double validateReadiness() {
        if (registeredTeam == null || car.getRegisteredTeam() != registeredTeam) {
            throw new InvalidStrategyException("Register the strategy and its car with the same team.");
        }
        if (!car.isAvailableForRace() || car.getPreparedSessionType() != SessionType.RACE
                || car.hasScheduledStrategy()) {
            throw new InvalidStrategyException("Prepare an available, uncommitted car for a race.");
        }
        Driver driver = car.getAssignedDriver();
        if (driver == null || !driver.isEligibleForAssignment()
                || !driver.getAssignedChassisIds().contains(chassisId)) {
            throw new InvalidStrategyException("An eligible driver must be assigned to this car.");
        }
        driver.requireReleasableAssignments();
        if (car.getAssignedEngineer() != engineer || engineer.isAvailable()
                || !chassisId.equals(engineer.getAssignedChassisId()) || !engineer.isQualifiedFor(car)) {
            throw new InvalidStrategyException("A qualified engineer must be assigned to this car.");
        }
        if (expectedWeather != circuit.getTypicalWeather()) {
            throw new InvalidStrategyException("Update the strategy for the circuit's current weather.");
        }
        if (!lapTimeEstimateFinalized || requiredParts.getOrDefault("TIRES", 0) < 1) {
            throw new InvalidStrategyException("Finalize the lap estimate and add required tires before readiness.");
        }
        if (car instanceof HybridRaceCar hybrid) {
            if (hybridEnergyPlan == null) {
                throw new InvalidStrategyException("A hybrid strategy requires a validated energy plan.");
            }
            validateEnergyPlan(hybridEnergyPlan, hybrid);
        }
        double estimate = calculateLapTimeImpact();
        registeredTeam.validateStrategyResources(this, requiredParts, driver);
        return estimate;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    private static void validateEnergyPlan(HybridEnergyPlan plan, HybridRaceCar hybrid) {
        if (!Double.isFinite(hybrid.getMguKPowerKw()) || hybrid.getMguKPowerKw() <= 0
                || hybrid.getMguKPowerKw() > 350.0) {
            throw new RegulatoryViolationException("Hybrid power must be positive and at most 350 kW.");
        }
        boolean currentLap = hybrid.isSessionActive() && !hybrid.isLapCompleted();
        double deployed = currentLap ? hybrid.getDeployedThisLapMJ() : 0;
        double recovered = currentLap ? hybrid.getRecoveredThisLapMJ() : 0;
        if (plan.getDeploymentMj() > 4.0 - deployed || plan.getRecoveryMj() > 9.0 - recovered) {
            throw new RegulatoryViolationException("Energy plan exceeds the remaining lap allowances.");
        }
        double charge = hybrid.getCurrentEnergyMJ();
        if (plan.getDeploymentMj() > charge) {
            throw new InvalidStrategyException("Planned deployment exceeds current battery charge.");
        }
        double finalCharge = charge - plan.getDeploymentMj() + plan.getRecoveryMj();
        if (!Double.isFinite(finalCharge) || finalCharge < 0 || finalCharge > hybrid.getBatteryCapacityMJ()) {
            throw new InvalidStrategyException("Energy plan would exceed the battery capacity.");
        }
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

    private void invalidateLapTimeEstimate() {
        estimatedLapTimeDelta = 0;
        lapTimeEstimateFinalized = false;
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
