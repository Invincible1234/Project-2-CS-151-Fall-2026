package f1.model;

import java.time.Clock;

public class ClassicRaceCar extends F1Car {
    private int originalityScore;
    private int restorationYear;
    private double powerLimitKw;
    private boolean restorationInProgress;
    private int scheduledRestorationYear;

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
        return !restorationInProgress && super.prepareForSession(sessionType);
    }

    @Override
    public boolean isAvailableForRace() {
        return !restorationInProgress && super.isAvailableForRace();
    }

    @Override
    public void scheduleStrategy(String strategy) {
        if (restorationInProgress) {
            throw new IllegalStateException("Cannot schedule a race strategy during restoration.");
        }
        super.scheduleStrategy(strategy);
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
