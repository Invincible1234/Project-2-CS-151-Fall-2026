package test;

import f1.model.HybridRaceCar;
import f1.model.RaceResult;
import f1.model.RaceStrategy;
import f1.model.SessionType;
import f1.model.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import model.StrategyStatus;
import static test.StrategyCompletionFixture.*;

public final class CompletedRaceIntegrationVerification {
    public static void main(String[] args) {
        Context context = fixture(null, true, 10);
        HybridRaceCar car = (HybridRaceCar) context.car();
        var strategy = context.strategy();
        var team = context.team();
        strategy.markReady();
        car.scheduleStrategy(strategy);
        expectFailure(IllegalStateException.class, () -> car.applyAeroUpgrade("LOW_DRAG"));
        expectFailure(IllegalStateException.class, () -> car.applyHybridUpgrade("EFFICIENCY"));
        expectFailure(IllegalStateException.class, () -> car.updateMileage(5));
        expectFailure(IllegalStateException.class, car::completeSession);
        expectFailure(IllegalStateException.class, car::completeLap);
        expectFailure(IllegalStateException.class, car::startNextLap);
        expectFailure(IllegalStateException.class, () -> car.deployEnergyBoost(3));
        expectFailure(IllegalStateException.class, () -> car.chargeBatteryFromRecovery(2));
        car.deployEnergyBoost(1);
        car.chargeBatteryFromRecovery(0.5);
        strategy.markComplete();
        check(strategy.getStatus() == StrategyStatus.COMPLETED && !car.isSessionActive()
                && car.getActiveStrategy() == null && !car.isRaceReady(), "Completion must release the session.");
        check(car.getTotalMileage() == 15 && car.getMileageSinceService() == 15,
                "Completion must add exactly one circuit lap.");
        check(car.getCurrentEnergyMJ() == 2 && car.getDeployedThisLapMJ() == 2
                && car.getRecoveredThisLapMJ() == 1, "Completion must apply only remaining planned energy.");
        check(car.getAssignedDriver() == null && car.getAssignedEngineer() == null
                && context.engineer().isAvailable() && context.driver().getAssignedChassisIds().isEmpty()
                && strategy.getRaceDriver() == context.driver(), "Completed entry must retain the released driver.");
        check(team.getPartsInventory(UserRole.TEAM_PRINCIPAL).get("TIRES") == 2,
                "Completion must consume the reserved tire once.");
        expectFailure(RuntimeException.class, strategy::markComplete);
        RaceResult result = result("FINISH", "RACE", context, context.driver().getDriverId());
        team.recordRaceResult(result, strategy);
        result.correctFinishingPosition(2, "Timing correction");
        check(team.getChampionshipPoints() == 18 && context.driver().getChampionshipPoints() == 18,
                "Corrections must update the original driver's points after release.");
        result.voidResult("Disqualified");
        expectFailure(RuntimeException.class, () -> team.recordRaceResult(
                result("COPY", "DIFFERENT-RACE", context, context.driver().getDriverId()), strategy));
        expectFailure(RuntimeException.class, () -> team.recordRaceResult(
                result("WRONG", "WRONG-RACE", context, "OTHER"), strategy));
        check(team.getChampionshipPoints() == 0 && context.driver().getChampionshipPoints() == 0
                && car.getTotalMileage() == 15 && car.getCurrentEnergyMJ() == 2
                && team.getPartsInventory(UserRole.TEAM_PRINCIPAL).get("TIRES") == 2,
                "Corrections, voiding, and retries must preserve completed mileage, energy, and parts.");
        verifyAmbiguousCompletion();
        System.out.println("Completed race integration verification passed (" + checks + " checks).");
    }

    private static void verifyAmbiguousCompletion() {
        Context context = fixture(null, false, 0);
        var first = context.strategy();
        first.markReady();
        first.startStrategy();
        first.markComplete();
        context.team().assignDriver(context.driver(), context.car());
        context.engineer().assignToCar(context.car());
        context.car().prepareForSession(SessionType.RACE);
        var second = new RaceStrategy("SECOND-PLAN", context.car(), context.engineer(), first.getCircuit());
        context.team().addStrategy(second);
        second.adjustForWeather(first.getExpectedWeather());
        second.addRequiredParts("TIRES", 1);
        second.finalizeLapTimeImpact(first.getCircuit());
        second.markReady();
        second.startStrategy();
        second.markComplete();
        RaceResult result = result("FIRST-RESULT", "FIRST-RACE", context, context.driver().getDriverId());
        expectFailure(IllegalStateException.class, () -> context.team().recordRaceResult(result));
        check(!result.arePointsApplied() && context.team().getRaceResults().isEmpty(),
                "Ambiguous completion must not apply points or register a result.");
        context.team().recordRaceResult(result, first);
        context.team().recordRaceResult(result("SECOND-RESULT", "SECOND-RACE", context,
                context.driver().getDriverId()));
        check(context.team().getChampionshipPoints() == 50 && context.car().getTotalMileage() == 10,
                "Explicit completion selection must preserve separate race entries and mileage.");
    }

    private static RaceResult result(String id, String race, Context context, String driverId) {
        RaceResult result = new RaceResult(id, race, context.car().getChassisId(), driverId,
                context.strategy().getCircuit().getCircuitId(), 1, "2026-10-22",
                Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC));
        result.recordResult();
        return result;
    }
}
