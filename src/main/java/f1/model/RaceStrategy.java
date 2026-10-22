package f1.model;

import util.Validation;

public class RaceStrategy {
    private final String strategyId;
    private final String chassisId;

    public RaceStrategy(String strategyId, String chassisId) {
        this.strategyId = Validation.requireText(strategyId, "Strategy ID");
        this.chassisId = Validation.requireText(chassisId, "Chassis ID");
    }

    public String getStrategyId() {
        return strategyId;
    }

    public String getChassisId() {
        return chassisId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof RaceStrategy strategy
                && strategyId.equals(strategy.strategyId)
                && chassisId.equals(strategy.chassisId);
    }

    @Override
    public int hashCode() {
        return 31 * strategyId.hashCode() + chassisId.hashCode();
    }

    @Override
    public String toString() {
        return "RaceStrategy{strategyId='" + strategyId + "', chassisId='" + chassisId + "'}";
    }
}
