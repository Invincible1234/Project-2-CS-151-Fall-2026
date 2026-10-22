package f1.model;

public class Engineer {
    private String employeeId;
    private String name;
    private String specialty;
    private boolean isAvailable;
    private String assignedChassisId;
    private int experienceYears;
    private String phone;

    public Engineer(String employeeId, String name, String specialty, int experienceYears, String phone) {
        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException("Employee ID is required.");
        }
        this.employeeId = employeeId.strip();
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public boolean assignToCar(F1Car car) { return false; }

    public boolean completeSetup(RaceStrategy strategy) { return false; }

    public boolean requestParts(String partType, int quantity) { return false; }

    public void releaseFromCar() { }

    public void updateSpecialty(String newSpecialty) { }

    public String getPhone(String role) { return null; }

    public String toString() { return ""; }

    public boolean equals(Object other) { return false; }

    public int hashCode() { return 0; }
}
