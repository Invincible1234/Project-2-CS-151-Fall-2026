package f1.model;

import java.time.Clock;
import java.util.Locale;
import model.WeatherCondition;

public class ClassicRaceCar extends F1Car {
    private int originalityScore;
    private int restorationYear;
    private double powerLimitKw;
    private boolean restorationInProgress;
    private int scheduledRestorationYear;
    private boolean preservationModeEnabled;
    private String ownershipRecord;

    public ClassicRaceCar(String chassisId, String teamName, String model, int year,
            int originalityScore, int restorationYear, double powerLimitKw) {
        this(chassisId, teamName, model, year, originalityScore, restorationYear,
                powerLimitKw, Clock.systemDefaultZone());
    }

    protected ClassicRaceCar(String chassisId, String teamName, String model, int year,
            int originalityScore, int restorationYear, double powerLimitKw, Clock clock) {
        super(chassisId, teamName, model, year, clock);
        if (originalityScore < 0 || originalityScore > 100) {
            throw new IllegalArgumentException("Originality score must be between zero and 100.");
        }
        if (restorationYear < year || restorationYear > currentDate().getYear()) {
            throw new IllegalArgumentException("Completed restoration year must be between the car's year and today.");
        }
        if (!Double.isFinite(powerLimitKw) || powerLimitKw <= 0) {
            throw new IllegalArgumentException("Power limit must be finite and positive.");
        }
        this.originalityScore = originalityScore;
        this.restorationYear = restorationYear;
        this.powerLimitKw = powerLimitKw;
        this.ownershipRecord = getTeamName();
    }

    public int getOriginalityScore() {
        return originalityScore;
    }

    public int getRestorationYear() {
        return restorationYear;
    }

    public double getPowerLimitKw() {
        return powerLimitKw;
    }

    public boolean isPreservationModeEnabled() {
        return preservationModeEnabled;
    }

    public String getOwnershipRecord() {
        return ownershipRecord + " (current team: " + getTeamName() + ")";
    }

    @Override
    public double calculateLapTimeDelta(Circuit circuit, WeatherCondition weather) {
        double baseline = performanceBaseline(circuit, weather);
        double powerPenalty = 0.18 * (300.0 / (300.0 + powerLimitKw));
        double authenticityPenalty = (100 - originalityScore) / 100.0 * 0.03;
        double fraction = weatherPenaltyFraction(weather) * 1.25
                + aeroDeltaFraction(circuit, weather) + powerPenalty + authenticityPenalty
                + (preservationModeEnabled ? 0.04 : 0);
        return lapTimeDelta(baseline, fraction);
    }

    @Override
    public String getRegulatoryComplianceNotes() {
        return "Classic car; originality " + originalityScore + "/100; power limit "
                + powerLimitKw + " kW; hybrid energy deployment prohibited; "
                + (preservationModeEnabled ? "preservation: practice demonstrations only" : "standard classic operation")
                + (restorationInProgress ? "; unavailable during restoration" : "")
                + (isPermanentlyDecommissioned() ? "; permanently decommissioned" : "");
    }

    public void applyPreservationMode() {
        requireIdleClassicCar();
        if (preservationModeEnabled || restorationInProgress || isDueForMaintenance()
                || !getOutOfServiceReason().isEmpty()) {
            throw new IllegalStateException("Preservation mode requires a serviceable car not already in that mode.");
        }
        double reducedPower = powerLimitKw / 2;
        if (reducedPower <= 0) {
            throw new IllegalStateException("Power is too small to reduce for preservation mode.");
        }
        super.applyAeroUpgrade("BALANCED");
        powerLimitKw = reducedPower;
        preservationModeEnabled = true;
        invalidatePreparation();
    }

    public boolean transferOwnershipRecord(String newTeam) {
        if (newTeam == null || newTeam.isBlank()) {
            throw new IllegalArgumentException("A new team name is required.");
        }
        requireTransferable();
        String name = newTeam.strip();
        if (name.equals(getTeamName())) {
            throw new IllegalArgumentException("Choose a different owner.");
        }
        changeUnregisteredTeamName(name);
        ownershipRecord += " -> " + name;
        invalidatePreparation();
        return true;
    }

