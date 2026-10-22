package f1.model;

import exception.RegulatoryViolationException;
import java.util.Locale;
import model.WeatherCondition;

public class HybridRaceCar extends F1Car {
    private static final double MAX_DEPLOYMENT_PER_LAP_MJ = 4.0;
    private static final double MAX_RECOVERY_PER_LAP_MJ = 9.0;
    private static final double MAX_MGU_K_POWER_KW = 350.0;
    private final double batteryCapacityMJ;
    private double currentEnergyMJ;
    private double deployedThisLapMJ;
    private double recoveredThisLapMJ;
    private double mguKPowerKw;
    private HybridUpgrade hybridUpgrade;
    private int currentLapNumber;
    private boolean lapCompleted;

    public HybridRaceCar(String chassisId, String teamName, String model, int year,
            double batteryCapacityMJ, double currentEnergyMJ) {
        this(chassisId, teamName, model, year, batteryCapacityMJ, currentEnergyMJ,
                HybridUpgrade.BALANCED.getPowerKw());
    }

    public HybridRaceCar(String chassisId, String teamName, String model, int year,
            double batteryCapacityMJ, double currentEnergyMJ, double mguKPowerKw) {
        super(chassisId, teamName, model, year);
        if (!Double.isFinite(batteryCapacityMJ) || batteryCapacityMJ <= 0) {
            throw new IllegalArgumentException("Battery capacity must be finite and positive.");
        }
        if (!Double.isFinite(currentEnergyMJ) || currentEnergyMJ < 0
                || currentEnergyMJ > batteryCapacityMJ) {
            throw new IllegalArgumentException("Battery energy must be between zero and capacity.");
        }
        this.batteryCapacityMJ = batteryCapacityMJ;
        this.currentEnergyMJ = currentEnergyMJ;
        validatePower(mguKPowerKw);
        this.mguKPowerKw = mguKPowerKw;
    }

    public double getBatteryCapacityMJ() {
        return batteryCapacityMJ;
    }

    public double getCurrentEnergyMJ() {
        return currentEnergyMJ;
    }

    public boolean deployEnergyBoost(double energyMJ) {
        if (!Double.isFinite(energyMJ) || energyMJ <= 0) {
            throw new IllegalArgumentException("Deployment energy must be finite and positive.");
        }
        requireActiveLap();
        if (energyMJ > currentEnergyMJ) {
            throw new IllegalArgumentException("Not enough battery energy for this boost.");
        }
        if (energyMJ > MAX_DEPLOYMENT_PER_LAP_MJ - deployedThisLapMJ) {
            throw new RegulatoryViolationException("Deployment cannot exceed 4 MJ per lap.");
        }
        currentEnergyMJ -= energyMJ;
        deployedThisLapMJ += energyMJ;
        return true;
    }

    public void chargeBatteryFromRecovery(double energyMJ) {
        if (!Double.isFinite(energyMJ) || energyMJ <= 0) {
            throw new IllegalArgumentException("Recovered energy must be finite and positive.");
        }
        requireActiveLap();
        double updatedEnergyMJ = currentEnergyMJ + energyMJ;
        if (!Double.isFinite(updatedEnergyMJ) || updatedEnergyMJ > batteryCapacityMJ) {
            throw new IllegalArgumentException("Recovered energy would exceed battery capacity.");
        }
        if (energyMJ > MAX_RECOVERY_PER_LAP_MJ - recoveredThisLapMJ) {
            throw new RegulatoryViolationException("Recovery cannot exceed 9 MJ per lap.");
        }
        currentEnergyMJ = updatedEnergyMJ;
        recoveredThisLapMJ += energyMJ;
    }

    public double getDeployedThisLapMJ() {
        return deployedThisLapMJ;
    }

    public double getRecoveredThisLapMJ() {
        return recoveredThisLapMJ;
    }

    @Override
    public void startSession() {
        super.startSession();
        clearLapCounters();
        currentLapNumber = 1;
        lapCompleted = false;
    }

    public void completeLap() {
        requireActiveLap();
        lapCompleted = true;
    }

    public void startNextLap() {
        if (!isSessionActive() || !lapCompleted) {
            throw new IllegalStateException("Complete the current lap before starting another.");
        }
        if (isDueForMaintenance() || currentLapNumber == Integer.MAX_VALUE) {
            throw new IllegalStateException("Cannot start another lap; end the session.");
        }
        clearLapCounters();
        currentLapNumber++;
        lapCompleted = false;
    }

    @Override
    public void completeSession() {
        super.completeSession();
        currentLapNumber = 0;
        lapCompleted = false;
    }

    public void resetEnergyState() {
        if (isSessionActive() || hasScheduledStrategy() || isPermanentlyDecommissioned()) {
            throw new IllegalStateException("Energy state can only be reset on an idle, uncommitted car.");
        }
        clearLapCounters();
        currentLapNumber = 0;
        lapCompleted = false;
    }

    public int getCurrentLapNumber() {
        return currentLapNumber;
    }

    public boolean isLapCompleted() {
        return lapCompleted;
    }

    private void clearLapCounters() {
        deployedThisLapMJ = 0;
        recoveredThisLapMJ = 0;
    }

    private void requireActiveLap() {
        if (!isSessionActive() || lapCompleted) {
            throw new IllegalStateException("Energy changes require an active, unfinished lap.");
        }
    }

    public boolean requiresSpecialistEngineer() {
        return true;
    }

    public double getMguKPowerKw() {
        return mguKPowerKw;
    }

    public String getHybridUpgrade() {
        return hybridUpgrade == null ? "" : hybridUpgrade.name();
    }

