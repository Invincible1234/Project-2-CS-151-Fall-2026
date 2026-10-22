package f1.model;

public class HybridRaceCar extends F1Car {
    private final double batteryCapacityMJ;
    private double currentEnergyMJ;

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
        currentEnergyMJ -= energyMJ;
        return true;
    }

    public boolean requiresSpecialistEngineer() {
        return true;
    }
}
