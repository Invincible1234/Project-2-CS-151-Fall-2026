package f1.model;

import java.util.ArrayList;
import java.util.List;
import util.InstanceLimit;
import util.Validation;

public class Driver {
    private final String driverId;
    private final String name;
    private final String licenseNumber;
    private final int age;
    private final List<String> assignedChassisIds;
    private int championshipPoints;
    private String preferredSetup;
    private String phone;
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
        this.championshipPoints = 0;
        this.preferredSetup = "";
        this.medicalClearance = false;
        this.banned = false;
        this.banReason = requireBanPairing(false, "");
        InstanceLimit.checkAndCount(Driver.class);
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
