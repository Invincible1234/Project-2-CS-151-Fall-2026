package test;

import exception.DuplicateEntityException;
import f1.model.Driver;
import f1.model.F1Car;
import f1.model.RaceStrategy;
import f1.model.SessionType;
import f1.model.RaceResult;
import f1.model.Team;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class RacePointsIntegrationVerification {
    private static String chassis;
    private static String circuit;
    private static String driverId;
    private static double expectedMileage;

    public static void main(String[] args) {
        var context = StrategyCompletionFixture.fixture(null, false, 37.5);
        Team team = context.team();
        F1Car car = context.car();
        Driver driver = context.driver();
        Driver other = new Driver("OTHER", "Other", "OTHER-LICENSE", 30, "5551234567");
        team.addDriver(other);
        chassis = car.getChassisId();
        driverId = driver.getDriverId();
        circuit = context.strategy().getCircuit().getCircuitId();
        RaceResult premature = result("PREMATURE", "RACE-1", driverId, 1);
        expect(IllegalStateException.class, () -> team.recordRaceResult(premature));
        context.strategy().markReady();
        car.scheduleStrategy(context.strategy());
        expect(IllegalStateException.class, () -> car.updateMileage(5));
        expect(IllegalStateException.class, () -> team.recordRaceResult(premature));
        context.strategy().markComplete();
        expectedMileage = 42.5;
        expect(RuntimeException.class, context.strategy()::markComplete);
        RaceResult win = result("WIN", "RACE-1", driverId, 1);
        team.recordRaceResult(win);
        checkTotals(team, driver, car, 25);
        expect(IllegalStateException.class, () -> team.recordRaceResult(win));
        expect(IllegalStateException.class, win::applyPointsToDriverAndTeam);
        expect(DuplicateEntityException.class,
                () -> team.recordRaceResult(result("COPY", "RACE-1", driverId, 2)));
        expect(DuplicateEntityException.class,
                () -> team.recordRaceResult(result("OTHER-COPY", "RACE-1", "OTHER", 2)));
        check(other.getChampionshipPoints() == 0 && team.getRaceResults().size() == 1,
                "Duplicate entry changed points or membership.");
        win.correctFinishingPosition(2, "Timing correction");
        checkTotals(team, driver, car, 18);
        check(team.generateTeamPerformanceReport().contains("Best finish: 2"), "Report ignored correction.");
        expect(IllegalArgumentException.class, () -> win.correctFinishingPosition(0, "Invalid"));
        expect(IllegalArgumentException.class, () -> win.voidResult(" "));
        checkTotals(team, driver, car, 18);
        win.correctFinishingPosition(11, "Penalty");
        checkTotals(team, driver, car, 0);
        win.correctFinishingPosition(1, "Appeal");
        checkTotals(team, driver, car, 25);
        win.voidResult("Disqualified");
        checkTotals(team, driver, car, 0);
        check(team.getRaceResults().size() == 1 && team.generateTeamPerformanceReport().contains("Wins: 0")
                && team.generateTeamPerformanceReport().contains("Recorded results: 0")
                && team.generateTeamPerformanceReport().contains("Best finish: N/A"),
                "Voided results corrupted history or performance reporting.");
        expect(IllegalStateException.class, () -> win.voidResult("Repeat"));
        expect(DuplicateEntityException.class,
                () -> team.recordRaceResult(result("REPLACEMENT", "RACE-1", driverId, 1)));
        completeNext(context);
        team.recordRaceResult(result("PODIUM", "RACE-2", driverId, 3));
        checkTotals(team, driver, car, 15);
        completeNext(context);
        RaceResult next = result("NEXT", "RACE-3", driverId, 10);
        driver.completeRace(next, team);
        checkTotals(team, driver, car, 16);
        expect(IllegalStateException.class, () -> driver.completeRace(next, team));
        expect(IllegalArgumentException.class, () -> other.completeRace(next, team));
        Team wrong = new Team("WRONG", "Wrong", 1, 0);
        RaceResult unregistered = result("UNREGISTERED", "RACE-4", driverId, 1);
        expect(IllegalArgumentException.class, () -> wrong.recordRaceResult(unregistered));
        check(!unregistered.arePointsApplied() && wrong.getRaceResults().isEmpty(),
                "Failed registration partially applied points.");
        checkTotals(team, driver, car, 16);
        System.out.println("Race points integration verification passed.");
    }

    private static void completeNext(StrategyCompletionFixture.Context context) {
        context.team().assignDriver(context.driver(), context.car());
        context.engineer().assignToCar(context.car());
        context.car().prepareForSession(SessionType.RACE);
        RaceStrategy strategy = StrategyCompletionFixture.strategy(context.team(), context.car(), context.engineer());
        circuit = strategy.getCircuit().getCircuitId();
        strategy.markReady();
        context.car().scheduleStrategy(strategy);
        strategy.markComplete();
        expectedMileage += 5;
    }

    private static RaceResult result(String id, String race, String driver, int position) {
        RaceResult result = new RaceResult(id, race, chassis, driver, circuit, position, "2026-10-22",
                Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC));
        result.recordResult();
        return result;
    }

    private static void checkTotals(Team team, Driver driver, F1Car car, int points) {
        check(team.getChampionshipPoints() == points && driver.getChampionshipPoints() == points,
                "Team and driver totals disagree.");
        check(car.getTotalMileage() == expectedMileage, "Points accounting counted mileage again.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("Unexpected exception type.", error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
