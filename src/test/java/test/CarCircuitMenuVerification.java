package test;

import f1.model.ClassicRaceCar;
import f1.model.HybridRaceCar;
import f1.model.Team;
import f1.service.CircuitRegistry;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.StringReader;
import model.WeatherCondition;
import ui.CarMenu;
import ui.CircuitMenu;
import ui.ConsoleInput;

public final class CarCircuitMenuVerification {
    public static void main(String[] args) {
        verifySharedInputAndActions();
        verifyClassicAndRemoval();
        verifyExitAndEndOfInput();
        System.out.println("Car and circuit menu verification passed.");
    }

    private static void verifySharedInputAndActions() {
        Team team = new Team("UI", "Apex", 2, 1000);
        CircuitRegistry circuits = new CircuitRegistry();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleInput input = input(output, """
                bad
                1
                H-UI
                Hybrid
                nope
                2026
                NaN
                4
                5
                3
                351
                300
                1
                H-UI
                4
                UNKNOWN
                4
                H-UI
                invalid
                practice
                5
                H-UI
                -1
                Infinity
                12.5
                6
                H-UI
                1
                low_drag
                3
                10
                H-UI
                0
                1
                C-UI
                Test Circuit
                Test Location
                5
                12
                maybe
                YES
                invalid
                CLEAR
                1
                C-UI
                3
                UNKNOWN
                3
                C-UI
                RAIN
                4
                C-UI
                Tight first corner
                4
                C-UI
                Tight first corner
                5
                C-UI
                NaN
                100
                5
                C-UI
                110
                8
                C-UI
                0
                1
                7
                C-UI
                NO
                6
                C-UI
                2
                0
                """);
        input.run(() -> {
            new CarMenu(input, team).run();
            new CircuitMenu(input, circuits).run();
        });
        HybridRaceCar car = (HybridRaceCar) team.getCars().get(0);
        check(team.getCars().size() == 1 && car.getTotalMileage() == 12.5
                && car.getCurrentAeroConfig().equals("LOW_DRAG") && !car.isRaceReady()
                && car.getPreparedSessionType() == null, "Car actions did not preserve validation and setup rules.");
        var circuit = circuits.findCircuit("C-UI");
        check(circuits.getCircuits().size() == 1 && circuit.getTypicalWeather() == WeatherCondition.RAIN
                && circuit.getCornerNotes().size() == 1 && circuit.getHistoricalLapTimes().size() == 1
                && circuit.estimateBaseLapTime() == 110 && !circuit.hasLongStraights(),
                "Circuit actions did not update the shared registry correctly.");
        expect(UnsupportedOperationException.class, () -> circuits.getCircuits().clear());
        String transcript = output.toString();
        check(transcript.contains("already registered") && transcript.contains("No car has")
                && transcript.contains("No circuit has") && transcript.contains("INTERMEDIATE")
                && !transcript.contains("Exception") && !transcript.contains("Goodbye."),
                "Failed actions must explain the error and keep the shared stream usable.");
    }

    private static void verifyClassicAndRemoval() {
        Team team = new Team("CLASSIC-UI", "Heritage", 2, 1000);
        ConsoleInput input = input(new ByteArrayOutputStream(), """
                2
                CLASSIC
                Historic
                1980
                90
                2020
                250
                8
                CLASSIC
                NO
                8
                CLASSIC
                YES
                1
                REMOVE
                Hybrid
                2026
                4
                3
                300
                7
                REMOVE
                0
                """);
        input.run(() -> new CarMenu(input, team).run());
        check(team.getCars().size() == 1 && team.getCars().get(0) instanceof ClassicRaceCar
                && team.getCars().get(0).isPermanentlyDecommissioned(),
                "Classic creation, confirmed retirement, or car removal failed.");
    }

    private static void verifyExitAndEndOfInput() {
        Team team = new Team("EXIT-UI", "Apex", 2, 1000);
        for (String script : new String[] {"EXIT\n", "1\nNEW\nEXIT\n", "1\nNEW\n"}) {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            ConsoleInput input = input(output, script);
            input.run(() -> {
                new CarMenu(input, team).run();
                throw new AssertionError("EXIT must leave the entire workflow.");
            });
            check(team.getCars().isEmpty() && output.toString().contains("Goodbye."),
                    "EXIT or EOF must terminate without partially registering a car.");
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ConsoleInput input = input(output, "1\nCIRCUIT\nEXIT\n");
        CircuitRegistry registry = new CircuitRegistry();
        input.run(() -> new CircuitMenu(input, registry).run());
        check(registry.getCircuits().isEmpty(), "EXIT partially registered a circuit.");
    }

    private static ConsoleInput input(ByteArrayOutputStream output, String script) {
        return new ConsoleInput(new StringReader(script), new PrintStream(output));
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("Unexpected rejection.", error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
