package f1.model;

import exception.RegulatoryViolationException;
import java.math.BigDecimal;
import java.util.Locale;

public final class HybridEnergyPlan {
    private final double deploymentMj;
    private final double recoveryMj;

    private HybridEnergyPlan(double deploymentMj, double recoveryMj) {
        this.deploymentMj = deploymentMj;
        this.recoveryMj = recoveryMj;
    }

    public static HybridEnergyPlan parse(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Use DEPLOY=<MJ>;RECOVER=<MJ>.");
        }
        String[] entries = text.strip().split(";", -1);
        if (entries.length != 2) {
            throw new IllegalArgumentException("Use DEPLOY=<MJ>;RECOVER=<MJ>.");
        }
        double deployment = parseEntry(entries[0], "DEPLOY");
        double recovery = parseEntry(entries[1], "RECOVER");
        if (deployment > 4.0 || recovery > 9.0) {
            throw new RegulatoryViolationException("Each lap permits at most 4 MJ deployment and 9 MJ recovery.");
        }
        return new HybridEnergyPlan(deployment, recovery);
    }

    public double getDeploymentMj() {
        return deploymentMj;
    }

    public double getRecoveryMj() {
        return recoveryMj;
    }

    private static double parseEntry(String entry, String expectedKey) {
        String[] pair = entry.split("=", -1);
        if (pair.length != 2 || !expectedKey.equals(pair[0].strip().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("Use DEPLOY=<MJ>;RECOVER=<MJ> in that order.");
        }
        String number = pair[1].strip();
        if (!number.matches("[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?")) {
            throw new IllegalArgumentException("Energy values must be finite, nonnegative numbers.");
        }
        double value;
        int sign;
        try {
            sign = new BigDecimal(number).signum();
            value = Double.parseDouble(number);
        } catch (NumberFormatException error) {
            throw new IllegalArgumentException("Energy values must be finite, nonnegative numbers.");
        }
        if (!Double.isFinite(value) || sign < 0 || value == 0 && sign > 0) {
            throw new IllegalArgumentException("Energy values must be representable, finite, nonnegative numbers.");
        }
        return value == 0 ? 0 : value;
    }

    @Override
    public String toString() {
        return "DEPLOY=" + deploymentMj + ";RECOVER=" + recoveryMj;
    }
}
