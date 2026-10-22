package f1.model;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import util.InstanceLimit;
import util.Validation;

public final class RaceResult {
    private static final int[] FINISH_POINTS = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
    private final String resultId;
    private final String raceId;
    private final String chassisId;
    private final String driverId;
    private final String circuitId;
    private final LocalDate date;
    private int finishingPosition;
    private int pointsEarned;
    private RaceResultStatus status = RaceResultStatus.DRAFT;
    private String correctionReason = "";
    private String voidReason = "";

    public RaceResult(String resultId, String raceId, String chassisId, String driverId,
            String circuitId, int finishingPosition, String date) {
        this(resultId, raceId, chassisId, driverId, circuitId, finishingPosition, date,
                Clock.systemDefaultZone());
    }

    public RaceResult(String resultId, String raceId, String chassisId, String driverId,
            String circuitId, int finishingPosition, String date, Clock clock) {
        this.resultId = Validation.requireText(resultId, "Result ID");
        this.raceId = Validation.requireText(raceId, "Race ID");
        this.chassisId = Validation.requireText(chassisId, "Chassis ID");
        this.driverId = Validation.requireText(driverId, "Driver ID");
        this.circuitId = Validation.requireText(circuitId, "Circuit ID");
        this.finishingPosition = Validation.requirePosition(finishingPosition);
        if (clock == null) {
            throw new IllegalArgumentException("Clock is required.");
        }
        String dateText = Validation.requireText(date, "Race date");
        try {
            this.date = LocalDate.parse(dateText);
        } catch (DateTimeParseException error) {
            throw new IllegalArgumentException("Race date must be a valid ISO date such as 2026-10-22.");
        }
        if (this.date.isAfter(LocalDate.now(clock))) {
            throw new IllegalArgumentException("A race result cannot be dated in the future.");
        }
        this.pointsEarned = pointsFor(finishingPosition);
        InstanceLimit.checkAndCount(RaceResult.class);
    }

    public String getResultId() {
        return resultId;
    }

    public String getRaceId() {
        return raceId;
    }

    public String getChassisId() {
        return chassisId;
    }

    public String getDriverId() {
        return driverId;
    }

    public String getCircuitId() {
        return circuitId;
    }

    public String getDate() {
        return date.toString();
    }

    public int getFinishingPosition() {
        return finishingPosition;
    }

    public int getPointsEarned() {
        return pointsEarned;
    }

    public RaceResultStatus getStatus() {
        return status;
    }

    public String getCorrectionReason() {
        return correctionReason;
    }

    public String getVoidReason() {
        return voidReason;
    }

    public void recordResult() {
        if (status != RaceResultStatus.DRAFT) {
            throw new IllegalStateException("Only a draft result can be recorded.");
        }
        status = RaceResultStatus.RECORDED;
    }

    public void correctFinishingPosition(int position, String reason) {
        requireRecorded();
        int validatedPosition = Validation.requirePosition(position);
        String validatedReason = Validation.requireText(reason, "Correction reason");
        if (validatedPosition == finishingPosition) {
            throw new IllegalArgumentException("The correction must change the finishing position.");
        }
        finishingPosition = validatedPosition;
        pointsEarned = pointsFor(validatedPosition);
        correctionReason = validatedReason;
    }

    public void voidResult(String reason) {
        requireRecorded();
        String validatedReason = Validation.requireText(reason, "Void reason");
        voidReason = validatedReason;
        pointsEarned = 0;
        status = RaceResultStatus.VOIDED;
    }

    public boolean isValidResult() {
        return status == RaceResultStatus.RECORDED && pointsEarned == pointsFor(finishingPosition);
    }

    public boolean hasSameRaceEntry(RaceResult other) {
        return other != null && raceId.equals(other.raceId) && driverId.equals(other.driverId);
    }

    public String summarize() {
        return "Result " + resultId + ": race " + raceId + ", driver " + driverId
                + ", car " + chassisId + ", circuit " + circuitId + ", date " + date
                + ", position " + finishingPosition + ", points " + pointsEarned + ", " + status;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof RaceResult result && resultId.equals(result.resultId);
    }

    @Override
    public int hashCode() {
        return resultId.hashCode();
    }

    @Override
    public String toString() {
        return summarize();
    }

    private void requireRecorded() {
        if (status != RaceResultStatus.RECORDED) {
            throw new IllegalStateException("Only a recorded, non-voided result can be changed.");
        }
    }

    private static int pointsFor(int position) {
        return position <= FINISH_POINTS.length ? FINISH_POINTS[position - 1] : 0;
    }
}
