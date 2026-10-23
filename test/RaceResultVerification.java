package test;

import f1.model.RaceResult;
import f1.model.RaceResultStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;

public class RaceResultVerification {
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC);

    public static void main(String[] args) {
        RaceResult result = result(" R1 ", " RACE-1 ", " DRIVER-1 ", 1, " 2026-10-22 ");
        check(result.getResultId().equals("R1") && result.getRaceId().equals("RACE-1")
                && result.getDriverId().equals("DRIVER-1") && result.getDate().equals("2026-10-22"),
                "Result identity and date were not normalized.");
        check(result.getStatus() == RaceResultStatus.DRAFT && !result.isValidResult()
                && result.getPointsEarned() == 25, "A draft result has the wrong initial state.");
        expect(IllegalStateException.class, () -> result.correctFinishingPosition(2, "Too early"));
        expect(IllegalStateException.class, () -> result.voidResult("Too early"));
        for (int invalid : new int[] {-1, 0, 21, Integer.MAX_VALUE}) {
            expect(IllegalArgumentException.class, () -> result("BAD", "RACE", "DRIVER", invalid, "2026-10-22"));
        }
        for (String invalid : new String[] {null, "", " ", "2026-02-30", "22/10/2026", "2026-10-23"}) {
            expect(IllegalArgumentException.class, () -> result("BAD", "RACE", "DRIVER", 1, invalid));
        }
        expect(IllegalArgumentException.class, () -> result(" ", "RACE", "DRIVER", 1, "2026-10-22"));
        expect(IllegalArgumentException.class, () -> result("BAD", null, "DRIVER", 1, "2026-10-22"));
        expect(IllegalArgumentException.class, () -> result("BAD", "RACE", " ", 1, "2026-10-22"));
        HashSet<RaceResult> identities = new HashSet<>();
        identities.add(result);
        RaceResult sameId = result("R1", "OTHER-RACE", "OTHER-DRIVER", 20, "2026-10-21");
        check(!identities.add(sameId) && result.hashCode() == sameId.hashCode(),
                "Stable result identity was not respected.");
        RaceResult duplicateEntry = result("DIFFERENT-ID", "RACE-1", "DRIVER-1", 2, "2026-10-22");
        check(result.hasSameRaceEntry(duplicateEntry) && !result.equals(duplicateEntry),
                "A generated result ID bypassed functional race-entry comparison.");
        check(!result.hasSameRaceEntry(null) && !result.hasSameRaceEntry(sameId),
                "Unrelated race entries were treated as duplicates.");
        result.recordResult();
        check(result.isValidResult() && result.getStatus() == RaceResultStatus.RECORDED,
                "Recording did not validate the result.");
        expect(IllegalStateException.class, result::recordResult);
        for (int invalid : new int[] {0, 21, 1}) {
            expect(IllegalArgumentException.class, () -> result.correctFinishingPosition(invalid, "Correction"));
        }
        expect(IllegalArgumentException.class, () -> result.correctFinishingPosition(2, " "));
        expect(IllegalArgumentException.class, () -> result.voidResult(null));
        check(result.getFinishingPosition() == 1 && result.getPointsEarned() == 25
                && result.getCorrectionReason().isEmpty() && result.isValidResult(),
                "Rejected correction or void changed a valid result.");
        result.correctFinishingPosition(2, "  Timing correction  ");
        check(result.getPointsEarned() == 18 && result.getCorrectionReason().equals("Timing correction")
                && result.getFinishingPosition() == 2 && identities.contains(result),
                "Correction did not update points or changed stable identity.");
        result.correctFinishingPosition(11, "Penalty");
        check(result.getPointsEarned() == 0 && result.isValidResult(), "Non-scoring finish is invalid.");
        result.correctFinishingPosition(3, "Appeal accepted");
        check(result.getPointsEarned() == 15 && result.summarize().contains("position 3"),
                "Corrected result summary is stale.");
        result.voidResult("  Disqualified result  ");
        check(result.getStatus() == RaceResultStatus.VOIDED && !result.isValidResult()
                && result.getPointsEarned() == 0 && result.getVoidReason().equals("Disqualified result"),
                "Voiding did not preserve its reason or remove earned points.");
        expect(IllegalStateException.class, result::recordResult);
        expect(IllegalStateException.class, () -> result.correctFinishingPosition(1, "Revive"));
        expect(IllegalStateException.class, () -> result.voidResult("Repeat"));
        check(result.getPointsEarned() == 0 && result.toString().contains("VOIDED"),
                "A terminal result changed after rejection.");
        System.out.println("Race result verification passed.");
    }

    private static RaceResult result(String id, String race, String driver, int position, String date) {
        return new RaceResult(id, race, "CHASSIS", driver, "CIRCUIT", position, date, CLOCK);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void expect(Class<? extends RuntimeException> type, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException error) {
            if (type.isInstance(error)) {
                return;
            }
            throw new AssertionError("Unexpected exception type.", error);
        }
        throw new AssertionError("Expected " + type.getSimpleName());
    }
}
