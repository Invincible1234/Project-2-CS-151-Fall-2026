package test;

import static test.StrategyCompletionFixture.check;
import static test.StrategyCompletionFixture.checks;
import static test.StrategyCompletionFixture.expectFailure;
import static test.StrategyCompletionFixture.fixture;

import f1.model.Team;
import f1.model.UserRole;

import test.StrategyCompletionFixture.Context;

import java.util.Map;

public final class RaceStrategyCompletionResourcesVerification {
    public static void main(String[] args) {
        verifyClassicCompletion();
        verifyCompetingReservation();
        System.out.println(
                "PASS: RaceStrategyCompletionResourcesVerification (" + checks + " checks)");
    }

    private static void verifyClassicCompletion() {
        Context context = fixture(null, false, 0);
        context.strategy().addRequiredParts("AERO_KIT", 1);
        context.team().addParts("AERO_KIT", 1);
        context.strategy().markReady();
        context.strategy().startStrategy();
        context.strategy().markComplete();
        check(
                context.car().getTotalMileage() == 5 && !context.car().isSessionActive(),
                "A classic strategy must complete without a hybrid plan.");
        check(
                context.team().getPartsInventory(UserRole.ENGINEER).equals(Map.of("TIRES", 2)),
                "Every reserved part must be consumed, including zero-stock keys.");
        check(context.team().consumeParts("TIRES", 2), "Completion must release all reservations.");
        check(
                context.engineer().isAvailable() && !context.car().hasActiveAssignments(),
                "Classic completion must release its crew.");
    }

    private static void verifyCompetingReservation() {
        Team team = new Team("SHARED", "Apex", 2, 100000);
        Context first = fixture(team, true, 0);
        Context second = fixture(team, true, 0);
        first.strategy().markReady();
        second.strategy().markReady();
        first.strategy().startStrategy();
        second.strategy().startStrategy();
        first.strategy().markComplete();
        check(
                second.car().getActiveStrategy() == second.strategy()
                        && second.car().isSessionActive(),
                "Completing one strategy must preserve the other active commitment.");
        team.consumeParts("TIRES", 4);
        expectFailure(
                exception.InsufficientResourcesException.class,
                () -> team.consumeParts("TIRES", 1));
        second.strategy().markComplete();
        check(
                team.getPartsInventory(UserRole.ENGINEER).isEmpty(),
                "Both strategies must consume only their stock.");
    }
}
