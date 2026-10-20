package f1.model;

public class ClassicRaceCar extends F1Car {
    private int originalityScore;
    private int restorationYear;
    private double powerLimitKw;

    public ClassicRaceCar(String chassisId, String teamName, String model, int year,
            int originalityScore, int restorationYear, double powerLimitKw) {
        super(chassisId, teamName, model, year);
        if (originalityScore < 0 || originalityScore > 100) {
            throw new IllegalArgumentException("Originality score must be between zero and 100.");
        }
        if (restorationYear < year) {
            throw new IllegalArgumentException("Restoration year cannot precede the car's year.");
        }
        if (!Double.isFinite(powerLimitKw) || powerLimitKw <= 0) {
            throw new IllegalArgumentException("Power limit must be finite and positive.");
        }
        this.originalityScore = originalityScore;
        this.restorationYear = restorationYear;
        this.powerLimitKw = powerLimitKw;
    }

    public int getOriginalityScore() {
        return originalityScore;
    }

    public int getRestorationYear() {
        return restorationYear;
    }

    public double getPowerLimitKw() {
        return powerLimitKw;
    }
}
