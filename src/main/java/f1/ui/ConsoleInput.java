package f1.ui;

import java.util.Scanner;

public final class ConsoleInput {
    private final Scanner scanner;

    public ConsoleInput(Scanner scanner) {
        if (scanner == null) {
            throw new IllegalArgumentException("Scanner is required.");
        }
        this.scanner = scanner;
    }

    public String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                return "EXIT";
            }
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("EXIT")) {
                return "EXIT";
            }
            if (!input.isBlank()) {
                return input;
            }
            System.out.println("Input cannot be blank. Try again or type EXIT to quit.");
        }
    }

    public int readInt(String prompt) {
        while (true) {
            String input = readText(prompt);
            if ("EXIT".equals(input)) {
                throw new ExitRequestedException();
            }
            try {
                return Integer.parseInt(input);
            } catch (NumberFormatException e) {
                System.out.println("Invalid integer. Try again or type EXIT to quit.");
            }
        }
    }

    public <E extends Enum<E>> E readEnum(String prompt, Class<E> enumClass) {
        while (true) {
            String input = readText(prompt);
            if ("EXIT".equals(input)) {
                throw new ExitRequestedException();
            }
            try {
                return Enum.valueOf(enumClass, input.toUpperCase());
            } catch (IllegalArgumentException e) {
                StringBuilder options = new StringBuilder();
                for (E constant : enumClass.getEnumConstants()) {
                    if (!options.isEmpty()) {
                        options.append(", ");
                    }
                    options.append(constant.name());
                }
                System.out.println("Invalid choice. Valid options: " + options + ". Try again or type EXIT to quit.");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String input = readText(prompt + " (yes/no): ");
            if ("EXIT".equals(input)) {
                throw new ExitRequestedException();
            }
            if (input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("y")) {
                return true;
            }
            if (input.equalsIgnoreCase("no") || input.equalsIgnoreCase("n")) {
                return false;
            }
            System.out.println("Please enter yes or no. Try again or type EXIT to quit.");
        }
    }

    public static class ExitRequestedException extends RuntimeException {
    }
}