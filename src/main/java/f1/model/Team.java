package f1.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import util.Constants;

public final class Team {
    private final String teamId;
    private final String name;
    private final int capacity;
    private final List<F1Car> cars;
    private final List<Driver> drivers;
    private final List<Engineer> engineers;
    private double budget;
    private int championshipPoints;
    private final Map<String, Integer> partsInventory;
    private final List<RaceResult> raceResults;
    private final List<RaceStrategy> strategies;

    public Team(String teamId, String name, int capacity, double budget) {
        this.teamId = requireText(teamId, "Team ID");
        this.name = requireText(name, "Team name");
        if (capacity < 1 || capacity > Constants.MAXIMUM_INSTANCES) {
            throw new IllegalArgumentException(
                    "Team capacity must be between 1 and " + Constants.MAXIMUM_INSTANCES + ".");
        }
        if (!Double.isFinite(budget) || budget < 0) {
            throw new IllegalArgumentException("Team budget must be finite and nonnegative.");
        }
        this.capacity = capacity;
        this.budget = budget;
        this.cars = new ArrayList<>();
        this.drivers = new ArrayList<>();
        this.engineers = new ArrayList<>();
        this.partsInventory = new HashMap<>();
        this.raceResults = new ArrayList<>();
        this.strategies = new ArrayList<>();
    }

    public String getTeamId() {
        return teamId;
    }

    public String getName() {
        return name;
    }

    public int getCapacity() {
        return capacity;
    }

    public int getChampionshipPoints() {
        return championshipPoints;
    }

    public List<F1Car> getCars() {
        return List.copyOf(cars);
    }

    public List<Driver> getDrivers() {
        return List.copyOf(drivers);
    }

    public List<Engineer> getEngineers() {
        return List.copyOf(engineers);
    }

    public List<RaceResult> getRaceResults() {
        return List.copyOf(raceResults);
    }

    public List<RaceStrategy> getStrategies() {
        return List.copyOf(strategies);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Team team && teamId.equals(team.teamId);
    }

    @Override
    public int hashCode() {
        return teamId.hashCode();
    }

    @Override
    public String toString() {
        return "Team{teamId='" + teamId + "', name='" + name + "', capacity=" + capacity
                + ", cars=" + cars.size() + ", drivers=" + drivers.size()
                + ", engineers=" + engineers.size()
                + ", championshipPoints=" + championshipPoints
                + ", raceResults=" + raceResults.size() + ", strategies=" + strategies.size() + "}";
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.strip();
    }
}