    @Override
    public double calculateLapTimeDelta(Circuit circuit, WeatherCondition weather) {
        double remainingDeployment = isSessionActive() && !lapCompleted
                ? MAX_DEPLOYMENT_PER_LAP_MJ - deployedThisLapMJ : MAX_DEPLOYMENT_PER_LAP_MJ;
        double usableEnergy = Math.min(currentEnergyMJ, remainingDeployment);
        return calculateLapTimeDelta(circuit, weather, usableEnergy);
    }

    public double calculateLapTimeDelta(Circuit circuit, WeatherCondition weather, double plannedDeploymentMj) {
        double baseline = performanceBaseline(circuit, weather);
        validatePower(mguKPowerKw);
        if (!Double.isFinite(plannedDeploymentMj) || plannedDeploymentMj < 0) {
            throw new IllegalArgumentException("Planned deployment must be finite and nonnegative.");
        }
        double remainingDeployment = isSessionActive() && !lapCompleted
                ? MAX_DEPLOYMENT_PER_LAP_MJ - deployedThisLapMJ : MAX_DEPLOYMENT_PER_LAP_MJ;
        if (plannedDeploymentMj > remainingDeployment) {
            throw new RegulatoryViolationException("Planned deployment exceeds the remaining lap allowance.");
        }
        if (plannedDeploymentMj > currentEnergyMJ) {
            throw new IllegalArgumentException("Planned deployment exceeds current battery charge.");
        }
        double powerFraction = mguKPowerKw / MAX_MGU_K_POWER_KW;
        boolean wet = weather == WeatherCondition.RAIN || weather == WeatherCondition.WET;
        // Simplified simulation: positive deltas are slower than the circuit baseline.
        double boostBenefit = plannedDeploymentMj / MAX_DEPLOYMENT_PER_LAP_MJ * powerFraction
                * (circuit.hasLongStraights() ? 0.06 : 0.015) * (wet ? 0.25 : 1);
        double fraction = weatherPenaltyFraction(weather) + aeroDeltaFraction(circuit, weather)
                + (1 - powerFraction) * 0.025 - boostBenefit;
        return lapTimeDelta(baseline, fraction);
    }

    @Override
    public String getRegulatoryComplianceNotes() {
        boolean withinLimits = mguKPowerKw <= MAX_MGU_K_POWER_KW
                && currentEnergyMJ >= 0 && currentEnergyMJ <= batteryCapacityMJ
                && deployedThisLapMJ <= MAX_DEPLOYMENT_PER_LAP_MJ
                && recoveredThisLapMJ <= MAX_RECOVERY_PER_LAP_MJ;
        return "Hybrid energy limits " + (withinLimits ? "satisfied" : "violated")
                + "; MGU-K " + mguKPowerKw + "/350 kW; battery " + currentEnergyMJ
                + "/" + batteryCapacityMJ + " MJ; deployed " + deployedThisLapMJ
                + "/4 MJ; recovered " + recoveredThisLapMJ + "/9 MJ this lap; specialist "
                + (getAssignedEngineer() == null ? "required before race execution" : getAssignedEngineerId());
    }

    public String getAssignedEngineerId() {
        Engineer engineer = getAssignedEngineer();
        return engineer == null ? "" : engineer.getEmployeeId();
    }

    public boolean lockForSpecialist(Engineer engineer) {
        if (engineer == null) {
            throw new IllegalArgumentException("A hybrid power-unit engineer is required.");
        }
        requireHybridChangesAllowed();
        if (getAssignedEngineer() != null) {
            throw new IllegalStateException("This car already has an assigned engineer.");
        }
        return engineer.assignToCar(this);
    }

    public void releaseSpecialist() {
        if (isSessionActive() || hasScheduledStrategy()) {
            throw new IllegalStateException("Complete or cancel the commitment before releasing the specialist.");
        }
        Engineer engineer = getAssignedEngineer();
        if (engineer == null) {
            throw new IllegalStateException("No specialist is assigned to this car.");
        }
        engineer.releaseFromCar();
    }

    public boolean applyHybridUpgrade(String upgradeType) {
        if (upgradeType == null || upgradeType.isBlank()) {
            throw new IllegalArgumentException("A hybrid upgrade is required.");
        }
        HybridUpgrade upgrade;
        try {
            upgrade = HybridUpgrade.valueOf(upgradeType.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("Choose EFFICIENCY, BALANCED, or PERFORMANCE.");
        }
        requireHybridChangesAllowed();
        Engineer engineer = getAssignedEngineer();
        if (engineer == null || engineer.isAvailable()
                || !getChassisId().equals(engineer.getAssignedChassisId())) {
            throw new IllegalStateException("An assigned hybrid specialist must approve the upgrade.");
        }
        validatePower(upgrade.getPowerKw());
        mguKPowerKw = upgrade.getPowerKw();
        hybridUpgrade = upgrade;
        invalidatePreparation();
        return true;
    }

    private void requireHybridChangesAllowed() {
        if (isSessionActive() || hasScheduledStrategy() || isPermanentlyDecommissioned()
                || isDueForMaintenance() || !getOutOfServiceReason().isEmpty()) {
            throw new IllegalStateException("Hybrid changes require an idle, serviceable, uncommitted car.");
        }
    }

    private static void validatePower(double powerKw) {
        if (!Double.isFinite(powerKw) || powerKw <= 0) {
            throw new IllegalArgumentException("MGU-K power must be finite and positive.");
        }
        if (powerKw > MAX_MGU_K_POWER_KW) {
            throw new RegulatoryViolationException("MGU-K power cannot exceed 350 kW.");
        }
    }
}
