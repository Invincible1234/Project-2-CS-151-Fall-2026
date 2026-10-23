package ui;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Scanner;

public final class ConsoleInput {
    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleInput(Readable source, PrintStream output) {
        this.scanner = new Scanner(Objects.requireNonNull(source, "Input source is required."));
        this.output = Objects.requireNonNull(output, "Output is required.");
    }

    public void print(String message) {
        output.println(message);
    }

    public String text(String prompt) {
        while (true) {
            print(prompt + " (EXIT to quit)");
            if (!scanner.hasNextLine()) {
                throw new ExitRequested();
            }
            String value = scanner.nextLine().strip();
            if (value.equalsIgnoreCase("EXIT")) {
                throw new ExitRequested();
            }
            if (!value.isEmpty()) {
                return value;
            }
            print("Enter a nonblank value.");
        }
    }

    public int integer(String prompt, int minimum, int maximum) {
        while (true) {
            String value = text(prompt + " [" + minimum + " to " + maximum + "]");
            try {
                int number = Integer.parseInt(value);
                if (number >= minimum && number <= maximum) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
            }
            print("Enter a whole number in the displayed range.");
        }
    }

    public double decimal(String prompt, double minimum, double maximum) {
        while (true) {
            String value = text(prompt + " [" + minimum + " to " + maximum + "]");
            try {
                double number = Double.parseDouble(value);
                if (Double.isFinite(number) && number >= minimum && number <= maximum) {
                    return number;
                }
            } catch (NumberFormatException ignored) {
            }
            print("Enter a finite number in the displayed range.");
        }
    }

    public boolean yesNo(String prompt) {
        while (true) {
            String value = text(prompt + " [YES/NO]");
            if (value.equalsIgnoreCase("YES")) {
                return true;
            }
            if (value.equalsIgnoreCase("NO")) {
                return false;
            }
            print("Enter YES or NO.");
        }
    }

    public <E extends Enum<E>> E choice(String prompt, Class<E> type) {
        while (true) {
            String value = text(prompt + " " + Arrays.toString(type.getEnumConstants()));
            try {
                return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                print("Choose one of the displayed values.");
            }
        }
    }

    public LocalDate pastOrPresentDate(String prompt) {
        while (true) {
            String value = text(prompt + " [YYYY-MM-DD, today or earlier]");
            try {
                LocalDate date = LocalDate.parse(value);
                if (!date.isAfter(LocalDate.now())) {
                    return date;
                }
            } catch (DateTimeParseException ignored) {
            }
            print("Enter a valid date that is not in the future.");
        }
    }

    public void action(Runnable action) {
        try {
            action.run();
        } catch (ExitRequested exit) {
            throw exit;
        } catch (RuntimeException error) {
            print("Action failed: " + error.getMessage());
        }
    }

    public void run(Runnable application) {
        try {
            application.run();
        } catch (ExitRequested exit) {
            print("Goodbye.");
        }
    }

    public static final class ExitRequested extends RuntimeException {
        private static final long serialVersionUID = 1L;

        private ExitRequested() {
            super("Exit requested.", null, false, false);
        }
    }
}
