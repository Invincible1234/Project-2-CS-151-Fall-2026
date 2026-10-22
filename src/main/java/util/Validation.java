package util;

public final class Validation {
    private static final String PHONE_PATTERN = "\\+?\\d{10,15}";

    private Validation() {
    }

    public static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " cannot be blank.");
        }
        return value.strip();
    }

    public static String requirePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Phone number cannot be blank.");
        }
        String stripped = phone.strip();
        if (!stripped.matches(PHONE_PATTERN)) {
            throw new IllegalArgumentException(
                    "Phone number must be 10 to 15 digits and may start with +.");
        }
        return stripped;
    }

    public static int requireAge(int age) {
        if (age < 18 || age > 99) {
            throw new IllegalArgumentException("Driver age must be from 18 through 99.");
        }
        return age;
    }

    public static int requireExperienceYears(int years) {
        if (years < 0 || years > 60) {
            throw new IllegalArgumentException("Experience years must be from 0 through 60.");
        }
        return years;
    }

    public static int requirePosition(int position) {
        if (position < 1 || position > 20) {
            throw new IllegalArgumentException("Finishing position must be from 1 through 20.");
        }
        return position;
    }
}
