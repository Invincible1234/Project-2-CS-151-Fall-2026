package f1.model;

import exception.CapacityExceededException;
import java.util.ArrayList;
import java.util.List;
import util.AccessContext;
import util.AccessPolicy;
import util.Constants;
import util.Validation;

public class Driver {
    private final String driverId;
    private final String name;
    private final String licenseNumber;
    private final int age;
    private final List<String> assignedChassisIds;
    private final List<F1Car> assignedCars;
    private int championshipPoints;
    private String preferredSetup;
    private String phone;
    private String medicalNotes;
    private boolean medicalClearance;
    private boolean banned;
    private String banReason;

    public Driver(String driverId, String name, String licenseNumber, int age, String phone) {
        this.driverId = Validation.requireText(driverId, "Driver ID");
        this.name = Validation.requireText(name, "Driver name");
        this.licenseNumber = Validation.requireText(licenseNumber, "License number");
        this.age = Validation.requireAge(age);
        this.phone = Validation.requirePhone(phone);
        this.assignedChassisIds = new ArrayList<>();
        this.assignedCars = new ArrayList<>();
        this.championshipPoints = 0;
        this.preferredSetup = "";
        this.medicalNotes = "";
        this.medicalClearance = false;
        this.banned = false;
        this.banReason = requireBanPairing(false, "");
    }

    public String getDriverId() {
        return driverId;
    }

    public String getName() {
        return name;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public int getAge() {
        return age;
    }

    public int getChampionshipPoints() {
        return championshipPoints;
    }

    public String getPreferredSetup() {
        return preferredSetup;
    }

    public List<String> getAssignedChassisIds() {
        return List.copyOf(assignedChassisIds);
    }

    public String phoneFor(AccessContext caller) {
        return AccessPolicy.visiblePhone(caller, driverId, phone);
    }

    public String medicalNotesFor(AccessContext caller) {
        return AccessPolicy.visibleMedicalNotes(caller, driverId, medicalNotes);
    }

    public String banReasonFor(AccessContext caller) {
        return AccessPolicy.visibleBanReason(caller, driverId, banReason);
    }

    public boolean isEligibleForAssignment() {
        return medicalClearance && !banned && !licenseNumber.isBlank();
    }

    public void assignToCar(F1Car car) {
        if (car == null) {
            throw new IllegalArgumentException("Car is required.");
        }
        if (!isEligibleForAssignment()) {
            throw new IllegalStateException("Driver is not eligible for assignment.");
        }
        if (assignedChassisIds.contains(car.getChassisId())) {
            throw new IllegalStateException("Driver is already assigned to this chassis.");
        }
        requireReleasableAssignments();
        if (assignedCars.size() >= Constants.MAXIMUM_INSTANCES) {
            throw new CapacityExceededException("Driver assignment capacity has been reached.");
        }
        car.assignDriver(this);
        assignedCars.add(car);
        assignedChassisIds.add(car.getChassisId());
    }

    public void unassignFromCar(F1Car car) {
        if (car == null) {
            throw new IllegalArgumentException("Car is required.");
        }
        int index = assignedChassisIds.indexOf(car.getChassisId());
        if (index < 0 || assignedCars.get(index) != car) {
            throw new IllegalArgumentException("Driver is not assigned to this car.");
        }
        car.releaseDriver(this);
        assignedCars.remove(index);
        assignedChassisIds.remove(index);
    }

    void requireReleasableAssignments() {
        for (F1Car car : assignedCars) {
            if (car.isSessionActive() || car.hasScheduledStrategy()) {
                throw new IllegalStateException(
                        "Driver has a car with an active session or scheduled strategy.");
            }
        }
    }

    void releaseAllAssignments() {
        requireReleasableAssignments();
        for (F1Car car : List.copyOf(assignedCars)) {
            unassignFromCar(car);
        }
    }

    public boolean requestCarSetup(String preference) {
        this.preferredSetup = Validation.requireText(preference, "Setup preference");
        return true;
    }

    public void setMedicalClearance(boolean cleared, String notes) {
        if (!cleared) {
            releaseAllAssignments();
        }
        this.medicalClearance = cleared;
        this.medicalNotes = notes == null ? "" : notes.strip();
    }

    public void applyBan(String reason) {
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("Ban reason is required.");
        }
        releaseAllAssignments();
        this.banned = true;
        this.banReason = requireBanPairing(true, reason);
    }

    public void clearBan() {
        this.banned = false;
        this.banReason = requireBanPairing(false, "");
    }

    public void updateContact(String newPhone) {
        this.phone = Validation.requirePhone(newPhone);
    }

    public void completeRace(int finishingPosition, Circuit circuit) {
        if (circuit == null) {
            throw new IllegalArgumentException("Circuit is required.");
        }
        int position = Validation.requirePosition(finishingPosition);
        if (banned) {
            throw new IllegalArgumentException("A banned driver cannot complete a race.");
        }
        championshipPoints += pointsFor(position);
    }

    private static int pointsFor(int position) {
        return switch (position) {
            case 1 -> 25;
            case 2 -> 18;
            case 3 -> 15;
            case 4 -> 12;
            case 5 -> 10;
            case 6 -> 8;
            case 7 -> 6;
            case 8 -> 4;
            case 9 -> 2;
            case 10 -> 1;
            default -> 0;
        };
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Driver driver
                && licenseNumber.equals(driver.licenseNumber);
    }

    @Override
    public int hashCode() {
        return licenseNumber.hashCode();
    }

    @Override
    public String toString() {
        return "Driver{driverId='" + driverId + "', name='" + name
                + "', licenseNumber='" + licenseNumber + "', age=" + age
                + ", championshipPoints=" + championshipPoints
                + ", assignments=" + assignedChassisIds.size() + "}";
    }

    private static String requireBanPairing(boolean banned, String banReason) {
        String validatedReason = banReason == null ? "" : banReason.strip();
        if (banned && validatedReason.isEmpty()) {
            throw new IllegalArgumentException("A banned driver must have a ban reason.");
        }
        if (!banned && !validatedReason.isEmpty()) {
            throw new IllegalArgumentException("An unbanned driver cannot have a ban reason.");
        }
        return validatedReason;
    }
}
