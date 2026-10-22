package test;

import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.SessionType;
import java.util.List;
import model.WeatherCondition;

public class CarPerformanceVerification {
    public static void main(String[] args) {
        Circuit straight = circuit("STRAIGHT", true);
        Circuit winding = circuit("WINDING", false);
        HybridRaceCar hybrid = new HybridRaceCar("H-PERF", "Team", "Hybrid", 2026, 8, 8, 350);
        ClassicRaceCar classic = new ClassicRaceCar("C-PERF", "Team", "Classic", 1980, 90, 2020, 300);
        List<F1Car> mixedCars = List.of(hybrid, classic);
        double[] deltas = new double[2];
        for (int i = 0; i < mixedCars.size(); i++) {
            F1Car car = mixedCars.get(i);
            deltas[i] = car.calculateLapTimeDelta(straight, WeatherCondition.CLEAR);
            check(Double.isFinite(deltas[i]) && straight.estimateBaseLapTime() + deltas[i] > 0,
                    "Polymorphic estimate produced an invalid lap time.");
            check(!car.getRegulatoryComplianceNotes().isBlank(), "Compliance report is empty.");
            check(car.calculateLapTimeDelta(straight, WeatherCondition.WET) > deltas[i],
                    "Wet weather did not affect performance.");
            expect(IllegalArgumentException.class,
                    () -> car.calculateLapTimeDelta(null, WeatherCondition.CLEAR));
            expect(IllegalArgumentException.class, () -> car.calculateLapTimeDelta(straight, null));
            check(car.getTotalMileage() == 0 && !car.isRaceReady(),
                    "Estimation changed mileage or readiness.");
        }
        check(deltas[0] < deltas[1], "Mixed car references did not use different power-unit behavior.");
        check(hybrid.calculateLapTimeDelta(straight, WeatherCondition.CLEAR)
                < hybrid.calculateLapTimeDelta(winding, WeatherCondition.CLEAR),
                "Hybrid boost did not account for circuit layout.");
        HybridRaceCar lowerPower = new HybridRaceCar("LOW-POWER", "Team", "Hybrid", 2026, 8, 8, 250);
        check(lowerPower.calculateLapTimeDelta(straight, WeatherCondition.CLEAR) > deltas[0],
                "Hybrid power did not affect estimates.");
        hybrid.applyAeroUpgrade("LOW_DRAG");
        check(hybrid.calculateLapTimeDelta(straight, WeatherCondition.CLEAR) < deltas[0],
                "Aero configuration did not affect estimates.");
        hybrid.applyAeroUpgrade("BALANCED");
        hybrid.prepareForSession(SessionType.RACE);
        hybrid.startSession();
        hybrid.deployEnergyBoost(4);
        double limited = hybrid.calculateLapTimeDelta(straight, WeatherCondition.CLEAR);
        check(limited > deltas[0], "The current lap's deployment allowance was ignored.");
        check(hybrid.getCurrentEnergyMJ() == 4 && hybrid.getDeployedThisLapMJ() == 4,
                "Estimation consumed energy or changed lap counters.");
        hybrid.completeLap();
        hybrid.startNextLap();
        check(hybrid.calculateLapTimeDelta(straight, WeatherCondition.CLEAR) < limited,
                "A new lap's allowance was not reflected in the estimate.");
        hybrid.completeSession();
        check(hybrid.getRegulatoryComplianceNotes().contains("350.0/350")
                && hybrid.getRegulatoryComplianceNotes().contains("specialist"),
                "Hybrid report does not expose limits and personnel requirements.");

        ClassicRaceCar powerful = new ClassicRaceCar("C-POWER", "Team", "Classic", 1980, 90, 2020, 600);
        check(powerful.calculateLapTimeDelta(straight, WeatherCondition.CLEAR) < deltas[1],
                "Classic power restrictions did not affect performance.");
        classic.updateOriginalityScore(10);
        double authentic = classic.calculateLapTimeDelta(straight, WeatherCondition.CLEAR);
        check(authentic < deltas[1], "Originality did not affect classic estimates.");
        classic.applyPreservationMode();
        check(classic.calculateLapTimeDelta(straight, WeatherCondition.CLEAR) > authentic
                && classic.getRegulatoryComplianceNotes().contains("practice demonstrations only"),
                "Preservation restrictions were ignored.");
        classic.scheduleRestoration(9999);
        expect(IllegalStateException.class,
                () -> classic.calculateLapTimeDelta(straight, WeatherCondition.CLEAR));
        classic.decommission();
        check(classic.getRegulatoryComplianceNotes().contains("permanently decommissioned"),
                "Retirement was missing from compliance notes.");
        expect(IllegalStateException.class,
                () -> classic.calculateLapTimeDelta(straight, WeatherCondition.CLEAR));
        hybrid.updateMileage(500);
        expect(IllegalStateException.class,
                () -> hybrid.calculateLapTimeDelta(straight, WeatherCondition.CLEAR));
        System.out.println("Car performance verification passed.");
    }

    private static Circuit circuit(String id, boolean straight) {
        return new Circuit(id, "Track", 5, 12, straight, WeatherCondition.CLEAR, "Location");
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
