package test;

import static test.StrategyCompletionFixture.check;
import static test.StrategyCompletionFixture.checks;
import static test.StrategyCompletionFixture.expectFailure;
import static test.StrategyCompletionFixture.fixture;
import static test.StrategyCompletionFixture.strategy;

import exception.InvalidStrategyException;

import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.SessionType;
import f1.model.UserRole;

import model.StrategyStatus;

import test.StrategyCompletionFixture.Context;

import java.util.Map;

public final class RaceStrategyCompletionVerification {
    public static void main(String[] args) {
        verifyHybridCompletion();
        System.out.println("PASS: RaceStrategyCompletionVerification (" + checks + " checks)");
    }

    private static void verifyHybridCompletion() {
        Context context = fixture(null, true, 499);
        expectFailure(InvalidStrategyException.class, context.strategy()::markComplete);
        context.strategy().markReady();
        expectFailure(InvalidStrategyException.class, context.strategy()::markComplete);
        context.strategy().startStrategy();
        double budget = context.team().getBudget(UserRole.TEAM_PRINCIPAL);
        check(context.strategy().markComplete(), "An active strategy must complete.");
        check(
                context.strategy().getStatus() == StrategyStatus.COMPLETED,
                "Completion must be terminal.");
        check(
                context.car().getTotalMileage() == 504
                        && context.car().getMileageSinceService() == 504
                        && context.car().isDueForMaintenance(),
                "Completion must add one circuit lap and trigger maintenance.");
        check(
                !context.car().isSessionActive()
                        && !context.car().isRaceReady()
                        && context.car().getPreparedSessionType() == null
                        && !context.car().hasScheduledStrategy()
                        && context.car().getActiveStrategy() == null,
                "Completion must clear the car commitment and preparation.");
        check(
                context.engineer().isAvailable()
                        && context.engineer().getAssignedChassisId().isEmpty()
                        && context.car().getAssignedEngineer() == null
                        && context.car().getAssignedDriver() == null
                        && !context.driver()
                                .getAssignedChassisIds()
                                .contains(context.car().getChassisId()),
                "Completion must release both personnel assignments.");
        HybridRaceCar hybrid = (HybridRaceCar) context.car();
        check(
                hybrid.getCurrentEnergyMJ() == 2
                        && hybrid.getDeployedThisLapMJ() == 2
                        && hybrid.getRecoveredThisLapMJ() == 1
                        && hybrid.getCurrentLapNumber() == 0
                        && !hybrid.isLapCompleted(),
                "Completion must apply the plan once and close the hybrid session.");
        check(
                context.team().getPartsInventory(UserRole.ENGINEER).equals(Map.of("TIRES", 2))
                        && context.team().getBudget(UserRole.TEAM_PRINCIPAL) == budget,
                "Completion must consume reserved stock without charging purchased parts again.");
        check(
                context.team().getRaceResults().isEmpty()
                        && context.team().getChampionshipPoints() == 0,
                "Strategy completion must leave result recording to the result workflow.");
        expectFailure(InvalidStrategyException.class, context.strategy()::markComplete);
        expectFailure(InvalidStrategyException.class, context.strategy()::startStrategy);
        check(
                hybrid.getCurrentEnergyMJ() == 2
                        && context.car().getTotalMileage() == 504
                        && context.team().getPartsInventory(UserRole.ENGINEER).get("TIRES") == 2,
                "Repeated completion must not apply effects again.");
        context.car().performService("Post-race inspection");
        context.team().assignDriver(context.driver(), context.car());
        context.engineer().assignToCar(context.car());
        context.car().prepareForSession(SessionType.RACE);
        RaceStrategy next = strategy(context.team(), context.car(), context.engineer());
        next.markReady();
        next.startStrategy();
        next.markComplete();
        check(
                context.car().getTotalMileage() == 509 && hybrid.getCurrentEnergyMJ() == 1,
                "Released resources must support another complete strategy.");
    }
}
