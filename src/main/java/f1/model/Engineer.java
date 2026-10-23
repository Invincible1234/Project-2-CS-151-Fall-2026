package f1.model;

import java.util.Locale;

public class Engineer {
    private String employeeId;
    private String name;
    private String specialty;
    private boolean isAvailable;
    private String assignedChassisId;
    private int experienceYears;
    private String phone;
    private F1Car assignedCar;

    public Engineer(String employeeId, String name, String specialty, int experienceYears, String phone) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID is required.");
        }
        this.employeeId = employeeId.strip();
        if (specialty == null || specialty.isBlank()) {
            throw new IllegalArgumentException("Engineer specialty is required.");
        }
        this.specialty = specialty.strip().toUpperCase(Locale.ROOT);
        this.isAvailable = true;
        this.assignedChassisId = "";
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public String getAssignedChassisId() {
        return assignedChassisId;
    }

    public boolean assignToCar(F1Car car) {
        if (car == null) {
            throw new IllegalArgumentException("Car is required.");
        }
        if (!isAvailable || assignedCar != null) {
            throw new IllegalStateException("Engineer is already assigned or unavailable.");
        }
        if (!isQualifiedFor(car)) {
            throw new IllegalArgumentException("Engineer specialty is incompatible with this car.");
        }
        car.assignEngineer(this);
        assignedCar = car;
        assignedChassisId = car.getChassisId();
        isAvailable = false;
        return true;
    }

    public boolean isQualifiedFor(F1Car car) {
        return car instanceof HybridRaceCar && "HYBRID_POWER_UNIT".equals(specialty)
                || car instanceof ClassicRaceCar && "CLASSIC_RESTORATION".equals(specialty);
    }

    public boolean completeSetup(RaceStrategy strategy) { return false; }

    public boolean requestParts(String partType, int quantity) { return false; }

    public void releaseFromCar() {
        if (assignedCar != null) {
            assignedCar.releaseEngineer(this);
        }
        assignedCar = null;
        assignedChassisId = "";
        isAvailable = true;
    }

    public void updateSpecialty(String newSpecialty) { }

    public String getPhone(String role) { return null; }

    public String toString() { return ""; }

    public boolean equals(Object other) { return false; }

    public int hashCode() { return 0; }
}
