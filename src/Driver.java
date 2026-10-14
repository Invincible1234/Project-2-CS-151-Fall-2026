public class Driver {
    private final String driverId;
    private final String name;
    private final String licenseNumber;
    private boolean medicalClearance;
    private boolean isBanned;
    private String banReason;

    public Driver(String driverId, String name, String licenseNumber,
                  boolean medicalClearance, boolean isBanned, String banReason) {
        this.driverId = requireText(driverId, "Driver ID");
        this.name = requireText(name, "Driver name");
        this.licenseNumber = requireText(licenseNumber, "License number");

        String validatedReason = banReason == null ? "" : banReason.strip();
        if (isBanned && validatedReason.isEmpty()) {
            throw new IllegalArgumentException("A banned driver must have a ban reason.");
        }
        if (!isBanned && !validatedReason.isEmpty()) {
            throw new IllegalArgumentException("An unbanned driver cannot have a ban reason.");
        }

        this.medicalClearance = medicalClearance;
        this.isBanned = isBanned;
        this.banReason = validatedReason;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.strip();
    }
}
