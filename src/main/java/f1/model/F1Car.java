package f1.model;

public abstract class F1Car {
    private final String chassisId;
    private final String teamName;
    private final String model;
    private final int year;
    private double totalMileage;
    private boolean raceReady;
    private String lastSetupDate;
    private String powerUnitType;
    private String currentAeroConfig;
    private boolean permanentlyDecommissioned;
    private String scheduledStrategy;
    private Driver assignedDriver;
    private Engineer assignedEngineer;

    protected F1Car(String chassisId, String teamName, String model, int year) {
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
        if (kilometers < 0) {
            throw new IllegalArgumentException("Mileage cannot be negative.");
        }
        this.totalMileage += kilometers;
        if (this.totalMileage > 500.0) {
            this.raceReady = false;
        }
    }

    public boolean prepareForSession(SessionType sessionType) {
        if (sessionType == null) {
            throw new IllegalArgumentException("Session type is required.");
        }
        if (permanentlyDecommissioned) {
            return false;
        }
        if (!raceReady) {
            return false;
        }
        if (totalMileage > 500.0) {
            return false;
        }
        this.raceReady = true;
        this.lastSetupDate = "today";
        return true;
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
