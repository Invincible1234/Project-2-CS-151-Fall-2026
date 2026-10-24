package test;

import static test.StrategyCompletionFixture.check;
import static test.StrategyCompletionFixture.checks;
import static test.StrategyCompletionFixture.expectFailure;
import static test.StrategyCompletionFixture.fixture;
import static test.StrategyCompletionFixture.strategy;

import exception.InsufficientResourcesException;

import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.Team;

import model.StrategyStatus;

import test.StrategyCompletionFixture.Context;

public final class RaceStrategyCancellationIsolationVerification {
    public static void main(String[] args) {
        verifyUntouchedEnergy();
        verifyOtherPlan();
        verifyOtherCar();
        verifyCancellationAfterRejectedActions();
        System.out.println("PASS: Strategy cancellation isolation (" + checks + " checks)");
    }

    private static void verifyUntouchedEnergy() {
        Context context = fixture(null, true, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        HybridRaceCar car = (HybridRaceCar) context.car();
        expectFailure(IllegalStateException.class, car::completeLap);
        expectFailure(IllegalStateException.class, car::startNextLap);
        context.strategy().cancelStrategy("Aborted after rejected manual lap changes");
        check(
                car.getCurrentEnergyMJ() == 3
                        && car.getDeployedThisLapMJ() == 0
                        && car.getRecoveredThisLapMJ() == 0
                        && car.getCurrentLapNumber() == 0
                        && !car.isLapCompleted()
                        && !car.isSessionActive(),
                "Cancellation must not apply an unused energy plan, after rejected lap changes.");
    }

    private static void verifyOtherPlan() {
        Context context = fixture(null, true, 0);
        RaceStrategy ready = strategy(context.team(), context.car(), context.engineer());
        ready.markReady();
        RaceStrategy draft = strategy(context.team(), context.car(), context.engineer());
        context.strategy().markReady();
        context.strategy().startStrategy();
        ready.cancelStrategy("Alternate plan withdrawn");
        draft.cancelStrategy("Unused draft");
        check(
                context.car().getActiveStrategy() == context.strategy()
                        && context.car().isSessionActive()
                        && !context.engineer().isAvailable()
                        && context.car().getAssignedDriver() == context.driver(),
                "Cancelling unused plans must not release another strategy's car or crew.");
        context.team().consumeParts("TIRES", 2);
        expectFailure(
                InsufficientResourcesException.class,
                () -> context.team().consumeParts("TIRES", 1));
        check(
                context.strategy().markComplete(),
                "The original active plan must remain completable.");
    }

    private static void verifyOtherCar() {
        Team team = new Team("CANCEL-SHARED", "Apex", 2, 100000);
        Context first = fixture(team, true, 0);
        Context second = fixture(team, false, 0);
        first.strategy().markReady();
        second.strategy().markReady();
        first.strategy().startStrategy();
        second.strategy().startStrategy();
        first.strategy().cancelStrategy("First car withdrawn");
        check(
                second.car().getActiveStrategy() == second.strategy()
                        && second.car().isSessionActive()
                        && !second.engineer().isAvailable(),
                "Cancellation must leave another car's commitment intact.");
        team.consumeParts("TIRES", 5);
        expectFailure(InsufficientResourcesException.class, () -> team.consumeParts("TIRES", 1));
        check(
                second.strategy().markComplete(),
                "Another car's parts must remain reserved until completion.");
    }

    private static void verifyCancellationAfterRejectedActions() {
        Context context = fixture(null, true, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        HybridRaceCar car = (HybridRaceCar) context.car();
        expectFailure(IllegalStateException.class, () -> car.deployEnergyBoost(3));
        expectFailure(IllegalStateException.class, car::completeLap);
        context.strategy().cancelStrategy("Manual energy and lap changes rejected");
        check(
                context.strategy().getStatus() == StrategyStatus.CANCELLED
                        && !car.isSessionActive()
                        && car.getCurrentEnergyMJ() == 3
                        && car.getTotalMileage() == 0,
                "Rejected manual actions must still allow cancellation without spending energy.");
        check(
                context.team().consumeParts("TIRES", 3),
                "Rejected actions followed by cancellation must free stock.");
    }
}
