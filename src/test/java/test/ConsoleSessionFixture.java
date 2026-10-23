package test;

import f1.ui.ConsoleApplication;
import f1.ui.ConsoleInput;
import f1.ui.ConsoleState;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

final class ConsoleSessionFixture {
    static String run(String script, ConsoleState state) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            new ConsoleApplication(new ConsoleInput(new StringReader(script), output), state).run();
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    static String action(int number, String strategy, String... values) {
        return "2\n" + number + "\n" + strategy + "\n"
                + (values.length == 0 ? "" : String.join("\n", values) + "\n");
    }

    static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
