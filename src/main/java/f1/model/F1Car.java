package f1.model;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import model.WeatherCondition;

public abstract class F1Car implements Raceable {
    public abstract double calculateLapTimeDelta(Circuit circuit, WeatherCondition weather);

    public abstract String getRegulatoryComplianceNotes();

    private static final double MAINTENANCE_INTERVAL_KM = 500.0;
    private final String chassisId;
    private String teamName;
    private Team registeredTeam;
    private final String model;
    private final int year;
    private double totalMileage;
    private double mileageSinceService;
    private final Clock clock;
    private String outOfServiceReason = "";
    private String lastServiceDate = "";
    private String lastServiceNotes = "";
    private boolean raceReady;
    private boolean sessionActive;
    private SessionType preparedSessionType;
    private String lastSetupDate;
    private String powerUnitType;
    private AeroConfiguration currentAeroConfig;
    private boolean permanentlyDecommissioned;
    private String scheduledStrategy;
    private RaceStrategy activeStrategy;
    private Driver assignedDriver;
    private Engineer assignedEngineer;

    protected F1Car(String chassisId, String teamName, String model, int year) {
        this(chassisId, teamName, model, year, Clock.systemDefaultZone());
    }

    protected F1Car(String chassisId, String teamName, String model, int year, Clock clock) {
        this.clock = Objects.requireNonNull(clock, "Clock is required.");
        if (chassisId == null || teamName == null || model == null) {
            throw new IllegalArgumentException("Chassis ID, team, and model are required.");
        }
        if (chassisId.isBlank() || teamName.isBlank() || model.isBlank() || year < 1950) {
            throw new IllegalArgumentException("Names must be nonblank and year must be at least 1950.");
        }
        this.chassisId = chassisId.trim();
        this.teamName = teamName.trim();
        this.model = model.trim();
        this.year = year;
        this.lastSetupDate = "";
        this.powerUnitType = "";
        this.permanentlyDecommissioned = false;
        this.scheduledStrategy = "";
    }

    public String getChassisId() {
        return chassisId;
    }

    public String getTeamName() {
        return teamName;
    }

    public Team getRegisteredTeam() {
        return registeredTeam;
    }

    final void changeRegisteredTeam(Team expected, Team replacement) {
        if (registeredTeam != expected || sessionActive) {
            throw new IllegalStateException("Car registration does not match the source team.");
        }
        registeredTeam = replacement;
        if (replacement != null) {
            teamName = replacement.getName();
        }
    }

    protected final void changeUnregisteredTeamName(String newTeam) {
        if (registeredTeam != null) {
            throw new IllegalStateException("Transfer a registered car using the destination Team object.");
        }
        teamName = newTeam;
    }

    public String getModel() {
        return model;
    }

    public int getYear() {
        return year;
    }

    public double getTotalMileage() {
        return totalMileage;
    }

    public boolean isRaceReady() {
        return raceReady;
    }

    public String getLastSetupDate() {
        return lastSetupDate;
    }

    protected final LocalDate currentDate() {
        return LocalDate.now(clock);
    }

    public String getPowerUnitType() {
        return powerUnitType;
    }

    public String getCurrentAeroConfig() {
        return currentAeroConfig == null ? "" : currentAeroConfig.name();
    }

    public boolean isPermanentlyDecommissioned() {
        return permanentlyDecommissioned;
    }

    public void updateMileage(double kilometers) {
        if (activeStrategy != null) {
            throw new IllegalStateException("Record strategy mileage through strategy completion.");
        }
        if (!Double.isFinite(kilometers) || kilometers < 0) {
            throw new IllegalArgumentException("Mileage must be finite and nonnegative.");
        }
        if (permanentlyDecommissioned) {
            throw new IllegalStateException("Cannot add mileage to a decommissioned car.");
        }
        double updatedTotal = totalMileage + kilometers;
        double updatedSinceService = mileageSinceService + kilometers;
        if (!Double.isFinite(updatedTotal) || !Double.isFinite(updatedSinceService)) {
            throw new IllegalArgumentException("Mileage would exceed the supported range.");
        }
        totalMileage = updatedTotal;
        mileageSinceService = updatedSinceService;
        if (isDueForMaintenance()) {
            this.raceReady = false;
        }
    }

    public double getMileageSinceService() {
        return mileageSinceService;
    }

    public boolean isDueForMaintenance() {
        return mileageSinceService >= MAINTENANCE_INTERVAL_KM;
    }

    public String getOutOfServiceReason() {
        return outOfServiceReason;
    }

    public String getLastServiceDate() {
        return lastServiceDate;
    }

    public String getLastServiceNotes() {
        return lastServiceNotes;
    }

