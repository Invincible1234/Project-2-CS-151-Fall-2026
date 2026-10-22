package f1.model;

public abstract class F1Car implements Raceable {
    private static final double MAINTENANCE_INTERVAL_KM = 500.0;
    private final String chassisId;
    private final String teamName;
    private final String model;
    private final int year;
    private double totalMileage;
    private boolean raceReady;
    private boolean sessionActive;
    private SessionType preparedSessionType;
    private String lastSetupDate;
    private String powerUnitType;
    private String currentAeroConfig;
    private boolean permanentlyDecommissioned;
    private String scheduledStrategy;

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
        if (this.totalMileage > MAINTENANCE_INTERVAL_KM) {
            this.raceReady = false;
        }
    }

    @Override
    public boolean prepareForSession(SessionType sessionType) {
        if (sessionType == null) {
            throw new IllegalArgumentException("Session type is required.");
        }
        if (permanentlyDecommissioned || sessionActive || raceReady
                || totalMileage > MAINTENANCE_INTERVAL_KM) {
            return false;
        }
        this.preparedSessionType = sessionType;
        this.raceReady = true;
        this.lastSetupDate = "today";
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
                && totalMileage <= MAINTENANCE_INTERVAL_KM;
    }

    @Override
    public String getNextMaintenanceDue() {
        if (totalMileage > MAINTENANCE_INTERVAL_KM) {
            return "Maintenance required before the next session.";
        }
        return "Maintenance interval: " + Math.max(0, MAINTENANCE_INTERVAL_KM - totalMileage)
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
