package ui;

import java.io.PrintStream;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public final class ConsoleInput {
    private final f1.ui.ConsoleInput shared;

    public ConsoleInput(Readable source, PrintStream output) {
        this(new f1.ui.ConsoleInput(source, output));
    }

    public ConsoleInput(f1.ui.ConsoleInput shared) {
        this.shared = Objects.requireNonNull(shared, "Shared input is required.");
    }

    public void print(String message) {
        shared.show(message);
    }

    public String text(String prompt) {
        try {
            return shared.text(prompt);
        } catch (f1.ui.ConsoleInput.ExitRequestedException exit) {
            throw new ExitRequested();
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