    public void markOutOfService(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("An out-of-service reason is required.");
        }
        if (sessionActive || permanentlyDecommissioned) {
            throw new IllegalStateException("Cannot change service status during a session or after retirement.");
        }
        outOfServiceReason = reason.trim();
        raceReady = false;
        preparedSessionType = null;
    }

    public void performService(String notes) {
        if (notes == null || notes.isBlank()) {
            throw new IllegalArgumentException("Service notes are required.");
        }
        if (sessionActive || permanentlyDecommissioned) {
            throw new IllegalStateException("Cannot service an active or decommissioned car.");
        }
        String serviceDate = LocalDate.now(clock).toString();
        lastServiceDate = serviceDate;
        lastServiceNotes = notes.trim();
        mileageSinceService = 0;
        outOfServiceReason = "";
        raceReady = false;
        preparedSessionType = null;
    }

    @Override
    public boolean prepareForSession(SessionType sessionType) {
        if (sessionType == null) {
            throw new IllegalArgumentException("Session type is required.");
        }
        if (permanentlyDecommissioned || sessionActive || raceReady
                || !outOfServiceReason.isEmpty() || isDueForMaintenance()) {
            return false;
        }
        String setupDate = LocalDate.now(clock).toString();
        this.preparedSessionType = sessionType;
        this.raceReady = true;
        this.lastSetupDate = setupDate;
        return true;
    }

    public void startSession() {
        if (!isAvailableForRace()) {
            throw new IllegalStateException("Prepare an available car before starting a session.");
        }
        sessionActive = true;
        raceReady = false;
    }

    @Override
    public void completeSession() {
        if (activeStrategy != null) {
            throw new IllegalStateException("Complete the active strategy through its workflow.");
        }
        if (!sessionActive) {
            throw new IllegalStateException("There is no active session to complete.");
        }
        sessionActive = false;
        preparedSessionType = null;
        raceReady = false;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

    public SessionType getPreparedSessionType() {
        return preparedSessionType;
    }

    protected final void invalidatePreparation() {
        raceReady = false;
        preparedSessionType = null;
    }

    protected final void retirePermanently() {
        if (sessionActive || hasScheduledStrategy() || permanentlyDecommissioned) {
            throw new IllegalStateException("End commitments before permanently retiring the car.");
        }
        if (assignedDriver != null && !assignedDriver.getAssignedChassisIds().contains(chassisId)
                || assignedEngineer != null && !chassisId.equals(assignedEngineer.getAssignedChassisId())) {
            throw new IllegalStateException("Repair inconsistent assignments before retiring the car.");
        }
        if (assignedDriver != null) {
            assignedDriver.unassignFromCar(this);
        }
        if (assignedEngineer != null) {
            assignedEngineer.releaseFromCar();
        }
        permanentlyDecommissioned = true;
        outOfServiceReason = "Permanently decommissioned";
        invalidatePreparation();
    }

    @Override
    public boolean isAvailableForRace() {
        return raceReady && !sessionActive && !permanentlyDecommissioned
                && outOfServiceReason.isEmpty() && !isDueForMaintenance();
    }

    @Override
    public String getNextMaintenanceDue() {
        if (isDueForMaintenance()) {
            return "Maintenance required before the next session.";
        }
        return "Maintenance interval: " + Math.max(0, MAINTENANCE_INTERVAL_KM - mileageSinceService)
                + " km remaining.";
    }

    protected final double performanceBaseline(Circuit circuit, WeatherCondition weather) {
        if (circuit == null || weather == null) {
            throw new IllegalArgumentException("Circuit and weather are required for performance estimates.");
        }
        if (permanentlyDecommissioned || isDueForMaintenance() || !outOfServiceReason.isEmpty()) {
            throw new IllegalStateException("Performance estimates require a serviceable car.");
        }
        return circuit.estimateBaseLapTime();
    }

    protected static double weatherPenaltyFraction(WeatherCondition weather) {
        return switch (weather) {
            case CLEAR -> 0;
            case CLOUDY -> 0.01;
            case RAIN -> 0.08;
            case WET -> 0.15;
            case EXTREME_HEAT -> 0.03;
        };
    }

    protected final double aeroDeltaFraction(Circuit circuit, WeatherCondition weather) {
        if (currentAeroConfig == null || currentAeroConfig == AeroConfiguration.BALANCED) {
            return 0;
        }
        boolean wet = weather == WeatherCondition.RAIN || weather == WeatherCondition.WET;
        if (currentAeroConfig == AeroConfiguration.LOW_DRAG) {
            return circuit.hasLongStraights() && !wet ? -0.015 : 0.015;
        }
        return !circuit.hasLongStraights() || wet ? -0.02 : 0.015;
    }

    protected static double lapTimeDelta(double baseline, double fraction) {
        double delta = baseline * fraction;
        if (!Double.isFinite(delta) || !Double.isFinite(baseline + delta) || baseline + delta <= 0) {
            throw new IllegalArgumentException("Circuit estimate exceeds the supported lap-time range.");
        }
        return delta;
    }

    public void applyAeroUpgrade(String upgrade) {
        if (upgrade == null || upgrade.isBlank()) {
            throw new IllegalArgumentException("Upgrade is required.");
        }
        AeroConfiguration configuration;
        try {
            configuration = AeroConfiguration.valueOf(upgrade.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException(
                    "Choose BALANCED, LOW_DRAG, or HIGH_DOWNFORCE.");
        }
        if (sessionActive || hasScheduledStrategy() || permanentlyDecommissioned || isDueForMaintenance()
                || !outOfServiceReason.isEmpty()) {
            throw new IllegalStateException(
                    "Upgrades require an idle, serviceable, uncommitted car that has not been decommissioned.");
        }
        currentAeroConfig = configuration;
        raceReady = false;
        preparedSessionType = null;
    }

    public String getScheduledStrategy() {
        return activeStrategy == null ? scheduledStrategy : activeStrategy.getStrategyId();
    }

    public RaceStrategy getActiveStrategy() {
        return activeStrategy;
    }

    public Driver getAssignedDriver() {
        return assignedDriver;
    }

    public Engineer getAssignedEngineer() {
        return assignedEngineer;
    }

    public boolean hasActiveAssignments() {
        return assignedDriver != null || assignedEngineer != null;
    }

    public boolean hasScheduledStrategy() {
        return activeStrategy != null || scheduledStrategy != null && !scheduledStrategy.isBlank();
    }

    public boolean scheduleStrategy(RaceStrategy strategy) {
        if (strategy == null || strategy.getCar() != this) {
            throw new IllegalArgumentException("Strategy must belong to this car.");
        }
        return strategy.startStrategy();
    }

    final void beginStrategySession(RaceStrategy strategy) {
        if (strategy.getCar() != this || hasScheduledStrategy() || !isAvailableForRace()) {
            throw new IllegalStateException("Car is unavailable for this strategy.");
        }
        startSession();
        activeStrategy = strategy;
    }

    final void validateStrategyCompletion(RaceStrategy strategy, double distanceKm) {
        if (activeStrategy != strategy || !sessionActive || strategy.getCar() != this) {
            throw new IllegalStateException("This strategy does not own an active car session.");
        }
        if (!Double.isFinite(distanceKm) || distanceKm <= 0
                || !Double.isFinite(totalMileage + distanceKm)
                || !Double.isFinite(mileageSinceService + distanceKm)
                || totalMileage + distanceKm <= totalMileage
                || mileageSinceService + distanceKm <= mileageSinceService) {
            throw new IllegalArgumentException("Completed distance must fit the supported mileage range.");
        }
    }

    final void finishStrategySession(RaceStrategy strategy, double distanceKm) {
        validateStrategyCompletion(strategy, distanceKm);
        totalMileage += distanceKm;
        mileageSinceService += distanceKm;
        sessionActive = false;
        activeStrategy = null;
        scheduledStrategy = "";
        invalidatePreparation();
    }

    void assignDriver(Driver driver) {
        requireAssignable();
        if (driver == null || assignedDriver != null) {
            throw new IllegalStateException("Car requires a driver and must not already have one.");
        }
        assignedDriver = driver;
        invalidatePreparation();
    }

    void releaseDriver(Driver driver) {
        if (sessionActive || hasScheduledStrategy()) {
            throw new IllegalStateException("Cannot release a driver during a session or scheduled strategy.");
        }
        if (assignedDriver != driver) {
            throw new IllegalStateException("Driver is not assigned to this car.");
        }
        assignedDriver = null;
        invalidatePreparation();
    }

    void assignEngineer(Engineer engineer) {
        requireAssignable();
        if (engineer == null || assignedEngineer != null) {
            throw new IllegalStateException("Car requires an engineer and must not already have one.");
        }
        assignedEngineer = engineer;
        invalidatePreparation();
    }

    void releaseEngineer(Engineer engineer) {
        if (sessionActive || hasScheduledStrategy()) {
            throw new IllegalStateException("Cannot release an engineer during a session or scheduled strategy.");
        }
        if (assignedEngineer != engineer) {
            throw new IllegalStateException("Engineer is not assigned to this car.");
        }
        assignedEngineer = null;
        invalidatePreparation();
    }

    private void requireAssignable() {
        if (permanentlyDecommissioned || sessionActive || hasScheduledStrategy()
                || isDueForMaintenance() || !outOfServiceReason.isEmpty()) {
            throw new IllegalStateException("Assignments require an idle, serviceable, uncommitted car.");
        }
    }

    public void scheduleStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException("Strategy is required.");
        }
        if (permanentlyDecommissioned) {
            throw new IllegalArgumentException("Cannot schedule strategy for decommissioned car.");
        }
        if (hasScheduledStrategy() || sessionActive) {
            throw new IllegalStateException("Car already has a session or strategy commitment.");
        }
        this.scheduledStrategy = strategy.trim();
    }

}
