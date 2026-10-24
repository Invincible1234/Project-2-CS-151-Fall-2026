package test;

import static test.StrategyCompletionFixture.check;
import static test.StrategyCompletionFixture.checks;
import static test.StrategyCompletionFixture.expectFailure;
import static test.StrategyCompletionFixture.fixture;
import static test.StrategyCompletionFixture.strategy;

import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.SessionType;
import f1.model.UserRole;

import model.StrategyStatus;

import test.StrategyCompletionFixture.Context;

import java.util.Map;

public final class RaceStrategyActiveCancellationVerification {
    public static void main(String[] args) {
        verifyCancellation(true);
        verifyCancellation(false);
        System.out.println("PASS: Active strategy cancellation (" + checks + " checks)");
    }

    private static void verifyCancellation(boolean hybrid) {
        Context context = fixture(null, hybrid, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        if (hybrid) {
            ((HybridRaceCar) context.car()).deployEnergyBoost(1);
            ((HybridRaceCar) context.car()).chargeBatteryFromRecovery(0.5);
        }
        double budget = context.team().getBudget(UserRole.TEAM_PRINCIPAL);
        Map<String, Integer> stock = context.team().getPartsInventory(UserRole.ENGINEER);
        for (String reason : new String[] {null, "", "  "}) {
            expectFailure(
                    IllegalArgumentException.class,
                    () -> context.strategy().cancelStrategy(reason));
            check(
                    context.strategy().getStatus() == StrategyStatus.ACTIVE
                            && context.car().isSessionActive()
                            && context.car().getActiveStrategy() == context.strategy()
                            && !context.engineer().isAvailable()
                            && context.team().getPartsInventory(UserRole.ENGINEER).equals(stock),
                    "Invalid active cancellation must preserve the commitment and reservation.");
        }
        context.strategy().cancelStrategy("  Safety stop  ");
        check(
                context.strategy().getStatus() == StrategyStatus.CANCELLED
                        && context.strategy().getCancellationReason().equals("Safety stop"),
                "An active strategy must record its cancellation reason.");
        check(
                !context.car().isSessionActive()
                        && !context.car().isRaceReady()
                        && !context.car().hasScheduledStrategy()
                        && context.car().getActiveStrategy() == null
                        && context.car().getPreparedSessionType() == null
                        && context.car().getTotalMileage() == 0,
                "Cancellation must close the commitment without adding completed mileage.");
        check(
                context.engineer().isAvailable()
                        && context.engineer().getAssignedChassisId().isEmpty()
                        && !context.car().hasActiveAssignments()
                        && context.driver().getAssignedChassisIds().isEmpty(),
                "Active cancellation must release both personnel assignments.");
        check(
                context.team().getPartsInventory(UserRole.ENGINEER).equals(stock)
                        && context.team().getBudget(UserRole.TEAM_PRINCIPAL) == budget
                        && context.team().getRaceResults().isEmpty()
                        && context.team().getChampionshipPoints() == 0,
                "Cancellation must not consume stock, charge money, or record a result.");
        if (hybrid) {
            HybridRaceCar car = (HybridRaceCar) context.car();
            check(
                    car.getCurrentEnergyMJ() == 2.5
                            && car.getDeployedThisLapMJ() == 1
                            && car.getRecoveredThisLapMJ() == 0.5
                            && car.getCurrentLapNumber() == 0
                            && !car.isLapCompleted(),
                    "Cancellation must preserve used energy and close the hybrid lap.");
        }
        check(
                context.team().consumeParts("TIRES", 3),
                "All cancelled reservations must be released.");
        context.team().addParts("TIRES", 1);
        context.team().assignDriver(context.driver(), context.car());
        context.engineer().assignToCar(context.car());
        check(
                context.car().prepareForSession(SessionType.RACE),
                "The cancelled car must support preparation again.");
        RaceStrategy replacement = strategy(context.team(), context.car(), context.engineer());
        replacement.markReady();
        replacement.startStrategy();
        check(
                replacement.markComplete(),
                "Released personnel and stock must support a replacement strategy.");
    }
}
