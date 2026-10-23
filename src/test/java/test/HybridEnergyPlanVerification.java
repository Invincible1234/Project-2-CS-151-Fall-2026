package test;

import exception.RegulatoryViolationException;
import f1.model.HybridEnergyPlan;

public final class HybridEnergyPlanVerification {
    private static int checks;

    public static void main(String[] args) {
        HybridEnergyPlan plan = HybridEnergyPlan.parse(" deploy = 2.5 ; recover = 1 ");
        check(plan.getDeploymentMj() == 2.5 && plan.getRecoveryMj() == 1, "Plan values must be parsed.");
        check("DEPLOY=2.5;RECOVER=1.0".equals(plan.toString()), "Plan format must be canonical.");
        HybridEnergyPlan maximum = HybridEnergyPlan.parse("DEPLOY=4;RECOVER=9");
        check(maximum.getDeploymentMj() == 4 && maximum.getRecoveryMj() == 9, "Exact limits must be valid.");
        HybridEnergyPlan zero = HybridEnergyPlan.parse("DEPLOY=-0.0;RECOVER=0");
        check("DEPLOY=0.0;RECOVER=0.0".equals(zero.toString()), "Coasting plans and signed zero must normalize.");
        HybridEnergyPlan small = HybridEnergyPlan.parse("DEPLOY=1e-100;RECOVER=.5");
        check(HybridEnergyPlan.parse(small.toString()).getDeploymentMj() == small.getDeploymentMj(),
                "Canonical plans must support round-trip parsing.");
        for (String invalid : new String[] { null, "", "DEPLOY=2", "DEPLOY=2;RECOVER=1;",
                "RECOVER=1;DEPLOY=2", "DEPLOY=1;DEPLOY=2", "DEPLOY=;RECOVER=1",
                "DEPLOY=NaN;RECOVER=0", "DEPLOY=1;RECOVER=Infinity", "DEPLOY=-1;RECOVER=0",
                "DEPLOY=1;RECOVER=-1", "DEPLOY=1e999;RECOVER=0", "DEPLOY=0x1p0;RECOVER=0",
                "DEPLOY=-1e-999;RECOVER=0", "DEPLOY=1e-999;RECOVER=0" }) {
            expectFailure(IllegalArgumentException.class, invalid);
        }
        expectFailure(RegulatoryViolationException.class, "DEPLOY=4.01;RECOVER=0");
        expectFailure(RegulatoryViolationException.class, "DEPLOY=0;RECOVER=9.01");
        System.out.println("PASS: Hybrid energy plan format (" + checks + " checks)");
    }

    private static void expectFailure(Class<? extends RuntimeException> expected, String text) {
        try {
            HybridEnergyPlan.parse(text);
            throw new AssertionError("Expected " + expected.getSimpleName() + ".");
        } catch (RuntimeException error) {
            if (!expected.isInstance(error)) {
                throw new AssertionError("Unexpected rejection type.", error);
            }
            check(error.getMessage() != null && !error.getMessage().isBlank(), "Rejection must explain the error.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
