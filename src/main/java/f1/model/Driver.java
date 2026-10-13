package f1.model;

import java.util.List;

public class Driver {
    private String driverId;
    private String name;
    private String licenseNumber;
    private int age;
    private List<String> assignedChassisIds;
    private int championshipPoints;
    private String preferredSetup;
    private String phone;
    private boolean medicalClearance;
    private boolean isBanned;
    private String banReason;

    public Driver(String driverId, String name, String licenseNumber, int age, String phone) { }

    public boolean requestCarSetup(String preference) { return false; }

    public void completeRace(int finishingPosition, Circuit circuit) { }

    public void setMedicalClearance(boolean cleared, String notes) { }

    public void applyBan(String reason) { }

    public void clearBan() { }

    public void updateContact(String newPhone) { }

    public String getPhone(String role) { return null; }

    public boolean isMedicalClearance(String role) { return false; }

    public boolean isBanned(String role) { return false; }

    public String toString() { return ""; }

    public boolean equals(Object other) { return false; }

    public int hashCode() { return 0; }
}
