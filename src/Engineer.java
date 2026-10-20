public class Engineer {
    private final String employeeId;
    private final String name;
    private String specialty;
    private boolean isAvailable;

    public Engineer(String employeeId, String name, String specialty,
                    boolean isAvailable) {
        this.employeeId = requireText(employeeId, "Employee ID");
        this.name = requireText(name, "Engineer name");
        this.specialty = requireText(specialty, "Specialty");
        this.isAvailable = isAvailable;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.strip();
    }
}
