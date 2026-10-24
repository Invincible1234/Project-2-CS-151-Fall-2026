package test;

import static test.ConsoleSessionFixture.check;
import static test.ConsoleSessionFixture.run;

import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.HybridRaceCar;
import f1.model.Team;
import f1.ui.ConsoleState;

import model.WeatherCondition;

public final class AdvancedCarConsoleVerification {
    public static void main(String[] args) {
        ConsoleState state = new ConsoleState();
        Team source = new Team("SOURCE", "Apex", 2, 0);
        Team destination = new Team("DEST", "Heritage", 2, 0);
        HybridRaceCar hybrid =
                new HybridRaceCar(
                        "H", "Apex", "Hybrid", 2026, 4, 3, 350, ConsoleSessionFixture.CLOCK);
        ClassicRaceCar classic =
                new ClassicRaceCar(
                        "C", "Apex", "Classic", 1980, 90, 2020, 250, ConsoleSessionFixture.CLOCK);
        source.addCar(hybrid);
        source.addCar(classic);
        source.addEngineer(
                new Engineer(
                        "E", "Engineer", EngineerSpecialty.HYBRID_POWER_UNIT, 5, "5551234567"));
        state.addTeam(source);
        state.addTeam(destination);
        state.selectTeam(source);
        state.addCircuit(
                new Circuit("TRACK", "Circuit", 5, 12, true, WeatherCondition.CLEAR, "Venue"));
        String script =
                advanced("H", 5, "6", "E")
                        + "6\n4\nH\nPRACTICE\n"
                        + advanced("H", 2)
                        + advanced("H", 5, "1", "1")
                        + advanced("H", 5, "2", "0.5")
                        + advanced("H", 5, "3")
                        + advanced("H", 5, "4")
                        + advanced("H", 5, "1", "1")
                        + advanced("H", 3)
                        + advanced("H", 5, "5")
                        + advanced("H", 5, "8")
                        + advanced("H", 5, "7")
                        + advanced("H", 1, "Battery inspection")
                        + "6\n9\nH\nRepaired battery\n"
                        + advanced("H", 6, "TRACK", "CLEAR")
                        + advanced("C", 4, "1", "5")
                        + advanced("C", 4, "2", ConsoleSessionFixture.TODAY.getYear() + "")
                        + advanced("C", 4, "3", "5", "Restored original bodywork")
                        + advanced("C", 4, "4", "NO")
                        + advanced("C", 4, "4", "YES")
                        + advanced("C", 4, "6")
                        + advanced("C", 4, "5", "UNKNOWN")
                        + advanced("C", 4, "5", "DEST")
                        + "1\n2\nDEST\n"
                        + "6\n4\nC\nRACE\n"
                        + "6\n4\nC\nPRACTICE\n"
                        + advanced("C", 2)
                        + advanced("C", 3)
                        + advanced("C", 5, "0")
                        + "EXIT\n";
        String transcript = run(script, state);
        check(
                source.getCars().size() == 1
                        && destination.getCars().size() == 1
                        && destination.getCars().get(0) == classic
                        && classic.getRegisteredTeam() == destination,
                "Ownership transfer must move the canonical car between the application's teams.");
        check(
                classic.getOriginalityScore() == 100
                        && classic.isPreservationModeEnabled()
                        && !classic.isRestorationInProgress()
                        && classic.getPowerLimitKw() == 125
                        && classic.getOwnershipRecord().contains("Heritage")
                        && !classic.isSessionActive(),
                "Classic console operations must update restoration, preservation, history, and"
                        + " session state.");
        check(
                hybrid.getCurrentEnergyMJ() == 1.5
                        && hybrid.getDeployedThisLapMJ() == 0
                        && hybrid.getRecoveredThisLapMJ() == 0
                        && !hybrid.isSessionActive()
                        && hybrid.getAssignedEngineer() == null
                        && source.getEngineers().get(0).isAvailable()
                        && hybrid.getOutOfServiceReason().isEmpty(),
                "Hybrid console actions must preserve charge, reset only counters, and release the"
                        + " specialist.");
        check(
                transcript.contains("Estimated lap impact")
                        && transcript.contains("Preservation cancelled")
                        && transcript.contains("No team has that ID")
                        && transcript.contains("could not be prepared")
                        && transcript.contains("Hybrid energy operations require a hybrid car")
                        && transcript.contains("Goodbye.")
                        && !transcript.contains("Exception"),
                "Advanced paths must expose predictions and recover from rejected actions.");
        String exitTranscript = run(advanced("C", 4, "3", "1", "EXIT"), state);
        check(
                exitTranscript.contains("Goodbye.") && !exitTranscript.contains("Action failed"),
                "EXIT from a nested restoration prompt must exit the application cleanly.");
        String release =
                run(
                        advanced("C", 4, "7", "Museum", "NO")
                                + advanced("C", 4, "7", "Museum", "YES")
                                + "EXIT\n",
                        state);
        check(
                release.contains("Ownership release cancelled")
                        && release.contains("Car released")
                        && destination.getCars().isEmpty()
                        && classic.getRegisteredTeam() == null
                        && classic.getOwnershipRecord().contains("Museum"),
                "Confirmed outside-owner transfers must release membership and preserve history.");
        verifyManagedGuards();
        System.out.println("Advanced car console verification passed.");
    }

    private static void verifyManagedGuards() {
        var context = StrategyCompletionFixture.fixture(null, true, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        ConsoleState state = new ConsoleState();
        state.addTeam(context.team());
        String chassis = context.car().getChassisId();
        String rejected =
                run(
                        advanced(chassis, 3)
                                + advanced(chassis, 5, "3")
                                + advanced(chassis, 5, "4")
                                + advanced(chassis, 5, "7")
                                + "6\n5\n"
                                + chassis
                                + "\n5\nEXIT\n",
                        state);
        check(
                rejected.contains("Action failed")
                        && context.car().isSessionActive()
                        && context.car().getTotalMileage() == 0
                        && context.car().getAssignedEngineer() == context.engineer(),
                "Advanced menu actions must preserve the active strategy's mileage and personnel"
                        + " guards.");
        run(
                ConsoleSessionFixture.action(9, chassis + "/" + context.strategy().getStrategyId())
                        + "EXIT\n",
                state);
        check(
                !context.car().isSessionActive()
                        && context.car().getTotalMileage() == 5
                        && context.car().getAssignedEngineer() == null,
                "A rejected manual operation must leave normal strategy completion usable.");
    }

    private static String advanced(String chassis, int action, String... values) {
        return "6\n11\n"
                + chassis
                + "\n"
                + action
                + "\n"
                + (values.length == 0 ? "" : String.join("\n", values) + "\n");
    }
}
