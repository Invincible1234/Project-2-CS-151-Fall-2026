package test;

import exception.CapacityExceededException;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import util.Constants;

public class PersonnelCapacityVerification {
    public static void main(String[] args) {
        for (int i = 0; i < Constants.MAXIMUM_INSTANCES; i++) {
            new Driver("D" + i, "Driver " + i, "LIC" + i, 25, "+123456789" + i);
        }
        try {
            new Driver("EXTRA", "Extra", "LICEXTRA", 25, "+12345678999");
            throw new AssertionError("The 101st driver was accepted.");
        } catch (CapacityExceededException expected) {
            System.out.println("Driver capacity verification passed.");
        }

        for (int i = 0; i < Constants.MAXIMUM_INSTANCES; i++) {
            new Engineer("E" + i, "Engineer " + i, EngineerSpecialty.AERODYNAMICS, 5, "+198765432" + i);
        }
        try {
            new Engineer("EXTRA", "Extra", EngineerSpecialty.AERODYNAMICS, 5, "+19876543299");
            throw new AssertionError("The 101st engineer was accepted.");
        } catch (CapacityExceededException expected) {
            System.out.println("Engineer capacity verification passed.");
        }

        System.out.println("All capacity verifications passed.");
    }
}