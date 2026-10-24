package test;

import f1.ui.PersonnelMenu;

import util.InstanceLimit;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;

public class PersonnelConsoleVerification {
    private static final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private static final PrintStream originalOut = System.out;
    private static int failures = 0;

    public static void main(String[] args) {
        runTest("badIntegerRejected", PersonnelConsoleVerification::testBadIntegerRejected);
        runTest("badMenuChoiceRejected", PersonnelConsoleVerification::testBadMenuChoiceRejected);
        runTest(
                "duplicateRegistrationRejected",
                PersonnelConsoleVerification::testDuplicateRegistrationRejected);
        runTest("exitEndsCleanly", PersonnelConsoleVerification::testExitEndsCleanly);
        runTest("singleScannerInSource", PersonnelConsoleVerification::testSingleScannerInSource);

        if (failures == 0) {
            System.out.println("All console verification tests passed.");
        } else {
            System.out.println(failures + " test(s) failed.");
            System.exit(1);
        }
    }

    private static void runTest(String name, Runnable test) {
        InstanceLimit.resetForTesting();
        output.reset();
        System.setOut(new PrintStream(output));
        try {
            test.run();
            System.setOut(originalOut);
            System.out.println(name + " passed.");
        } catch (AssertionError e) {
            System.setOut(originalOut);
            System.out.println(name + " FAILED: " + e.getMessage());
            failures++;
        } catch (Exception e) {
            System.setOut(originalOut);
            System.out.println(name + " ERROR: " + e.getMessage());
            e.printStackTrace(originalOut);
            failures++;
        }
    }

    private static void testBadIntegerRejected() {
        String input = "1\nD1\nDriver One\nLIC123\nnotanumber\n25\n+1234567890\nEXIT\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        runMenuWithInput(in);

        String out = output.toString();
        if (!out.contains("Invalid integer")) {
            throw new AssertionError(
                    "Expected 'Invalid integer' message not found. Output: " + out);
        }
        if (!out.contains("Age: ")) {
            throw new AssertionError(
                    "Menu should re-prompt for age after invalid input. Output: " + out);
        }
    }

    private static void testBadMenuChoiceRejected() {
        String input = "99\nEXIT\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        runMenuWithInput(in);

        String out = output.toString();
        if (!out.contains("Unrecognized choice: 99")) {
            throw new AssertionError(
                    "Expected 'Unrecognized choice' message not found. Output: " + out);
        }
    }

    private static void testDuplicateRegistrationRejected() {
        String input =
                "1\n"
                        + "D1\n"
                        + "Driver One\n"
                        + "LIC123\n"
                        + "25\n"
                        + "+1234567890\n"
                        + "1\n"
                        + "D2\n"
                        + "Driver Two\n"
                        + "LIC123\n"
                        + "30\n"
                        + "+1987654321\n"
                        + "EXIT\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        runMenuWithInput(in);

        String out = output.toString();
        if (!out.contains("A driver with this license number is already registered")) {
            throw new AssertionError("Expected duplicate license error not found. Output: " + out);
        }
        int menuCount = countOccurrences(out, "--- Personnel Menu ---");
        if (menuCount < 3) {
            throw new AssertionError(
                    "Menu should appear at least 3 times (initial, after first registration, after"
                            + " duplicate error). Found "
                            + menuCount
                            + ". Output: "
                            + out);
        }
    }

    private static int countOccurrences(String text, String substr) {
        int count = 0;
        int idx = 0;
        while ((idx = text.indexOf(substr, idx)) != -1) {
            count++;
            idx += substr.length();
        }
        return count;
    }

    private static void testExitEndsCleanly() {
        String input = "EXIT\n";
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8));
        runMenuWithInput(in);

        String out = output.toString();
        if (!out.contains("Exiting...")) {
            throw new AssertionError("Expected 'Exiting...' message not found. Output: " + out);
        }
        if (out.contains("Exception") || out.contains("Stack trace") || out.contains("at ")) {
            throw new AssertionError("Stack trace found in output: " + out);
        }
    }

    private static void testSingleScannerInSource() {
        String projectRoot = System.getProperty("user.dir");
        String[] patterns = {"pom.xml", "mvnw", "build.gradle", "settings.gradle"};
        for (String pattern : patterns) {
            java.io.File file = new java.io.File(projectRoot, pattern);
            if (file.exists()) {
                throw new AssertionError("Found build file: " + pattern);
            }
        }
        java.io.File mvnDir = new java.io.File(projectRoot, ".mvn");
        if (mvnDir.exists() && mvnDir.isDirectory()) {
            throw new AssertionError("Found .mvn directory");
        }

        int scannerCount = countScannerConstructions(projectRoot + "/src");
        if (scannerCount != 1) {
            throw new AssertionError(
                    "Expected exactly 1 'new Scanner' construction, found " + scannerCount);
        }
    }

    private static int countScannerConstructions(String dirPath) {
        java.io.File dir = new java.io.File(dirPath);
        if (!dir.exists()) return 0;
        int count = 0;
        for (java.io.File file : dir.listFiles()) {
            if (file.isDirectory()) {
                count += countScannerConstructions(file.getAbsolutePath());
            } else if (file.getName().endsWith(".java")
                    && !file.getName().equals("PersonnelConsoleVerification.java")) {
                try {
                    String content = java.nio.file.Files.readString(file.toPath());
                    int idx = 0;
                    while ((idx = content.indexOf("new Scanner", idx)) != -1) {
                        count++;
                        idx += 11;
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return count;
    }

    private static void runMenuWithInput(ByteArrayInputStream input) {
        try {
            Class<?> menuClass = PersonnelMenu.class;
            Method mainMethod = menuClass.getDeclaredMethod("main", String[].class);
            mainMethod.setAccessible(true);

            System.setIn(input);
            mainMethod.invoke(null, (Object) new String[0]);
        } catch (Exception e) {
            if (e.getCause() instanceof ExitRequestedException) {
                return;
            }
            throw new RuntimeException(e);
        }
    }

    static class ExitRequestedException extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
