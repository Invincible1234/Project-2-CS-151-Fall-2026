package test;

import exception.DuplicateEntityException;
import f1.model.Driver;
import f1.model.HybridRaceCar;
import f1.model.RaceResult;
import f1.model.Team;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class RacePointsIntegrationVerification {
    public static void main(String[] args) {
        Team team = new Team("POINTS", "Team", 2, 0);
        HybridRaceCar car = new HybridRaceCar("CAR", "Team", "Hybrid", 2026, 4, 3);
        Driver driver = new Driver("DRIVER", "Driver", "LICENSE", 30, "5551234567");
        Driver other = new Driver("OTHER", "Other", "OTHER-LICENSE", 30, "5551234567");
        team.addCar(car);
        team.addDriver(driver);
        team.addDriver(other);
        car.updateMileage(37.5);
        RaceResult win = result("WIN", "RACE-1", "DRIVER", 1);
        team.recordRaceResult(win);
        checkTotals(team, driver, car, 25);
        expect(IllegalStateException.class, () -> team.recordRaceResult(win));
        expect(IllegalStateException.class, win::applyPointsToDriverAndTeam);
        expect(DuplicateEntityException.class,
                () -> team.recordRaceResult(result("COPY", "RACE-1", "DRIVER", 2)));
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
                () -> team.recordRaceResult(result("REPLACEMENT", "RACE-1", "DRIVER", 1)));
        team.recordRaceResult(result("PODIUM", "RACE-2", "DRIVER", 3));
        checkTotals(team, driver, car, 15);
        RaceResult next = result("NEXT", "RACE-3", "DRIVER", 10);
        driver.completeRace(next, team);
        checkTotals(team, driver, car, 16);
        expect(IllegalStateException.class, () -> driver.completeRace(next, team));
        expect(IllegalArgumentException.class, () -> other.completeRace(next, team));
        Team wrong = new Team("WRONG", "Wrong", 1, 0);
        RaceResult unregistered = result("UNREGISTERED", "RACE-4", "DRIVER", 1);
        expect(IllegalArgumentException.class, () -> wrong.recordRaceResult(unregistered));
        check(!unregistered.arePointsApplied() && wrong.getRaceResults().isEmpty(),
                "Failed registration partially applied points.");
        checkTotals(team, driver, car, 16);
        System.out.println("Race points integration verification passed.");
    }

    private static RaceResult result(String id, String race, String driver, int position) {
        RaceResult result = new RaceResult(id, race, "CAR", driver, "CIRCUIT", position, "2026-10-22",
                Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC));
        result.recordResult();
        return result;
    }

    private static void checkTotals(Team team, Driver driver, HybridRaceCar car, int points) {
        check(team.getChampionshipPoints() == points && driver.getChampionshipPoints() == points,
                "Team and driver totals disagree.");
        check(car.getTotalMileage() == 37.5, "Points accounting counted mileage again.");
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