    public boolean transferOwnershipRecord(Team destination) {
        requireTransferable();
        Team source = getRegisteredTeam();
        if (source == null) {
            throw new IllegalStateException("Register the car with its source team before a team-to-team transfer.");
        }
        source.transferClassicCar(this, destination);
        ownershipRecord += " -> " + destination.getName();
        invalidatePreparation();
        return true;
    }

    public void decommission() {
        requireIdleClassicCar();
        retirePermanently();
        restorationInProgress = false;
        scheduledRestorationYear = 0;
    }

    public boolean isRestorationInProgress() {
        return restorationInProgress;
    }

    public int getScheduledRestorationYear() {
        return scheduledRestorationYear;
    }

    public void updateOriginalityScore(int delta) {
        requireIdleClassicCar();
        if (restorationInProgress) {
            throw new IllegalStateException("Record originality changes when completing restoration.");
        }
        int updatedScore = validateOriginalityChange(delta);
        originalityScore = updatedScore;
        invalidatePreparation();
    }

    public boolean scheduleRestoration(int year) {
        requireIdleClassicCar();
        if (restorationInProgress) {
            throw new IllegalStateException("A restoration is already scheduled.");
        }
        if (year < currentDate().getYear() || year < restorationYear || year > 9999) {
            throw new IllegalArgumentException("Schedule restoration for the current year or a later year through 9999.");
        }
        markOutOfService("Restoration scheduled for " + year);
        scheduledRestorationYear = year;
        restorationInProgress = true;
        return true;
    }

    public void completeRestoration(int originalityDelta, String serviceNotes) {
        requireIdleClassicCar();
        if (!restorationInProgress) {
            throw new IllegalStateException("There is no scheduled restoration to complete.");
        }
        int completedYear = currentDate().getYear();
        if (scheduledRestorationYear > completedYear) {
            throw new IllegalStateException("A future restoration cannot be completed yet.");
        }
        int updatedScore = validateOriginalityChange(originalityDelta);
        super.performService(serviceNotes);
        originalityScore = updatedScore;
        restorationYear = completedYear;
        scheduledRestorationYear = 0;
        restorationInProgress = false;
    }

    @Override
    public void performService(String notes) {
        if (restorationInProgress) {
            throw new IllegalStateException("Complete the restoration before returning to ordinary servicing.");
        }
        super.performService(notes);
    }

    @Override
    public boolean prepareForSession(SessionType sessionType) {
        if (sessionType == null) {
            throw new IllegalArgumentException("Session type is required.");
        }
        if (preservationModeEnabled && sessionType != SessionType.PRACTICE) {
            return false;
        }
        return !restorationInProgress && super.prepareForSession(sessionType);
    }

    @Override
    public boolean isAvailableForRace() {
        return !restorationInProgress && super.isAvailableForRace();
    }

    @Override
    public void scheduleStrategy(String strategy) {
        if (restorationInProgress || preservationModeEnabled) {
            throw new IllegalStateException("Cannot schedule a race strategy during restoration or preservation.");
        }
        super.scheduleStrategy(strategy);
    }

    @Override
    public void applyAeroUpgrade(String upgrade) {
        if (preservationModeEnabled && upgrade != null
                && !"BALANCED".equals(upgrade.strip().toUpperCase(Locale.ROOT))) {
            throw new IllegalStateException("Preserved classic cars require BALANCED aerodynamics.");
        }
        super.applyAeroUpgrade(upgrade);
    }

    private void requireTransferable() {
        requireIdleClassicCar();
        if (restorationInProgress || hasActiveAssignments() || originalityScore == 0) {
            throw new IllegalStateException("Transfers require verified originality and no restoration or assignments.");
        }
    }

    private int validateOriginalityChange(int delta) {
        long updatedScore = (long) originalityScore + delta;
        if (updatedScore < 0 || updatedScore > 100) {
            throw new IllegalArgumentException("Originality score must remain between zero and 100.");
        }
        return (int) updatedScore;
    }

    private void requireIdleClassicCar() {
        if (isSessionActive() || hasScheduledStrategy() || isPermanentlyDecommissioned()) {
            throw new IllegalStateException("Classic car changes require an idle, uncommitted car that is not retired.");
        }
    }
}
