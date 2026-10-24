package test;

import static test.StrategyCompletionFixture.check;
import static test.StrategyCompletionFixture.checks;
import static test.StrategyCompletionFixture.expectFailure;
import static test.StrategyCompletionFixture.fixture;

import exception.InvalidStrategyException;

import f1.model.UserRole;

import model.StrategyStatus;
import model.TireCompound;

import test.StrategyCompletionFixture.Context;

public final class RaceStrategyCancellationVerification {
    public static void main(String[] args) {
        Context draft = fixture(null, true, 0);
        verifyInvalidReasons(draft);
        draft.strategy().cancelStrategy("  Change of plans  ");
        check(
                draft.strategy().getStatus() == StrategyStatus.CANCELLED
                        && draft.strategy().getCancellationReason().equals("Change of plans"),
                "Draft cancellation must preserve the trimmed reason.");
        check(
                draft.car().isRaceReady()
                        && draft.car().getAssignedEngineer() == draft.engineer()
                        && draft.car().getAssignedDriver() == draft.driver(),
                "Draft cancellation must preserve assignments and preparation it did not reserve.");
        verifyTerminal(draft);
        Context ready = fixture(null, false, 0);
        ready.strategy().markReady();
        verifyInvalidReasons(ready);
        ready.strategy().cancelStrategy("Circuit unavailable");
        check(
                ready.strategy().getStatus() == StrategyStatus.CANCELLED
                        && ready.car().isRaceReady(),
                "Ready cancellation must not start or finish a session.");
        check(
                ready.team().consumeParts("TIRES", 3),
                "Ready cancellation must leave all stock usable.");
        verifyTerminal(ready);
        Context completed = fixture(null, true, 0);
        completed.strategy().markReady();
        completed.strategy().startStrategy();
        completed.strategy().markComplete();
        expectFailure(
                InvalidStrategyException.class,
                () -> completed.strategy().cancelStrategy("Too late"));
        check(
                completed.strategy().getStatus() == StrategyStatus.COMPLETED
                        && completed.strategy().getCancellationReason().isEmpty()
                        && completed.car().getTotalMileage() == 5,
                "A completed strategy cannot become cancelled.");
        System.out.println("PASS: Strategy cancellation transitions (" + checks + " checks)");
    }

    private static void verifyInvalidReasons(Context context) {
        StrategyStatus status = context.strategy().getStatus();
        double budget = context.team().getBudget(UserRole.TEAM_PRINCIPAL);
        for (String reason : new String[] {null, "", "  "}) {
            expectFailure(
                    IllegalArgumentException.class,
                    () -> context.strategy().cancelStrategy(reason));
            check(
                    context.strategy().getStatus() == status
                            && context.strategy().getCancellationReason().isEmpty()
                            && context.car().isRaceReady()
                            && !context.car().isSessionActive()
                            && context.team().getBudget(UserRole.TEAM_PRINCIPAL) == budget,
                    "Invalid reasons must not change strategy or car state.");
        }
    }

    private static void verifyTerminal(Context context) {
        String reason = context.strategy().getCancellationReason();
        expectFailure(
                InvalidStrategyException.class,
                () -> context.strategy().cancelStrategy("Replacement reason"));
        expectFailure(InvalidStrategyException.class, context.strategy()::markReady);
        expectFailure(InvalidStrategyException.class, context.strategy()::startStrategy);
        expectFailure(InvalidStrategyException.class, context.strategy()::markComplete);
        expectFailure(
                InvalidStrategyException.class,
                () -> context.strategy().selectTireCompound(TireCompound.SOFT));
        check(
                context.strategy().getCancellationReason().equals(reason),
                "Terminal rejection must retain the reason.");
    }
}
