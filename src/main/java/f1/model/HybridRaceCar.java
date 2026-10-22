package f1.model;

import exception.RegulatoryViolationException;

public class HybridRaceCar extends F1Car {
    private static final double MAX_DEPLOYMENT_PER_LAP_MJ = 4.0;
    private static final double MAX_RECOVERY_PER_LAP_MJ = 9.0;
    private final double batteryCapacityMJ;
    private double currentEnergyMJ;
    private double deployedThisLapMJ;
    private double recoveredThisLapMJ;

    public HybridRaceCar(String chassisId, String teamName, String model, int year,
            double batteryCapacityMJ, double currentEnergyMJ) {
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

    public void startNextLap() {
        deployedThisLapMJ = 0;
        recoveredThisLapMJ = 0;
    }

    public boolean requiresSpecialistEngineer() {
        return true;
    }
}
