package f1.model;

import util.Validation;

public class Engineer {
    private final String employeeId;
    private final String name;
    private EngineerSpecialty specialty;
    private boolean isAvailable;
    private String assignedChassisId;
    private final int experienceYears;
    private String phone;
    private F1Car assignedCar;

    public Engineer(String employeeId, String name, EngineerSpecialty specialty,
            int experienceYears, String phone) {
        this.employeeId = Validation.requireText(employeeId, "Employee ID");
        this.name = Validation.requireText(name, "Engineer name");
        if (specialty == null) {
            throw new IllegalArgumentException("Engineer specialty is required.");
        }
        this.specialty = specialty;
        this.experienceYears = Validation.requireExperienceYears(experienceYears);
        this.phone = Validation.requirePhone(phone);
        this.isAvailable = true;
        this.assignedChassisId = "";
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getName() {
        return name;
    }

    public EngineerSpecialty getSpecialty() {
        return specialty;
    }

    public int getExperienceYears() {
        return experienceYears;
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
        boolean compatible = car instanceof HybridRaceCar
                && specialty == EngineerSpecialty.HYBRID_POWER_UNIT
                || car instanceof ClassicRaceCar
                && specialty == EngineerSpecialty.CLASSIC_RESTORATION;
        if (!compatible) {
            throw new IllegalArgumentException("Engineer specialty is incompatible with this car.");
        }
        car.assignEngineer(this);
        assignedCar = car;
        assignedChassisId = car.getChassisId();
        isAvailable = false;
        return true;
    }

    public boolean completeSetup(RaceStrategy strategy) {
        return false;
    }

    public boolean requestParts(String partType, int quantity) {
        return false;
    }

    public void releaseFromCar() {
        if (assignedCar != null) {
            assignedCar.releaseEngineer(this);
        }
        assignedCar = null;
        assignedChassisId = "";
        isAvailable = true;
    }

    public void updateSpecialty(EngineerSpecialty newSpecialty) {
    }

    public String getPhone(String role) {
        return null;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Engineer engineer
                && employeeId.equals(engineer.employeeId);
    }

    @Override
    public int hashCode() {
        return employeeId.hashCode();
    }

    @Override
    public String toString() {
        return "Engineer{employeeId='" + employeeId + "', name='" + name
                + "', specialty=" + specialty + ", experienceYears=" + experienceYears
                + ", available=" + isAvailable + ", assignedChassisId='" + assignedChassisId + "'}";
    }
}
