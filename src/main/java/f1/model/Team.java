package f1.model;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

    public double getBudget(UserRole role) {
        requireTeamPrincipal(role);
        return budget;
    }

    public void addBudget(double amount, UserRole role) {
        requireTeamPrincipal(role);
        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException("Budget addition must be finite and positive.");
        }
        double updatedBudget = budget + amount;
        if (!Double.isFinite(updatedBudget)) {
            throw new IllegalArgumentException("Budget addition exceeds the supported amount.");
        }
        if (updatedBudget <= budget) {
            throw new IllegalArgumentException("Budget addition is too small for the current balance.");
        }
        budget = updatedBudget;
    }

    public boolean addCar(F1Car car) {
        return register(cars, car, F1Car::getChassisId, capacity, "Car");
    }

    public boolean addDriver(Driver driver) {
        return register(drivers, driver, Driver::getLicenseNumber,
                Constants.MAXIMUM_INSTANCES, "Driver");
    }

    public boolean addEngineer(Engineer engineer) {
        return register(engineers, engineer, Engineer::getEmployeeId,
                Constants.MAXIMUM_INSTANCES, "Engineer");
    }

    public boolean assignDriver(Driver driver, F1Car car) {
        Driver registeredDriver = findRegistered(drivers, driver, Driver::getLicenseNumber, "Driver");
        F1Car registeredCar = findRegistered(cars, car, F1Car::getChassisId, "Car");
        if (registeredDriver == null || registeredCar == null) {
            throw new IllegalArgumentException("Driver and car must be registered with this team.");
        }
        registeredDriver.assignToCar(registeredCar);
        return true;
    }

    public boolean removeCar(F1Car car) {
        F1Car registered = findRegistered(cars, car, F1Car::getChassisId, "Car");
        if (registered == null) {
            return false;
        }
        if (registered.hasScheduledStrategy() || registered.hasActiveAssignments()) {
            throw new IllegalStateException("Cannot remove a car with assignments or a scheduled strategy.");
        }
        return cars.removeIf(existing -> existing == registered);
    }

    public boolean removeDriver(Driver driver) {
        Driver registered = findRegistered(drivers, driver, Driver::getLicenseNumber, "Driver");
        if (registered == null) {
            return false;
        }
        registered.releaseAllAssignments();
        return drivers.removeIf(existing -> existing == registered);
    }

    public boolean removeEngineer(Engineer engineer) {
        Engineer registered = findRegistered(engineers, engineer, Engineer::getEmployeeId, "Engineer");
        if (registered == null) {
            return false;
        }
        registered.releaseFromCar();
        return engineers.removeIf(existing -> existing == registered);
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

    private static void requireTeamPrincipal(UserRole role) {
        if (role != UserRole.TEAM_PRINCIPAL) {
            throw new SecurityException("Team principal access is required.");
        }
    }

    private static <T> T findRegistered(List<T> members, T member,
            Function<T, String> identity, String type) {
        if (member == null) {
            throw new IllegalArgumentException(type + " is required.");
        }
        String memberId = requireText(identity.apply(member), type + " identity");
        for (T existing : members) {
            if (memberId.equals(requireText(identity.apply(existing), type + " identity"))) {
                return existing;
            }
        }
        return null;
    }

    private static <T> boolean register(List<T> members, T member,
            Function<T, String> identity, int limit, String type) {
        if (member == null) {
            throw new IllegalArgumentException(type + " is required.");
        }
        String memberId = requireText(identity.apply(member), type + " identity");
        for (T existing : members) {
            if (memberId.equals(requireText(identity.apply(existing), type + " identity"))) {
                throw new DuplicateEntityException(type + " is already registered with this team.");
            }
        }
        if (members.size() >= limit) {
            throw new CapacityExceededException(type + " capacity of " + limit + " has been reached.");
        }
        return members.add(member);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.strip();
    }
}
