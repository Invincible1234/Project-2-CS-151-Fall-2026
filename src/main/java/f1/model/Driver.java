package f1.model;

import exception.CapacityExceededException;
import java.util.ArrayList;
import java.util.List;
import util.Constants;

public class Driver {
    private String driverId;
    private String name;
    private String licenseNumber;
    private int age;
    private final List<String> assignedChassisIds;
    private final List<F1Car> assignedCars;
    private int championshipPoints;
    private String preferredSetup;
    private String phone;
    private boolean medicalClearance;
    private boolean isBanned;
    private String banReason;
    private String medicalNotes;

    public Driver(String driverId, String name, String licenseNumber, int age, String phone) {
        if (licenseNumber == null || licenseNumber.isBlank()) {
            throw new IllegalArgumentException("License number is required.");
        }
        this.licenseNumber = licenseNumber.strip();
        this.assignedChassisIds = new ArrayList<>();
        this.assignedCars = new ArrayList<>();
        this.banReason = "";
        this.medicalNotes = "";
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public List<String> getAssignedChassisIds() {
        return List.copyOf(assignedChassisIds);
    }

    public boolean isEligibleForAssignment() {
        return medicalClearance && !isBanned && licenseNumber != null && !licenseNumber.isBlank();
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
                throw new IllegalStateException("Driver has a car with an active session or scheduled strategy.");
            }
        }
    }

    void releaseAllAssignments() {
        requireReleasableAssignments();
        for (F1Car car : List.copyOf(assignedCars)) {
            unassignFromCar(car);
        }
    }

    public boolean requestCarSetup(String preference) { return false; }

    public void completeRace(int finishingPosition, Circuit circuit) { }

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
        this.isBanned = true;
        this.banReason = reason.strip();
    }

    public void clearBan() {
        this.isBanned = false;
        this.banReason = "";
    }

    public void updateContact(String newPhone) { }

    public String getPhone(String role) { return null; }

    public boolean isMedicalClearance(String role) { return false; }

    public boolean isBanned(String role) { return false; }

    public String toString() { return ""; }

    public boolean equals(Object other) { return false; }

    public int hashCode() { return 0; }
}
