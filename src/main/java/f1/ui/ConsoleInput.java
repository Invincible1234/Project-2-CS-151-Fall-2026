package f1.ui;

import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Scanner;
import java.util.function.Function;

public final class ConsoleInput {
    private final Scanner scanner;
    private final PrintStream output;

    public ConsoleInput(Readable source, PrintStream output) {
        scanner = new Scanner(java.util.Objects.requireNonNull(source));
        this.output = java.util.Objects.requireNonNull(output);
    }

    public ConsoleInput(Scanner scanner) {
        this.scanner = java.util.Objects.requireNonNull(scanner, "Scanner is required.");
        this.output = System.out;
    }

    public String readText(String prompt) {
        try {
            return text(prompt);
        } catch (Exit exit) {
            return "EXIT";
        }
    }

    public int readInt(String prompt) {
        return integer(prompt, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    public <E extends Enum<E>> E readEnum(String prompt, Class<E> type) {
        return choice(prompt, type);
    }

    public boolean readYesNo(String prompt) {
        return read(prompt + " (yes/no)", value -> switch (value.toLowerCase(Locale.ROOT)) {
            case "yes", "y" -> true;
            case "no", "n" -> false;
            default -> throw new IllegalArgumentException("Enter yes or no.");
        });
    }

    public void show(Object message) {
        output.println(message);
    }

    public String text(String prompt) {
        while (true) {
            output.print(prompt + " (EXIT to quit): ");
            if (!scanner.hasNextLine()) {
                throw new Exit();
            }
            String value = scanner.nextLine().strip();
            if (value.equalsIgnoreCase("EXIT")) {
                throw new Exit();
            }
            if (!value.isEmpty()) {
                return value;
            }
            show("Enter a nonblank value.");
        }
    }

    public <T> T read(String prompt, Function<String, T> parser) {
        while (true) {
            String value = text(prompt);
            try {
                return parser.apply(value);
            } catch (IllegalArgumentException error) {
                show("Invalid input: " + (error instanceof NumberFormatException
                        ? "Enter a number in the requested range." : error.getMessage()));
            }
        }
    }

    public int integer(String prompt, int min, int max) {
        return read(prompt, value -> {
            int number = Integer.parseInt(value);
            if (number < min || number > max) {
                throw new IllegalArgumentException("Enter a whole number from " + min + " to " + max + ".");
            }
            return number;
        });
    }

    public double amount(String prompt, boolean positive) {
        return read(prompt, value -> {
            double number = Double.parseDouble(value);
            if (!Double.isFinite(number) || number < 0 || positive && number == 0) {
                throw new IllegalArgumentException("Enter a finite " + (positive ? "positive" : "nonnegative") + " number.");
            }
            return number;
        });
    }

    public <E extends Enum<E>> E choice(String prompt, Class<E> type) {
        return read(prompt + " " + Arrays.toString(type.getEnumConstants()), value -> {
            try {
                return Enum.valueOf(type, value.toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException error) {
                throw new IllegalArgumentException("Choose one of the listed values.");
            }
        });
    }

    public <T> T select(String prompt, List<T> values, Function<T, String> identifier) {
        if (values.isEmpty()) {
            throw new IllegalStateException("No entries available for " + prompt + ".");
        }
        show(prompt + " options: " + values.stream().map(identifier).toList());
        return read(prompt + " ID", value -> values.stream()
                .filter(item -> identifier.apply(item).equals(value)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Choose one of the listed IDs.")));
    }

    public static class ExitRequestedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }

    static final class Exit extends ExitRequestedException {
        private static final long serialVersionUID = 1L;
    }
}
