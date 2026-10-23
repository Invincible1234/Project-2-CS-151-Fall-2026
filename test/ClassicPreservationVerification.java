package test;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;
import f1.model.ClassicRaceCar;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.SessionType;
import f1.model.Team;

public class ClassicPreservationVerification {
    public static void main(String[] args) {
        verifyPreservation();
        verifyTransfers();
        verifyRetirement();
        System.out.println("Classic preservation and ownership verification passed.");
    }

    private static void verifyPreservation() {
        ClassicRaceCar car = car("PRESERVE");
        car.applyAeroUpgrade("HIGH_DOWNFORCE");
        car.prepareForSession(SessionType.RACE);
        car.applyPreservationMode();
        check(car.isPreservationModeEnabled() && car.getPowerLimitKw() == 150
                && car.getCurrentAeroConfig().equals("BALANCED") && !car.isRaceReady(),
                "Preservation did not restrict power, aerodynamics, and preparation.");
        expect(IllegalStateException.class, car::applyPreservationMode);
        check(car.getPowerLimitKw() == 150, "Repeated preservation reduced power again.");
        check(!car.prepareForSession(SessionType.RACE)
                && !car.prepareForSession(SessionType.QUALIFYING), "Preservation allowed competition.");
        expect(IllegalStateException.class, () -> car.scheduleStrategy("Race"));
        expect(IllegalStateException.class, () -> car.applyAeroUpgrade("LOW_DRAG"));
        check(car.prepareForSession(SessionType.PRACTICE), "Preservation blocked demonstration practice.");
        car.startSession();
        expect(IllegalStateException.class, car::decommission);
        expect(IllegalStateException.class, () -> car.transferOwnershipRecord("Other"));
        check(car.isSessionActive() && !car.isPermanentlyDecommissioned(),
                "Rejected action interrupted practice or retired the car.");
        car.completeSession();
    }

    private static void verifyTransfers() {
        Team source = new Team("SOURCE", "Original", 2, 0);
        Team target = new Team("TARGET", "Destination", 1, 0);
        ClassicRaceCar car = car("TRANSFER");
        source.addCar(car);
        check(car.getRegisteredTeam() == source && car.getTeamName().equals("Original"),
                "Registration did not establish team ownership.");
        expect(IllegalStateException.class, () -> target.addCar(car));
        expect(IllegalStateException.class, () -> car.transferOwnershipRecord("Destination"));
        expect(IllegalArgumentException.class, () -> car.transferOwnershipRecord(source));
        ClassicRaceCar blocker = car("BLOCKER");
        target.addCar(blocker);
        String history = car.getOwnershipRecord();
        expect(CapacityExceededException.class, () -> car.transferOwnershipRecord(target));
        check(source.getCars().get(0) == car && target.getCars().get(0) == blocker
                && car.getRegisteredTeam() == source && car.getOwnershipRecord().equals(history),
                "Rejected transfer partially changed ownership.");
        target.removeCar(blocker);
        ClassicRaceCar duplicate = car("TRANSFER");
        target.addCar(duplicate);
        expect(DuplicateEntityException.class, () -> car.transferOwnershipRecord(target));
        check(car.getRegisteredTeam() == source, "Duplicate transfer changed source ownership.");
        target.removeCar(duplicate);
        check(car.transferOwnershipRecord(target), "Valid transfer failed.");
        check(source.getCars().isEmpty() && target.getCars().get(0) == car
                && car.getRegisteredTeam() == target && car.getTeamName().equals("Destination")
                && car.getOwnershipRecord().contains("-> Destination"),
                "Team lists and car ownership disagree after transfer.");
        check(target.removeCar(car) && car.getRegisteredTeam() == null,
                "Removal did not release registration.");
        car.transferOwnershipRecord("  Private Collection  ");
        check(car.getTeamName().equals("Private Collection"), "Unregistered ownership was not updated.");
        expect(IllegalArgumentException.class, () -> car.transferOwnershipRecord((String) null));
        expect(IllegalArgumentException.class, () -> car.transferOwnershipRecord(" "));
        expect(IllegalArgumentException.class, () -> car.transferOwnershipRecord("Private Collection"));
        ClassicRaceCar unrestored = new ClassicRaceCar("UNVERIFIED", "Original", "Classic", 1980, 0, 2020, 300);
        expect(IllegalStateException.class, () -> unrestored.transferOwnershipRecord("Other"));
    }

    private static void verifyRetirement() {
        ClassicRaceCar car = car("RETIRE");
        Driver driver = new Driver("D1", "Driver", "LICENSE", 30, "5551234567");
        driver.setMedicalClearance(true, "Cleared");
        driver.assignToCar(car);
        Engineer engineer = new Engineer("E1", "Engineer", f1.model.EngineerSpecialty.CLASSIC_RESTORATION, 5, "5551234567");
        engineer.assignToCar(car);
        expect(IllegalStateException.class, () -> car.transferOwnershipRecord("Other"));
        car.decommission();
        check(car.isPermanentlyDecommissioned() && !car.isRaceReady() && !car.hasActiveAssignments()
                && driver.getAssignedChassisIds().isEmpty() && engineer.isAvailable()
                && engineer.getAssignedChassisId().isEmpty(), "Retirement did not release personnel safely.");
        check(!car.prepareForSession(SessionType.PRACTICE), "Retired car became ready.");
        expect(IllegalStateException.class, car::decommission);
        expect(IllegalStateException.class, () -> car.performService("Revive"));
        expect(IllegalStateException.class, () -> car.updateMileage(1));
        expect(IllegalStateException.class, () -> car.scheduleRestoration(2026));
        expect(IllegalStateException.class, () -> car.updateOriginalityScore(1));
        expect(IllegalStateException.class, car::applyPreservationMode);
        expect(IllegalStateException.class, () -> car.transferOwnershipRecord("Other"));
        expect(IllegalStateException.class, () -> driver.assignToCar(car));
        expect(IllegalStateException.class, () -> engineer.assignToCar(car));
        check(car.isPermanentlyDecommissioned() && driver.getAssignedChassisIds().isEmpty()
                && engineer.isAvailable(), "Rejected revival changed retirement or assignment state.");
        ClassicRaceCar reserved = car("RESERVED");
        reserved.scheduleStrategy("Race");
        expect(IllegalStateException.class, reserved::decommission);
        check(!reserved.isPermanentlyDecommissioned(), "A reserved car was retired.");
        ClassicRaceCar restoring = car("RESTORING");
        Team workshop = new Team("WORKSHOP", "Original", 1, 0);
        workshop.addCar(restoring);
        restoring.scheduleRestoration(9999);
        expect(IllegalStateException.class, () -> workshop.removeCar(restoring));
        restoring.decommission();
        check(restoring.isPermanentlyDecommissioned() && !restoring.isRestorationInProgress()
                && restoring.getScheduledRestorationYear() == 0, "Retirement retained restoration work.");
        check(workshop.removeCar(restoring), "A retired car could not be removed from its team.");
    }

    private static ClassicRaceCar car(String id) {
        return new ClassicRaceCar(id, "Original", "Classic", 1980, 90, 2020, 300);
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
