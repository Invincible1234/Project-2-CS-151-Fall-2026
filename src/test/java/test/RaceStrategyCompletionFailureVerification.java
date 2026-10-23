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

public final class RaceStrategyCompletionFailureVerification {
    public static void main(String[] args) {
        verifyRejectedCompletion();
        verifyMileageGuard();
        verifyManualEnergy();
        System.out.println("PASS: RaceStrategyCompletionFailureVerification (" + checks + " checks)");
    }

    private static void verifyRejectedCompletion() {
        Context context = fixture(null, true, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        HybridRaceCar hybrid = (HybridRaceCar) context.car();
        expectFailure(IllegalStateException.class, () -> hybrid.deployEnergyBoost(3));
        Map<String, Integer> stock = context.team().getPartsInventory(UserRole.ENGINEER);
        check(context.strategy().getStatus() == StrategyStatus.ACTIVE && context.car().isSessionActive()
                && context.car().getActiveStrategy() == context.strategy() && context.car().getTotalMileage() == 0
                && hybrid.getCurrentEnergyMJ() == 3 && !context.engineer().isAvailable()
                && context.team().getPartsInventory(UserRole.ENGINEER).equals(stock),
                "Rejected energy deployment must preserve mileage, stock, assignments, and the active commitment.");
        check(context.team().consumeParts("TIRES", 2), "Rejected deployment must preserve its reservation.");
        expectFailure(exception.InsufficientResourcesException.class,
                () -> context.team().consumeParts("TIRES", 1));
        Context completedLap = fixture(null, true, 0);
        completedLap.strategy().markReady();
        completedLap.strategy().startStrategy();
        expectFailure(IllegalStateException.class, ((HybridRaceCar) completedLap.car())::completeLap);
        check(completedLap.car().isSessionActive() && completedLap.car().getTotalMileage() == 0,
                "Manual lap completion must not bypass the managed strategy.");
    }

    private static void verifyMileageGuard() {
        Context context = fixture(null, true, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        expectFailure(IllegalStateException.class, () -> context.car().updateMileage(Double.MAX_VALUE));
        HybridRaceCar hybrid = (HybridRaceCar) context.car();
        check(hybrid.getCurrentEnergyMJ() == 3 && hybrid.getDeployedThisLapMJ() == 0
                && context.car().getTotalMileage() == 0
                && context.strategy().getStatus() == StrategyStatus.ACTIVE && !context.engineer().isAvailable()
                && context.team().getPartsInventory(UserRole.ENGINEER).get("TIRES") == 3,
                "Manual mileage must be rejected before energy, stock, or crew changes.");
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
        expectFailure(IllegalStateException.class, finishedCar::completeLap);
        check(finished.strategy().markComplete() && finishedCar.getCurrentEnergyMJ() == 2,
                "A strategy must complete without counting its manually applied energy twice.");
        Context nextLap = fixture(null, true, 0);
        nextLap.strategy().markReady();
        nextLap.strategy().startStrategy();
        HybridRaceCar nextCar = (HybridRaceCar) nextLap.car();
        expectFailure(IllegalStateException.class, nextCar::completeLap);
        expectFailure(IllegalStateException.class, nextCar::startNextLap);
        check(nextCar.getCurrentLapNumber() == 1 && nextLap.car().getTotalMileage() == 0,
                "A single-lap strategy must reject advancing to a different lap.");
        nextLap.strategy().markComplete();
        check(nextLap.car().getTotalMileage() == 5, "Rejected lap changes must leave completion usable.");
    }

}
