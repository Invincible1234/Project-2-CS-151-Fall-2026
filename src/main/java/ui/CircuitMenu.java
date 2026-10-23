package ui;

import f1.model.Circuit;
import f1.service.CircuitRegistry;
import java.util.Objects;
import model.WeatherCondition;

public final class CircuitMenu {
    private final ConsoleInput input;
    private final CircuitRegistry registry;

    public CircuitMenu(ConsoleInput input, CircuitRegistry registry) {
        this.input = Objects.requireNonNull(input, "Shared input is required.");
        this.registry = Objects.requireNonNull(registry, "Shared circuit registry is required.");
    }

    public void run() {
        while (true) {
            input.print("Circuits: 1 Create | 2 List | 3 Weather | 4 Corner note | 5 Record lap"
                    + " | 6 Strategy hint | 7 Long straights | 8 Remove historical lap | 0 Back");
            int option = input.integer("Choose a circuit action", 0, 8);
            if (option == 0) {
                return;
            }
            input.action(() -> perform(option));
        }
    }

    private void perform(int option) {
        if (option == 1) {
            createCircuit();
            return;
        }
        if (option == 2) {
            if (registry.getCircuits().isEmpty()) {
                input.print("No circuits registered.");
            }
            registry.getCircuits().forEach(circuit -> {
                input.print(circuit.toString());
                input.print("Corner notes: " + circuit.getCornerNotes());
                input.print("Historical laps in seconds: " + circuit.getHistoricalLapTimes());
            });
            return;
        }
        if (registry.getCircuits().isEmpty()) {
            throw new IllegalStateException("Register a circuit first.");
        }
        Circuit circuit = registry.findCircuit(input.text("Circuit ID"));
        switch (option) {
            case 3 -> circuit.updateConditions(input.choice("Weather", WeatherCondition.class));
            case 4 -> circuit.addCornerNote(input.text("Corner note"));
            case 5 -> circuit.recordHistoricalLap(input.decimal("Lap time in seconds", Double.MIN_VALUE,
                    Double.MAX_VALUE));
            case 6 -> {
                input.print(circuit.generateStrategyHint());
                input.print("Estimated base lap: " + circuit.estimateBaseLapTime() + " seconds.");
                return;
            }
            case 7 -> {
                boolean changed = circuit.setLongStraightSuitability(input.yesNo("Has long straights?"));
                input.print(changed ? "Long-straight suitability updated." : "Long-straight setting unchanged.");
                return;
            }
            case 8 -> {
                if (circuit.getHistoricalLapTimes().isEmpty()) {
                    throw new IllegalStateException("No historical laps to remove.");
                }
                input.print("Historical laps in seconds: " + circuit.getHistoricalLapTimes());
                int number = input.integer("Lap number to remove (starting at 1)", 1,
                        circuit.getHistoricalLapTimes().size());
                circuit.removeHistoricalLap(number - 1);
            }
            default -> throw new IllegalArgumentException("Unknown circuit action.");
        }
        input.print("Circuit updated: " + circuit.getCircuitId());
    }

    private void createCircuit() {
        String id = input.text("Unique circuit ID");
        registry.requireAvailableId(id);
        String name = input.text("Circuit name");
        String location = input.text("Location");
        double length = input.decimal("Circuit length in km", Double.MIN_VALUE, Double.MAX_VALUE / 40);
        int corners = input.integer("Number of corners", 1, Integer.MAX_VALUE);
        boolean straights = input.yesNo("Has long straights?");
        WeatherCondition weather = input.choice("Weather", WeatherCondition.class);
        registry.addCircuit(new Circuit(id, name, length, corners, straights, weather, location));
        input.print("Circuit registered: " + id);
    }
}
