package test;

import f1.model.ClassicRaceCar;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.SessionType;
import java.util.List;

public class CarUpgradeVerification {
    public static void main(String[] args) {
        for (F1Car car : List.of(
                new HybridRaceCar("H3", "Team", "Hybrid", 2026, 4, 3),
                new ClassicRaceCar("C3", "Team", "Classic", 1980, 90, 2020, 300))) {
            for (String configuration : List.of("BALANCED", "LOW_DRAG", "HIGH_DOWNFORCE")) {
                check(car.prepareForSession(SessionType.PRACTICE), "Preparation failed.");
                car.applyAeroUpgrade(configuration);
                check(car.getCurrentAeroConfig().equals(configuration), "Upgrade was not saved.");
                check(!car.isRaceReady() && car.getPreparedSessionType() == null,
                        "An upgrade must invalidate the previous setup.");
            }
            car.applyAeroUpgrade("  low_drag  ");
            check(car.getCurrentAeroConfig().equals("LOW_DRAG"), "Input was not normalized.");
            check(car.prepareForSession(SessionType.RACE), "Fresh preparation failed.");
            for (String invalid : new String[] {null, "", "  ", "UNKNOWN", "BOOST"}) {
                expect(IllegalArgumentException.class, () -> car.applyAeroUpgrade(invalid));
                check(car.getCurrentAeroConfig().equals("LOW_DRAG") && car.isRaceReady()
                        && car.getPreparedSessionType() == SessionType.RACE,
                        "Rejected upgrade changed a valid setup.");
            }
            car.startSession();
            expect(IllegalStateException.class, () -> car.applyAeroUpgrade("BALANCED"));
            check(car.isSessionActive() && car.getCurrentAeroConfig().equals("LOW_DRAG"),
                    "Rejected upgrade changed an active car.");
            car.completeSession();

            car.markOutOfService("Damaged front wing");
            expect(IllegalStateException.class, () -> car.applyAeroUpgrade("BALANCED"));
            check(!car.isPermanentlyDecommissioned(), "A repairable fault retired the car.");
            check(car.getOutOfServiceReason().equals("Damaged front wing"),
                    "Rejected upgrade cleared a fault.");
            car.performService("Replaced damaged wing");
            car.applyAeroUpgrade("BALANCED");
            check(car.getCurrentAeroConfig().equals("BALANCED"), "Repair did not allow upgrades.");

            car.updateMileage(500);
            expect(IllegalStateException.class, () -> car.applyAeroUpgrade("HIGH_DOWNFORCE"));
            check(car.isDueForMaintenance() && car.getCurrentAeroConfig().equals("BALANCED"),
                    "Upgrade bypassed maintenance or changed configuration on failure.");
        }
        System.out.println("Car upgrade verification passed for both car types.");
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
