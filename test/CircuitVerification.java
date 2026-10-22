package test;

import f1.model.Circuit;
import java.util.HashSet;
import java.util.List;
import model.WeatherCondition;

public class CircuitVerification {
    public static void main(String[] args) {
        Circuit circuit = new Circuit(" C1 ", " Test Circuit ", 5, 10, true,
                WeatherCondition.CLEAR, " Test Location ");
        check(circuit.getCircuitId().equals("C1") && circuit.getName().equals("Test Circuit")
                && circuit.getLocation().equals("Test Location"), "Text was not normalized.");
        check(circuit.getLengthKm() == 5 && circuit.getNumberOfCorners() == 10
                && circuit.hasLongStraights(), "Track properties were not retained.");
        check(circuit.estimateBaseLapTime() == 105, "Unexpected baseline estimate.");
        for (double invalid : new double[] {0, -1, Double.NaN,
                Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.MAX_VALUE}) {
            expect(IllegalArgumentException.class,
                    () -> new Circuit("BAD", "Track", invalid, 10, true, WeatherCondition.CLEAR, "Location"));
        }
        expect(IllegalArgumentException.class,
                () -> new Circuit("BAD", "Track", 5, 0, true, WeatherCondition.CLEAR, "Location"));
        expect(IllegalArgumentException.class,
                () -> new Circuit("BAD", "Track", 5, 10, true, null, "Location"));
        expect(IllegalArgumentException.class,
                () -> new Circuit(" ", "Track", 5, 10, true, WeatherCondition.CLEAR, "Location"));
        expect(IllegalArgumentException.class,
                () -> new Circuit("BAD", null, 5, 10, true, WeatherCondition.CLEAR, "Location"));
        expect(IllegalArgumentException.class,
                () -> new Circuit("BAD", "Track", 5, 10, true, WeatherCondition.CLEAR, " "));

        List<String> originalNotes = circuit.getCornerNotes();
        circuit.addCornerNote("  Brake early at turn one  ");
        check(originalNotes.isEmpty()
                && circuit.getCornerNotes().equals(List.of("Brake early at turn one")),
                "Notes were not normalized or snapshots changed.");
        expect(UnsupportedOperationException.class, () -> circuit.getCornerNotes().clear());
        expect(IllegalArgumentException.class, () -> circuit.addCornerNote("Brake early at turn one"));
        expect(IllegalArgumentException.class, () -> circuit.addCornerNote(" "));
        check(circuit.getCornerNotes().size() == 1, "Rejected note changed history.");

        List<Double> originalTimes = circuit.getHistoricalLapTimes();
        circuit.recordHistoricalLap(100);
        circuit.recordHistoricalLap(110);
        check(originalTimes.isEmpty() && circuit.estimateBaseLapTime() == 105,
                "Historical average or snapshot is incorrect.");
        expect(UnsupportedOperationException.class, () -> circuit.getHistoricalLapTimes().add(90.0));
        for (double invalid : new double[] {0, -1, Double.NaN, Double.POSITIVE_INFINITY}) {
            expect(IllegalArgumentException.class, () -> circuit.recordHistoricalLap(invalid));
        }
        expect(IllegalArgumentException.class, () -> circuit.removeHistoricalLap(-1));
        expect(IllegalArgumentException.class, () -> circuit.removeHistoricalLap(2));
        check(circuit.getHistoricalLapTimes().equals(List.of(100.0, 110.0)),
                "Rejected lap action changed history.");
        circuit.removeHistoricalLap(0);
        check(circuit.estimateBaseLapTime() == 110, "Removing a bad sample did not update estimates.");
        circuit.removeHistoricalLap(0);
        check(circuit.estimateBaseLapTime() == 105, "Empty history did not use the baseline.");
        circuit.recordHistoricalLap(Double.MAX_VALUE);
        circuit.recordHistoricalLap(Double.MAX_VALUE);
        check(circuit.estimateBaseLapTime() == Double.MAX_VALUE, "Averaging overflowed.");

        int stableHash = circuit.hashCode();
        HashSet<Circuit> identities = new HashSet<>();
        identities.add(circuit);
        Circuit same = new Circuit("C1", "Another label", 3, 8, false, WeatherCondition.RAIN, "Other");
        check(!identities.add(same) && circuit.equals(same) && same.equals(circuit)
                && stableHash == same.hashCode(), "Business identity is inconsistent.");
        for (WeatherCondition weather : WeatherCondition.values()) {
            circuit.updateConditions(weather);
            check(circuit.getTypicalWeather() == weather && !circuit.generateStrategyHint().isBlank(),
                    "Weather change was not reflected in strategy advice.");
        }
        circuit.updateConditions(WeatherCondition.RAIN);
        check(circuit.generateStrategyHint().contains("INTERMEDIATE"), "Rain advice is missing.");
        circuit.updateConditions(WeatherCondition.WET);
        check(circuit.generateStrategyHint().contains("WET"), "Wet-weather advice is missing.");
        expect(IllegalArgumentException.class, () -> circuit.updateConditions(null));
        check(circuit.getTypicalWeather() == WeatherCondition.WET, "Invalid weather changed conditions.");
        circuit.updateConditions(WeatherCondition.CLEAR);
        check(circuit.generateStrategyHint().contains("long straights"), "Layout advice is missing.");
        check(circuit.setLongStraightSuitability(false), "Suitability change was rejected.");
        check(!circuit.setLongStraightSuitability(false), "An unchanged flag reported a modification.");
        check(!circuit.generateStrategyHint().contains("long straights")
                && circuit.hashCode() == stableHash && identities.contains(circuit),
                "Mutable conditions corrupted advice or stable identity.");
        check(circuit.toString().contains("C1"), "Summary does not identify the circuit.");
        System.out.println("Circuit verification passed.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("Unexpected exception type.", error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
