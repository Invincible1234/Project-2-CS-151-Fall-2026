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
}
