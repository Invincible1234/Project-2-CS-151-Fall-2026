package f1.model;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;
import exception.InsufficientResourcesException;
import java.util.ArrayList;
import java.util.HashMap;
import model.StrategyStatus;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import util.Constants;
import util.PartsCatalog;

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
    private final Map<RaceStrategy, Map<String, Integer>> strategyReservations = new HashMap<>();
    private final Map<RaceStrategy, RaceResult> strategyResults = new HashMap<>();
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

    public boolean addParts(String partType, int quantity) {
        String part = PartsCatalog.normalizePartType(partType);
        requirePositiveQuantity(quantity);
        int currentQuantity = partsInventory.getOrDefault(part, 0);
        if (quantity > Integer.MAX_VALUE - currentQuantity) {
            throw new CapacityExceededException("Part quantity exceeds the supported inventory range.");
        }
        double cost = PartsCatalog.getUnitPrice(part) * quantity;
        if (!Double.isFinite(cost) || cost <= 0) {
            throw new IllegalArgumentException("Purchase cost must be finite and positive.");
        }
        if (cost > budget) {
            throw new InsufficientResourcesException("Not enough budget to purchase the requested parts.");
        }
        double updatedBudget = budget - cost;
        if (updatedBudget >= budget) {
            throw new IllegalArgumentException("Purchase cost is too small for the current balance.");
        }
        partsInventory.put(part, currentQuantity + quantity);
        budget = updatedBudget;
        return true;
    }

    public boolean consumeParts(String partType, int quantity) {
        String part = PartsCatalog.normalizePartType(partType);
        requirePositiveQuantity(quantity);
        int currentQuantity = partsInventory.getOrDefault(part, 0);
        if (quantity > availableParts(part)) {
            throw new InsufficientResourcesException("Not enough unreserved parts in inventory.");
        }
        int remaining = currentQuantity - quantity;
        if (remaining == 0) {
            partsInventory.remove(part);
        } else {
            partsInventory.put(part, remaining);
        }
        return true;
    }

    public Map<String, Integer> getPartsInventory(UserRole role) {
        if (role != UserRole.TEAM_PRINCIPAL && role != UserRole.ENGINEER) {
            throw new SecurityException("Team principal or engineer access is required.");
        }
        return Map.copyOf(partsInventory);
    }

    void validateStrategyResources(RaceStrategy strategy, Map<String, Integer> requiredParts, Driver driver) {
        if (strategy.getRegisteredTeam() != this || strategies.stream().noneMatch(existing -> existing == strategy)
                || cars.stream().noneMatch(existing -> existing == strategy.getCar())
                || drivers.stream().noneMatch(existing -> existing == driver)
                || engineers.stream().noneMatch(existing -> existing == strategy.getEngineer())) {
            throw new IllegalStateException("Strategy resources must be registered with this team.");
        }
        if (!Double.isFinite(budget) || budget < 0) {
            throw new IllegalStateException("Team budget is invalid.");
        }
        if (strategyReservations.containsKey(strategy)) {
            throw new IllegalStateException("Strategy already holds resource reservations.");
        }
        for (RaceStrategy reserved : strategyReservations.keySet()) {
            if (reserved.getCar() == strategy.getCar() || reserved.getEngineer() == strategy.getEngineer()
                    || reserved.getCar().getAssignedDriver() == driver) {
                throw new IllegalStateException("Car or personnel are reserved by another strategy.");
            }
        }
        for (Map.Entry<String, Integer> part : requiredParts.entrySet()) {
            if (part.getValue() > availableParts(part.getKey())) {
                throw new InsufficientResourcesException("Not enough unreserved parts for this strategy.");
            }
        }
    }

    void reserveStrategyResources(RaceStrategy strategy, Map<String, Integer> requiredParts, Driver driver) {
        validateStrategyResources(strategy, requiredParts, driver);
        strategyReservations.put(strategy, Map.copyOf(requiredParts));
    }

    void releaseStrategyResources(RaceStrategy strategy) {
        strategyReservations.remove(strategy);
    }

    void validateReservedParts(RaceStrategy strategy) {
        Map<String, Integer> reserved = strategyReservations.get(strategy);
        if (reserved == null || !reserved.equals(strategy.getRequiredParts())) {
            throw new IllegalStateException("Strategy must hold its required parts reservation.");
        }
        for (Map.Entry<String, Integer> part : reserved.entrySet()) {
            if (partsInventory.getOrDefault(part.getKey(), 0) < part.getValue()) {
                throw new InsufficientResourcesException("Reserved strategy parts are missing from inventory.");
            }
        }
    }

    void consumeReservedParts(RaceStrategy strategy) {
        validateReservedParts(strategy);
        for (Map.Entry<String, Integer> part : strategyReservations.get(strategy).entrySet()) {
            int remaining = partsInventory.get(part.getKey()) - part.getValue();
            if (remaining == 0) {
                partsInventory.remove(part.getKey());
            } else {
                partsInventory.put(part.getKey(), remaining);
            }
        }
        releaseStrategyResources(strategy);
    }

    private int availableParts(String partType) {
        int available = partsInventory.getOrDefault(partType, 0);
        for (Map<String, Integer> reserved : strategyReservations.values()) {
            available -= reserved.getOrDefault(partType, 0);
        }
        return available;
    }

    public boolean addCar(F1Car car) {
        validateRegistration(cars, car, F1Car::getChassisId, capacity, "Car");
        car.changeRegisteredTeam(null, this);
        return cars.add(car);
    }

    public boolean addDriver(Driver driver) {
        return register(drivers, driver, Driver::getLicenseNumber,
                Constants.MAXIMUM_INSTANCES, "Driver");
    }

    public boolean addEngineer(Engineer engineer) {
        return register(engineers, engineer, Engineer::getEmployeeId,
                Constants.MAXIMUM_INSTANCES, "Engineer");
    }

    public boolean addStrategy(RaceStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Strategy is required.");
        }
        if (strategies.contains(strategy)) {
            throw new DuplicateEntityException("Strategy is already registered for this chassis.");
        }
        if (strategies.size() >= Constants.MAXIMUM_INSTANCES) {
            throw new CapacityExceededException("Strategy capacity has been reached.");
        }
        if (strategy.getRegisteredTeam() != null) {
            throw new IllegalStateException("Strategy is already registered with a team.");
        }
        F1Car car = strategy.getCar();
        Engineer engineer = strategy.getEngineer();
        if (findRegistered(cars, car, F1Car::getChassisId, "Car") != car
                || car.getRegisteredTeam() != this) {
            throw new IllegalArgumentException("Strategy must reference this team's registered car.");
        }
        if (findRegistered(engineers, engineer, Engineer::getEmployeeId, "Engineer") != engineer) {
            throw new IllegalArgumentException("Strategy must reference this team's registered engineer.");
        }
        strategy.registerWithTeam(this);
        return strategies.add(strategy);
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
        if (registered.isSessionActive() || registered.hasScheduledStrategy() || registered.hasActiveAssignments()
                || registered instanceof ClassicRaceCar classic && classic.isRestorationInProgress()) {
            throw new IllegalStateException("Cannot remove a car with active work, assignments, or a strategy.");
        }
        registered.changeRegisteredTeam(this, null);
        return cars.removeIf(existing -> existing == registered);
    }

    void transferClassicCar(ClassicRaceCar car, Team destination) {
        if (destination == null || equals(destination)) {
            throw new IllegalArgumentException("Choose a different destination team.");
        }
        if (car == null || findRegistered(cars, car, F1Car::getChassisId, "Car") != car) {
            throw new IllegalArgumentException("The source team must own this exact car.");
        }
        if (car.isSessionActive() || car.hasScheduledStrategy() || car.hasActiveAssignments()
                || car.isRestorationInProgress() || car.isPermanentlyDecommissioned()) {
            throw new IllegalStateException("Resolve car commitments before transferring ownership.");
        }
        validateRegistration(destination.cars, car, F1Car::getChassisId, destination.capacity, "Car");
        car.changeRegisteredTeam(this, destination);
        cars.removeIf(existing -> existing == car);
        destination.cars.add(car);
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

    public void recordRaceResult(RaceResult result) {
        recordRaceResult(result, null);
    }

    public void recordRaceResult(RaceResult result, RaceStrategy completedStrategy) {
        if (result == null || !result.isValidResult()) {
            throw new IllegalArgumentException("A valid race result is required.");
        }
        if (result.hasPointsAccount()) {
            throw new IllegalStateException("This result is already registered with a team.");
        }
        String resultId = requireText(result.getResultId(), "Result ID");
        String chassisId = requireText(result.getChassisId(), "Result chassis ID");
        String driverId = requireText(result.getDriverId(), "Result driver ID");
        requireText(result.getCircuitId(), "Result circuit ID");
        for (RaceResult existing : raceResults) {
            if (resultId.equals(existing.getResultId().strip()) || result.hasSameRaceEntry(existing)
                    || result.getRaceId().equals(existing.getRaceId())
                    && chassisId.equals(existing.getChassisId())) {
                throw new DuplicateEntityException("This race result has already been recorded.");
            }
        }
        if (raceResults.size() >= Constants.MAXIMUM_INSTANCES) {
            throw new CapacityExceededException("Race result capacity has been reached.");
        }
        boolean registeredCar = cars.stream().anyMatch(car -> chassisId.equals(car.getChassisId().strip()));
        if (!registeredCar) {
            throw new IllegalArgumentException("Result car must be registered with this team.");
        }
        Driver resultDriver = null;
        for (Driver driver : drivers) {
            if (driverId.equals(driver.getDriverId())) {
                if (resultDriver != null) {
                    throw new IllegalArgumentException("Result driver ID is ambiguous within this team.");
                }
                resultDriver = driver;
            }
        }
        if (resultDriver == null) {
            throw new IllegalArgumentException("Result driver must be registered with this team.");
        }
        RaceStrategy source = completedStrategy;
        if (source == null) {
            for (RaceStrategy candidate : strategies) {
                if (matchesCompletedEntry(candidate, result, resultDriver)
                        && !strategyResults.containsKey(candidate)) {
                    if (source != null) {
                        throw new IllegalStateException("Specify which completed strategy produced this result.");
                    }
                    source = candidate;
                }
            }
        }
        if (source == null || !matchesCompletedEntry(source, result, resultDriver)) {
            throw new IllegalStateException("Result must match a completed strategy registered with this team.");
        }
        if (strategyResults.containsKey(source)) {
            throw new DuplicateEntityException("This completed strategy already has a result.");
        }
        int points = result.getPointsEarned();
        if (result.getFinishingPosition() < 1 || points < 0) {
            throw new IllegalArgumentException("Result position must be positive and points nonnegative.");
        }
        if (points > Integer.MAX_VALUE - championshipPoints
                || points > Integer.MAX_VALUE - resultDriver.getChampionshipPoints()) {
            throw new IllegalStateException("Championship point total exceeds the supported range.");
        }
        Driver registeredDriver = resultDriver;
        result.bindPointsAccount(delta -> adjustResultPoints(result, registeredDriver, delta));
        raceResults.add(result);
        strategyResults.put(source, result);
        result.applyPointsToDriverAndTeam();
    }

    private boolean matchesCompletedEntry(RaceStrategy strategy, RaceResult result, Driver driver) {
        return strategy.getRegisteredTeam() == this
                && strategies.stream().anyMatch(candidate -> candidate == strategy)
                && cars.stream().anyMatch(candidate -> candidate == strategy.getCar())
                && strategy.getStatus() == StrategyStatus.COMPLETED
                && strategy.getRaceDriver() == driver
                && strategy.getChassisId().equals(result.getChassisId())
                && strategy.getCircuit().getCircuitId().equals(result.getCircuitId());
    }

    private void adjustResultPoints(RaceResult result, Driver driver, int delta) {
        if (raceResults.stream().noneMatch(existing -> existing == result)) {
            throw new IllegalStateException("The result is not registered with this team.");
        }
        long teamTotal = (long) championshipPoints + delta;
        long driverTotal = (long) driver.getChampionshipPoints() + delta;
        if (teamTotal < 0 || driverTotal < 0
                || teamTotal > Integer.MAX_VALUE || driverTotal > Integer.MAX_VALUE) {
            throw new IllegalStateException("Corrected championship points exceed the supported range.");
        }
        driver.adjustChampionshipPoints(delta);
        championshipPoints = (int) teamTotal;
    }

    public String generateTeamPerformanceReport() {
        int readyCars = 0;
        int decommissionedCars = 0;
        double totalMileage = 0;
        for (F1Car car : cars) {
            if (car.isRaceReady() && !car.isPermanentlyDecommissioned()) {
                readyCars++;
            }
            if (car.isPermanentlyDecommissioned()) {
                decommissionedCars++;
            }
            totalMileage += car.getTotalMileage();
        }
        int wins = 0;
        int podiums = 0;
        int bestFinish = Integer.MAX_VALUE;
        double finishTotal = 0;
        List<RaceResult> validResults = raceResults.stream().filter(RaceResult::isValidResult).toList();
        for (RaceResult result : validResults) {
            int position = result.getFinishingPosition();
            if (position == 1) {
                wins++;
            }
            if (position <= 3) {
                podiums++;
            }
            bestFinish = Math.min(bestFinish, position);
            finishTotal += position;
        }
        String mileage = Double.isFinite(totalMileage) && totalMileage >= 0
                ? String.format(Locale.ROOT, "%.2f", totalMileage) : "Unavailable";
        StringBuilder report = new StringBuilder("Team performance: ")
                .append(name).append(" (").append(teamId).append(")\n")
                .append("Cars: ").append(cars.size()).append('/').append(capacity).append('\n')
                .append("Ready cars: ").append(readyCars).append('\n')
                .append("Decommissioned cars: ").append(decommissionedCars).append('\n')
                .append("Total mileage (km): ").append(mileage).append('\n')
                .append("Drivers: ").append(drivers.size()).append('\n')
                .append("Engineers: ").append(engineers.size()).append('\n')
                .append("Recorded results: ").append(validResults.size()).append('\n')
                .append("Championship points: ").append(championshipPoints).append('\n')
                .append("Wins: ").append(wins).append('\n')
                .append("Podiums: ").append(podiums).append('\n')
                .append("Best finish: ").append(validResults.isEmpty() ? "N/A" : bestFinish).append('\n')
                .append("Average finish: ").append(validResults.isEmpty() ? "N/A"
                        : String.format(Locale.ROOT, "%.2f", finishTotal / validResults.size()))
                .append("\nDriver standings:\n");
        if (drivers.isEmpty()) {
            report.append("No registered drivers.\n");
        } else {
            drivers.stream()
                    .sorted(java.util.Comparator.comparingInt(Driver::getChampionshipPoints).reversed()
                            .thenComparing(Driver::getDriverId))
                    .forEach(driver -> report.append(driver.getDriverId()).append(": ")
                            .append(driver.getChampionshipPoints()).append(" points\n"));
        }
        return report.toString();
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

    private static void requirePositiveQuantity(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Part quantity must be positive.");
        }
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
        validateRegistration(members, member, identity, limit, type);
        return members.add(member);
    }

    private static <T> void validateRegistration(List<T> members, T member,
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
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.strip();
    }
}
