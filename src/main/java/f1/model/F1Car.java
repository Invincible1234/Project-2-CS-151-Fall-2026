package f1.model;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;

public abstract class F1Car implements Raceable {
    private static final double MAINTENANCE_INTERVAL_KM = 500.0;
    private final String chassisId;
    private final String teamName;
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
    private String currentAeroConfig;
    private boolean permanentlyDecommissioned;
    private String scheduledStrategy;
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
        this.currentAeroConfig = "";
        this.permanentlyDecommissioned = false;
        this.scheduledStrategy = "";
    }

    public String getChassisId() {
        return chassisId;
    }

    public String getTeamName() {
        return teamName;
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

    public String getPowerUnitType() {
        return powerUnitType;
    }

    public String getCurrentAeroConfig() {
        return currentAeroConfig;
    }

    public boolean isPermanentlyDecommissioned() {
        return permanentlyDecommissioned;
    }

    public void updateMileage(double kilometers) {
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

    public void applyAeroUpgrade(String upgrade) {
        if (upgrade == null || upgrade.isBlank()) {
            throw new IllegalArgumentException("Upgrade is required.");
        }
        if (permanentlyDecommissioned) {
            throw new IllegalArgumentException("Cannot upgrade a decommissioned car.");
        }
        this.currentAeroConfig = upgrade.trim();
    }

    public String getScheduledStrategy() {
        return scheduledStrategy;
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
        return scheduledStrategy != null && !scheduledStrategy.isBlank();
    }

    void assignDriver(Driver driver) {
        requireAssignable();
        if (driver == null || assignedDriver != null) {
            throw new IllegalStateException("Car requires a driver and must not already have one.");
        }
        assignedDriver = driver;
    }

    void releaseDriver(Driver driver) {
        if (hasScheduledStrategy()) {
            throw new IllegalStateException("Cannot release a driver while a strategy is scheduled.");
        }
        if (assignedDriver != driver) {
            throw new IllegalStateException("Driver is not assigned to this car.");
        }
        assignedDriver = null;
    }

    void assignEngineer(Engineer engineer) {
        requireAssignable();
        if (engineer == null || assignedEngineer != null) {
            throw new IllegalStateException("Car requires an engineer and must not already have one.");
        }
        assignedEngineer = engineer;
    }

    void releaseEngineer(Engineer engineer) {
        if (hasScheduledStrategy()) {
            throw new IllegalStateException("Cannot release an engineer while a strategy is scheduled.");
        }
        if (assignedEngineer != engineer) {
            throw new IllegalStateException("Engineer is not assigned to this car.");
        }
        assignedEngineer = null;
    }

    private void requireAssignable() {
        if (permanentlyDecommissioned || hasScheduledStrategy()) {
            throw new IllegalStateException("Car is decommissioned or has a scheduled strategy.");
        }
    }

    public void scheduleStrategy(String strategy) {
        if (strategy == null || strategy.isBlank()) {
            throw new IllegalArgumentException("Strategy is required.");
        }
        if (permanentlyDecommissioned) {
            throw new IllegalArgumentException("Cannot schedule strategy for decommissioned car.");
        }
        this.scheduledStrategy = strategy.trim();
    }

    public void markOutOfService() {
        this.permanentlyDecommissioned = true;
        this.raceReady = false;
    }
}
