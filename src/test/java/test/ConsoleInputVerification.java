package test;

import static test.ConsoleSessionFixture.check;
import static test.ConsoleSessionFixture.run;

import f1.model.UserRole;
import f1.ui.ConsoleInput;
import f1.ui.ConsoleState;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

public final class ConsoleInputVerification {
    public static void main(String[] args) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            String script =
                    "\nabc\n0\n2147483648\n2\nNaN\nInfinity\n-1\n0\n1.5\n"
                            + "-5\n0\nunknown\nengineer\nmissing\nH1\n";
            ConsoleInput input = new ConsoleInput(new StringReader(script), output);
            check(input.integer("Quantity", 1, 10) == 2, "Whole-number validation must re-prompt.");
            check(
                    input.amount("Positive amount", true) == 1.5,
                    "Amounts must reject zero, negatives, and nonfinite values.");
            check(input.amount("Budget", false) == 0, "A starting budget may be zero.");
            check(
                    input.choice("Role", UserRole.class) == UserRole.ENGINEER,
                    "Enums must re-prompt and accept lowercase.");
            check(
                    input.select("Car", List.of("H1"), value -> value).equals("H1"),
                    "IDs must match an available entry.");
            boolean rejected = false;
            try {
                input.select("Missing car", List.<String>of(), value -> value);
            } catch (IllegalStateException error) {
                rejected = true;
            }
            check(rejected, "Empty selection must report an unavailable action.");
        }
        String output = bytes.toString(StandardCharsets.UTF_8);
        check(
                output.contains("Invalid input") && output.contains("nonblank"),
                "Validation must explain invalid input.");
        for (String script :
                new String[] {
                    "",
                    "EXIT\n",
                    "1\nEXIT\n",
                    "1\n1\nEXIT\n",
                    "1\n1\nT\nName\nEXIT\n",
                    "1\n1\nT\nName\n2\nexit\n",
                    "5\nEXIT\n"
                }) {
            ConsoleState state = new ConsoleState();
            String transcript = run(script, state);
            check(
                    transcript.endsWith("Goodbye." + System.lineSeparator()),
                    "EXIT and EOF must terminate gracefully.");
            check(
                    state.getTeams().isEmpty(),
                    "Exiting an unfinished creation must not register a partial team.");
            check(
                    !transcript.contains("Exception") && !transcript.contains("at f1."),
                    "Normal exit must not print a stack trace.");
        }
        System.out.println("PASS: Console input validation and graceful exit");
    }
}
