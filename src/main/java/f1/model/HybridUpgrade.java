package f1.model;

public enum HybridUpgrade {
    EFFICIENCY(250.0),
    BALANCED(300.0),
    PERFORMANCE(350.0);

    private final double powerKw;

    HybridUpgrade(double powerKw) {
        this.powerKw = powerKw;
    }

    public double getPowerKw() {
        return powerKw;
    }
}
