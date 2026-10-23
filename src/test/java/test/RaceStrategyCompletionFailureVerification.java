package test;

import f1.model.HybridRaceCar;
import f1.model.UserRole;
import java.util.Map;
import model.StrategyStatus;
import test.StrategyCompletionFixture.Context;
import static test.StrategyCompletionFixture.check;
import static test.StrategyCompletionFixture.checks;
import static test.StrategyCompletionFixture.expectFailure;
import static test.StrategyCompletionFixture.fixture;
import static test.StrategyCompletionFixture.strategy;

public final class RaceStrategyCompletionFailureVerification {
    public static void main(String[] args) {
        verifyRejectedCompletion();
        verifyMileageFailure();
        verifyManualEnergy();
        System.out.println("PASS: RaceStrategyCompletionFailureVerification (" + checks + " checks)");
    }

    private static void verifyRejectedCompletion() {
        Context context = fixture(null, true, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        HybridRaceCar hybrid = (HybridRaceCar) context.car();
        hybrid.deployEnergyBoost(3);
        Map<String, Integer> stock = context.team().getPartsInventory(UserRole.ENGINEER);
        expectFailure(IllegalStateException.class, context.strategy()::markComplete);
        check(context.strategy().getStatus() == StrategyStatus.ACTIVE && context.car().isSessionActive()
                && context.car().getActiveStrategy() == context.strategy() && context.car().getTotalMileage() == 0
                && hybrid.getCurrentEnergyMJ() == 0 && !context.engineer().isAvailable()
                && context.team().getPartsInventory(UserRole.ENGINEER).equals(stock),
                "Rejected energy completion must preserve mileage, stock, assignments, and the active commitment.");
        check(context.team().consumeParts("TIRES", 2), "Rejected completion must preserve its reservation.");
        expectFailure(exception.InsufficientResourcesException.class,
                () -> context.team().consumeParts("TIRES", 1));
        Context completedLap = fixture(null, true, 0);
        completedLap.strategy().markReady();
        completedLap.strategy().startStrategy();
        ((HybridRaceCar) completedLap.car()).completeLap();
        expectFailure(IllegalStateException.class, completedLap.strategy()::markComplete);
        check(completedLap.car().isSessionActive() && completedLap.car().getTotalMileage() == 0,
                "A finished lap with unapplied energy must not silently apply more energy.");
    }

    private static void verifyMileageFailure() {
        Context context = fixture(null, true, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        context.car().updateMileage(Double.MAX_VALUE);
        HybridRaceCar hybrid = (HybridRaceCar) context.car();
        expectFailure(IllegalArgumentException.class, context.strategy()::markComplete);
        check(hybrid.getCurrentEnergyMJ() == 3 && hybrid.getDeployedThisLapMJ() == 0
                && context.strategy().getStatus() == StrategyStatus.ACTIVE && !context.engineer().isAvailable()
                && context.team().getPartsInventory(UserRole.ENGINEER).get("TIRES") == 3,
                "Invalid mileage must be rejected before energy, stock, or crew changes.");
    }

    private static void verifyManualEnergy() {
        Context context = fixture(null, true, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        HybridRaceCar hybrid = (HybridRaceCar) context.car();
        hybrid.deployEnergyBoost(1);
        hybrid.chargeBatteryFromRecovery(0.5);
        context.strategy().markComplete();
        check(hybrid.getCurrentEnergyMJ() == 2 && hybrid.getDeployedThisLapMJ() == 2
                && hybrid.getRecoveredThisLapMJ() == 1, "Already applied energy must not be counted twice.");
        Context finished = fixture(null, true, 0);
        finished.strategy().markReady();
        finished.strategy().startStrategy();
        HybridRaceCar finishedCar = (HybridRaceCar) finished.car();
        finishedCar.deployEnergyBoost(2);
        finishedCar.chargeBatteryFromRecovery(1);
        finishedCar.completeLap();
        check(finished.strategy().markComplete() && finishedCar.getCurrentEnergyMJ() == 2,
                "A manually finished lap must complete when its energy plan is already satisfied.");
        Context nextLap = fixture(null, true, 0);
        nextLap.strategy().markReady();
        nextLap.strategy().startStrategy();
        HybridRaceCar nextCar = (HybridRaceCar) nextLap.car();
        nextCar.completeLap();
        nextCar.startNextLap();
        expectFailure(IllegalStateException.class, nextLap.strategy()::markComplete);
        check(nextCar.getCurrentLapNumber() == 2 && nextLap.car().getTotalMileage() == 0,
                "A single-lap strategy must not silently complete a different lap.");
    }

}
