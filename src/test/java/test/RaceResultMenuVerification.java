package test;

import f1.model.RaceResultStatus;
import f1.model.Team;
import f1.model.UserRole;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.time.LocalDate;
import ui.ConsoleInput;
import ui.RaceResultMenu;
import static test.StrategyCompletionFixture.*;

public final class RaceResultMenuVerification {
    public static void main(String[] args) {
        verifyResultWorkflow();
        verifyMissingCompletionAndExit();
        System.out.println("Race result menu verification passed (" + checks + " checks).");
    }

    private static void verifyResultWorkflow() {
        Context context = fixture(null, true, 12);
        context.strategy().markReady();
        context.strategy().startStrategy();
        context.strategy().markComplete();
        String plan = context.strategy().getStrategyId();
        String chassis = context.car().getChassisId();
        String today = LocalDate.now().toString();
        String future = LocalDate.now().plusDays(1).toString();
        String script = String.join("\n",
                "bad", "1", "UNKNOWN", chassis,
                "1", plan, chassis, "FINISH-UI", "RACE-UI", "0", "1",
                "2026-02-30", future, today,
                "1", plan, chassis, "FINISH-UI",
                "3", "UNKNOWN",
                "3", "FINISH-UI", "2", "", "Timing correction",
                "3", "FINISH-UI", "2", "Same position",
                "2", "5", "6", "0",
                "4", "FINISH-UI", "Not needed", "NO", "0",
                "4", "FINISH-UI", "Disqualified", "YES",
                "4", "FINISH-UI", "Repeat", "YES",
                "3", "FINISH-UI", "1", "Appeal",
                "1", plan, chassis, "COPY-UI", "ANOTHER-RACE", "1", today,
                "2", "5", "6", "0", "");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleInput input = new ConsoleInput(new StringReader(script), new PrintStream(output));
        input.run(() -> {
            new RaceResultMenu(input, context.team()).run();
            check(context.team().getChampionshipPoints() == 18
                    && context.driver().getChampionshipPoints() == 18
                    && context.team().getRaceResults().size() == 1,
                    "Recording, corrections, or rejected duplicates changed totals incorrectly.");
            new RaceResultMenu(input, context.team()).run();
            check(context.team().getChampionshipPoints() == 18,
                    "Cancelling void must preserve championship points.");
            new RaceResultMenu(input, context.team()).run();
        });
        var result = context.team().getRaceResults().get(0);
        check(result.getStatus() == RaceResultStatus.VOIDED && result.getDate().equals(today)
                && result.getCorrectionReason().equals("Timing correction")
                && result.getVoidReason().equals("Disqualified")
                && context.team().getRaceResults().size() == 1,
                "Result history or validated date was lost.");
        check(context.team().getChampionshipPoints() == 0 && context.driver().getChampionshipPoints() == 0
                && context.car().getTotalMileage() == 17
                && context.team().getPartsInventory(UserRole.TEAM_PRINCIPAL).get("TIRES") == 2,
                "Result menus must reverse points without repeating completed race effects.");
        String transcript = output.toString();
        check(transcript.contains("18 points") && transcript.contains("0 points")
                && transcript.contains("Recorded results: 0") && transcript.contains("Disqualified")
                && transcript.contains("already registered") && transcript.contains("No result has")
                && transcript.contains("No completed strategy") && transcript.contains("not in the future")
                && !transcript.contains("Exception") && !transcript.contains("Goodbye."),
                "Standings, validation messages, or correction history were not displayed.");
    }

    private static void verifyMissingCompletionAndExit() {
        Team empty = new Team("EMPTY-RESULT-UI", "Empty", 1, 0);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleInput input = new ConsoleInput(new StringReader("1\n2\n5\n6\nEXIT\n"), new PrintStream(output));
        input.run(() -> new RaceResultMenu(input, empty).run());
        check(empty.getRaceResults().isEmpty() && output.toString().contains("Complete a race strategy")
                && output.toString().contains("No registered drivers.") && output.toString().contains("Goodbye."),
                "An empty results menu must recover and exit cleanly.");
        Context context = fixture(null, false, 0);
        context.strategy().markReady();
        context.strategy().startStrategy();
        context.strategy().markComplete();
        for (String ending : new String[] {"EXIT\n", ""}) {
            String script = String.join("\n", "1", context.strategy().getStrategyId(),
                    context.car().getChassisId(), "ABORT", "ABORT-RACE", "1") + "\n" + ending;
            ConsoleInput interrupted = new ConsoleInput(new StringReader(script), new PrintStream(output));
            interrupted.run(() -> new RaceResultMenu(interrupted, context.team()).run());
            check(context.team().getRaceResults().isEmpty() && context.team().getChampionshipPoints() == 0
                    && context.car().getTotalMileage() == 5,
                    "EXIT or EOF at the date prompt must not partially record a result.");
        }
    }
}
