package f1.model;

import java.util.ArrayList;
import java.util.List;
import model.WeatherCondition;
import util.InstanceLimit;
import util.Validation;

public final class Circuit {
    private final String circuitId;
    private final String name;
    private final double lengthKm;
    private final int numberOfCorners;
    private boolean hasLongStraights;
    private WeatherCondition typicalWeather;
    private final String location;
    private final List<String> cornerNotes = new ArrayList<>();
    private final List<Double> historicalLapTimes = new ArrayList<>();

    public Circuit(String circuitId, String name, double lengthKm, int numberOfCorners,
            boolean hasLongStraights, WeatherCondition typicalWeather, String location) {
        this.circuitId = Validation.requireText(circuitId, "Circuit ID");
        this.name = Validation.requireText(name, "Circuit name");
        this.location = Validation.requireText(location, "Circuit location");
        if (!Double.isFinite(lengthKm) || lengthKm <= 0 || numberOfCorners < 1
                || !Double.isFinite(lengthKm * 20 + numberOfCorners * 0.5)) {
            throw new IllegalArgumentException("Circuit length must be finite and positive, with at least one corner.");
        }
        if (typicalWeather == null) {
            throw new IllegalArgumentException("Circuit weather is required.");
        }
        this.lengthKm = lengthKm;
        this.numberOfCorners = numberOfCorners;
        this.hasLongStraights = hasLongStraights;
        this.typicalWeather = typicalWeather;
        InstanceLimit.checkAndCount(Circuit.class);
    }

    public String getCircuitId() {
        return circuitId;
    }

    public String getName() {
        return name;
    }

    public double getLengthKm() {
        return lengthKm;
    }

    public int getNumberOfCorners() {
        return numberOfCorners;
    }

    public boolean hasLongStraights() {
        return hasLongStraights;
    }

    public WeatherCondition getTypicalWeather() {
        return typicalWeather;
    }

    public String getLocation() {
        return location;
    }

    public List<String> getCornerNotes() {
        return List.copyOf(cornerNotes);
    }

    public List<Double> getHistoricalLapTimes() {
        return List.copyOf(historicalLapTimes);
    }

    public void updateConditions(WeatherCondition weather) {
        if (weather == null) {
            throw new IllegalArgumentException("Weather is required.");
        }
        typicalWeather = weather;
    }

    public void addCornerNote(String note) {
        String validated = Validation.requireText(note, "Corner note");
        if (cornerNotes.contains(validated)) {
            throw new IllegalArgumentException("This corner note is already recorded.");
        }
        cornerNotes.add(validated);
    }

    public boolean setLongStraightSuitability(boolean longStraights) {
        if (hasLongStraights == longStraights) {
            return false;
        }
        hasLongStraights = longStraights;
        return true;
    }

    public void recordHistoricalLap(double timeSeconds) {
        if (!Double.isFinite(timeSeconds) || timeSeconds <= 0) {
            throw new IllegalArgumentException("Lap time must be finite and positive in seconds.");
        }
        historicalLapTimes.add(timeSeconds);
    }

    public void removeHistoricalLap(int index) {
        if (index < 0 || index >= historicalLapTimes.size()) {
            throw new IllegalArgumentException("Choose an existing historical lap index.");
        }
        historicalLapTimes.remove(index);
    }

    public double estimateBaseLapTime() {
        if (historicalLapTimes.isEmpty()) {
            return lengthKm * 20 + numberOfCorners * 0.5;
        }
        double average = 0;
        for (int i = 0; i < historicalLapTimes.size(); i++) {
            average += (historicalLapTimes.get(i) - average) / (i + 1);
        }
        return average;
    }

    public String generateStrategyHint() {
        String tires = switch (typicalWeather) {
            case RAIN -> "Use INTERMEDIATE tires and conservative energy deployment.";
            case WET -> "Use WET tires and prioritize grip over boost.";
            case EXTREME_HEAT -> "Consider HARD tires and monitor tire wear and cooling.";
            case CLEAR, CLOUDY -> "Use dry-weather tires suited to the planned stint.";
        };
        boolean dry = typicalWeather != WeatherCondition.RAIN && typicalWeather != WeatherCondition.WET;
        String layout = hasLongStraights && dry
                ? " Plan available boost on long straights within lap limits."
                : " Prioritize traction and corner stability.";
        return tires + layout;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Circuit circuit && circuitId.equals(circuit.circuitId);
    }

    @Override
    public int hashCode() {
        return circuitId.hashCode();
    }

    @Override
    public String toString() {
        return "Circuit{id='" + circuitId + "', name='" + name + "', lengthKm=" + lengthKm
                + ", corners=" + numberOfCorners + ", weather=" + typicalWeather
                + ", location='" + location + "', notes=" + cornerNotes.size()
                + ", historicalLaps=" + historicalLapTimes.size() + "}";
    }
}
