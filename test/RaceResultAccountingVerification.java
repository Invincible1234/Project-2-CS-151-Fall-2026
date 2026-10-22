package f1.model;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

public class RaceResultAccountingVerification {
    public static void main(String[] args) {
        RaceResult result = new RaceResult("ACCOUNT", "RACE", "CAR", "DRIVER", "CIRCUIT",
                1, "2026-10-22", Clock.fixed(Instant.parse("2026-10-22T12:00:00Z"), ZoneOffset.UTC));
        result.recordResult();
        expect(IllegalStateException.class, result::applyPointsToDriverAndTeam);
        int[] awarded = {0};
        boolean[] reject = {true};
        result.bindPointsAccount(delta -> {
            if (reject[0]) {
                throw new IllegalStateException("Rejected by the points account.");
            }
            awarded[0] += delta;
        });
        expect(IllegalStateException.class, result::applyPointsToDriverAndTeam);
        check(!result.arePointsApplied() && awarded[0] == 0, "Failed award changed result state.");
        reject[0] = false;
        result.applyPointsToDriverAndTeam();
        check(result.arePointsApplied() && awarded[0] == 25, "Initial award was incorrect.");
        expect(IllegalStateException.class, result::applyPointsToDriverAndTeam);
        expect(IllegalStateException.class, () -> result.bindPointsAccount(delta -> { }));
        reject[0] = true;
        expect(IllegalStateException.class, () -> result.correctFinishingPosition(2, "Rejected correction"));
        expect(IllegalStateException.class, () -> result.voidResult("Rejected void"));
        check(result.getFinishingPosition() == 1 && result.getPointsEarned() == 25
                && result.getCorrectionReason().isEmpty() && result.getVoidReason().isEmpty()
                && result.isValidResult() && awarded[0] == 25,
                "Failed accounting changed result history or status.");
        reject[0] = false;
        result.correctFinishingPosition(2, "Timing correction");
        check(awarded[0] == 18, "Correction applied more than the point difference.");
        result.correctFinishingPosition(11, "Penalty");
        check(awarded[0] == 0, "Non-scoring correction did not reconcile the award.");
        result.correctFinishingPosition(3, "Appeal");
        check(awarded[0] == 15, "Appeal did not update the accounted points.");
        result.voidResult("Disqualification");
        check(awarded[0] == 0 && !result.isValidResult(), "Void did not reverse applied points.");
        expect(IllegalStateException.class, () -> result.voidResult("Repeat"));
        check(awarded[0] == 0, "Repeated void reversed points twice.");
        System.out.println("Race result accounting verification passed.");
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
