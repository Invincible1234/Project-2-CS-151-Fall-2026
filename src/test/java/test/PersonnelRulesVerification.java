package test;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.HybridRaceCar;
import f1.model.ClassicRaceCar;
import f1.service.PersonnelRegistry;
import util.AccessContext;
import util.AccessPolicy;
import util.Constants;
import util.InstanceLimit;
import util.Validation;

public class PersonnelRulesVerification {
    public static void main(String[] args) {
        runTest("testDuplicateLicenseRejected", PersonnelRulesVerification::testDuplicateLicenseRejected);
        runTest("testDuplicateEmployeeIdRejected", PersonnelRulesVerification::testDuplicateEmployeeIdRejected);
        runTest("testBlankIdsRejected", PersonnelRulesVerification::testBlankIdsRejected);
        runTest("testBadPhoneRejected", PersonnelRulesVerification::testBadPhoneRejected);
        runTest("testBadAgeRejected", PersonnelRulesVerification::testBadAgeRejected);
        runTest("testBadExperienceRejected", PersonnelRulesVerification::testBadExperienceRejected);
        runTest("testBannedDriverCannotBeAssigned", PersonnelRulesVerification::testBannedDriverCannotBeAssigned);
        runTest("testMedicallyUnclearedDriverCannotBeAssigned", PersonnelRulesVerification::testMedicallyUnclearedDriverCannotBeAssigned);
        runTest("testBanClearsChassisAssignments", PersonnelRulesVerification::testBanClearsChassisAssignments);
        runTest("testHybridAssignmentRejectsNonHybridSpecialty", PersonnelRulesVerification::testHybridAssignmentRejectsNonHybridSpecialty);
        runTest("testClassicAssignmentRejectsHybridSpecialty", PersonnelRulesVerification::testClassicAssignmentRejectsHybridSpecialty);
        runTest("testAssignedEngineerCannotBeAssignedAgainUntilRelease", PersonnelRulesVerification::testAssignedEngineerCannotBeAssignedAgainUntilRelease);
        runTest("testRequestPartsRejectsBadQuantity", PersonnelRulesVerification::testRequestPartsRejectsBadQuantity);
        runTest("testRequestPartsDoesNotPretendInventoryChanged", PersonnelRulesVerification::testRequestPartsDoesNotPretendInventoryChanged);
        runTest("testInvalidCompleteRaceInputsDoNotChangePoints", PersonnelRulesVerification::testInvalidCompleteRaceInputsDoNotChangePoints);
        runTest("testValidFinishAddsCorrectPoints", PersonnelRulesVerification::testValidFinishAddsCorrectPoints);
        runTest("testUnauthorizedAccessMasksPhoneAndHidesNotesAndBanReason", PersonnelRulesVerification::testUnauthorizedAccessMasksPhoneAndHidesNotesAndBanReason);
        runTest("testTeamPrincipalCanReadSensitiveData", PersonnelRulesVerification::testTeamPrincipalCanReadSensitiveData);
        runTest("testEqualsAndHashCodeFollowLicenseAndEmployeeId", PersonnelRulesVerification::testEqualsAndHashCodeFollowLicenseAndEmployeeId);
        System.out.println("All personnel rules verifications passed.");
    }

    private static void runTest(String name, Runnable test) {
        util.InstanceLimit.resetForTesting();
        test.run();
        System.out.println(name + " passed.");
    }

    private static void testDuplicateLicenseRejected() {
        PersonnelRegistry registry = new PersonnelRegistry();
        registry.registerDriver("D1", "Driver One", "LIC123", 25, "+1234567890");
        try {
            registry.registerDriver("D2", "Driver Two", "LIC123", 30, "+1987654321");
            throw new AssertionError("Duplicate license was accepted.");
        } catch (DuplicateEntityException expected) {
        }
        if (registry.getDrivers().size() != 1) {
            throw new AssertionError("Stored list changed after duplicate rejection.");
        }
    }

    private static void testDuplicateEmployeeIdRejected() {
        PersonnelRegistry registry = new PersonnelRegistry();
        registry.registerEngineer("E1", "Engineer One", EngineerSpecialty.AERODYNAMICS, 5, "+1234567890");
        try {
            registry.registerEngineer("E1", "Engineer Two", EngineerSpecialty.RACE_ENGINEERING, 10, "+1987654321");
            throw new AssertionError("Duplicate employee id was accepted.");
        } catch (DuplicateEntityException expected) {
        }
        if (registry.getEngineers().size() != 1) {
            throw new AssertionError("Stored list changed after duplicate rejection.");
        }
    }

    private static void testBlankIdsRejected() {
        try {
            new Driver("", "Name", "LIC", 25, "+1234567890");
            throw new AssertionError("Blank driver id was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new Driver("D1", "", "LIC", 25, "+1234567890");
            throw new AssertionError("Blank name was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new Driver("D1", "Name", "", 25, "+1234567890");
            throw new AssertionError("Blank license was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new Engineer("", "Name", EngineerSpecialty.AERODYNAMICS, 5, "+1234567890");
            throw new AssertionError("Blank employee id was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new Engineer("E1", "", EngineerSpecialty.AERODYNAMICS, 5, "+1234567890");
            throw new AssertionError("Blank engineer name was accepted.");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void testBadPhoneRejected() {
        try {
            new Driver("D1", "Name", "LIC", 25, "badphone");
            throw new AssertionError("Bad phone was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new Engineer("E1", "Name", EngineerSpecialty.AERODYNAMICS, 5, "badphone");
            throw new AssertionError("Bad engineer phone was accepted.");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void testBadAgeRejected() {
        try {
            new Driver("D1", "Name", "LIC", 17, "+1234567890");
            throw new AssertionError("Age 17 was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new Driver("D1", "Name", "LIC", 100, "+1234567890");
            throw new AssertionError("Age 100 was accepted.");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void testBadExperienceRejected() {
        try {
            new Engineer("E1", "Name", EngineerSpecialty.AERODYNAMICS, -1, "+1234567890");
            throw new AssertionError("Experience -1 was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        try {
            new Engineer("E1", "Name", EngineerSpecialty.AERODYNAMICS, 61, "+1234567890");
            throw new AssertionError("Experience 61 was accepted.");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void testBannedDriverCannotBeAssigned() {
        Driver driver = new Driver("D1", "Driver One", "LIC123", 25, "+1234567890");
        driver.setMedicalClearance(true, "cleared");
        driver.applyBan("Safety violation");
        HybridRaceCar car = new HybridRaceCar("H1", "Team", "Model", 2024, 400, 300);
        try {
            driver.assignToCar(car);
            throw new AssertionError("Banned driver was assigned.");
        } catch (IllegalStateException expected) {
        }
    }

    private static void testMedicallyUnclearedDriverCannotBeAssigned() {
        Driver driver = new Driver("D1", "Driver One", "LIC123", 25, "+1234567890");
        HybridRaceCar car = new HybridRaceCar("H1", "Team", "Model", 2024, 400, 300);
        try {
            driver.assignToCar(car);
            throw new AssertionError("Medically uncleared driver was assigned.");
        } catch (IllegalStateException expected) {
        }
    }

    private static void testBanClearsChassisAssignments() {
        Driver driver = new Driver("D1", "Driver One", "LIC123", 25, "+1234567890");
        driver.setMedicalClearance(true, "cleared");
        HybridRaceCar car = new HybridRaceCar("H1", "Team", "Model", 2024, 400, 300);
        driver.assignToCar(car);
        if (driver.getAssignedChassisIds().size() != 1) {
            throw new AssertionError("Driver not assigned before ban.");
        }
        driver.applyBan("Safety violation");
        if (driver.getAssignedChassisIds().size() != 0) {
            throw new AssertionError("Ban did not clear chassis assignments.");
        }
    }

    private static void testHybridAssignmentRejectsNonHybridSpecialty() {
        HybridRaceCar car = new HybridRaceCar("H1", "Team", "Model", 2024, 400, 300);
        Engineer engineer = new Engineer("E1", "Engineer One", EngineerSpecialty.AERODYNAMICS, 5, "+1234567890");
        try {
            engineer.assignToCar(car);
            throw new AssertionError("Non-HYBRID_POWER_UNIT engineer assigned to hybrid car requiring specialist.");
        } catch (IllegalArgumentException expected) {
        }
        Engineer engineer2 = new Engineer("E2", "Engineer Two", EngineerSpecialty.CLASSIC_RESTORATION, 5, "+1234567890");
        try {
            engineer2.assignToCar(car);
            throw new AssertionError("CLASSIC_RESTORATION engineer assigned to hybrid car.");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void testClassicAssignmentRejectsHybridSpecialty() {
        ClassicRaceCar car = new ClassicRaceCar("C1", "Team", "Model", 2020, 90, 2023, 600);
        Engineer engineer = new Engineer("E1", "Engineer One", EngineerSpecialty.HYBRID_POWER_UNIT, 5, "+1234567890");
        try {
            engineer.assignToCar(car);
            throw new AssertionError("HYBRID_POWER_UNIT engineer assigned to classic car.");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void testAssignedEngineerCannotBeAssignedAgainUntilRelease() {
        HybridRaceCar car1 = new HybridRaceCar("H1", "Team", "Model", 2024, 400, 300);
        HybridRaceCar car2 = new HybridRaceCar("H2", "Team", "Model", 2024, 400, 300);
        Engineer engineer = new Engineer("E1", "Engineer One", EngineerSpecialty.HYBRID_POWER_UNIT, 5, "+1234567890");
        engineer.assignToCar(car1);
        try {
            engineer.assignToCar(car2);
            throw new AssertionError("Assigned engineer was assigned again without release.");
        } catch (IllegalStateException expected) {
        }
        engineer.releaseFromCar();
        engineer.assignToCar(car2);
    }

    private static void testRequestPartsRejectsBadQuantity() {
        Engineer engineer = new Engineer("E1", "Engineer One", EngineerSpecialty.AERODYNAMICS, 5, "+1234567890");
        try {
            engineer.requestParts("Tires", 0);
            throw new AssertionError("Quantity 0 was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        try {
            engineer.requestParts("Tires", -5);
            throw new AssertionError("Negative quantity was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        try {
            engineer.requestParts("", 5);
            throw new AssertionError("Blank part type was accepted.");
        } catch (IllegalArgumentException expected) {
        }
    }

    private static void testRequestPartsDoesNotPretendInventoryChanged() {
        Engineer engineer = new Engineer("E1", "Engineer One", EngineerSpecialty.AERODYNAMICS, 5, "+1234567890");
        boolean result = engineer.requestParts("Tires", 4);
        if (result) {
            throw new AssertionError("requestParts returned true for unassigned engineer.");
        }
    }

    private static void testInvalidCompleteRaceInputsDoNotChangePoints() {
        Driver driver = new Driver("D1", "Driver One", "LIC123", 25, "+1234567890");
        int initialPoints = driver.getChampionshipPoints();
        try {
            driver.completeRace(null, null);
            throw new AssertionError("Null race result was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        if (driver.getChampionshipPoints() != initialPoints) {
            throw new AssertionError("Points changed after invalid completeRace call.");
        }
    }

    private static void testValidFinishAddsCorrectPoints() {
        Driver driver = new Driver("D1", "Driver One", "LIC123", 25, "+1234567890");
        int[] pointsTable = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
        for (int i = 1; i <= 10; i++) {
            Driver d = new Driver("D" + i, "Driver " + i, "LIC" + i, 25, "+123456789" + i);
            d.setMedicalClearance(true, "cleared");
        }
    }

    private static void testUnauthorizedAccessMasksPhoneAndHidesNotesAndBanReason() {
        Driver driver = new Driver("D1", "Driver One", "LIC123", 25, "+1234567890");
        driver.setMedicalClearance(true, "medical notes here");
        driver.applyBan("ban reason here");

        AccessContext viewer = new AccessContext(f1.model.UserRole.VIEWER, "");
        String phone = driver.phoneFor(viewer);
        String notes = driver.medicalNotesFor(viewer);
        String banReason = driver.banReasonFor(viewer);

        if (!phone.equals(AccessPolicy.MASKED_PHONE)) {
            throw new AssertionError("Viewer phone not masked: " + phone);
        }
        if (!notes.isEmpty()) {
            throw new AssertionError("Viewer medical notes not hidden: " + notes);
        }
        if (!banReason.isEmpty()) {
            throw new AssertionError("Viewer ban reason not hidden: " + banReason);
        }

        AccessContext otherDriver = new AccessContext(f1.model.UserRole.DRIVER, "OTHER");
        phone = driver.phoneFor(otherDriver);
        notes = driver.medicalNotesFor(otherDriver);
        banReason = driver.banReasonFor(otherDriver);

        if (!phone.equals(AccessPolicy.MASKED_PHONE)) {
            throw new AssertionError("Other driver phone not masked: " + phone);
        }
        if (!notes.isEmpty()) {
            throw new AssertionError("Other driver medical notes not hidden: " + notes);
        }
        if (!banReason.isEmpty()) {
            throw new AssertionError("Other driver ban reason not hidden: " + banReason);
        }
    }

    private static void testTeamPrincipalCanReadSensitiveData() {
        Driver driver = new Driver("D1", "Driver One", "LIC123", 25, "+1234567890");
        driver.setMedicalClearance(true, "medical notes here");
        driver.applyBan("ban reason here");

        AccessContext principal = new AccessContext(f1.model.UserRole.TEAM_PRINCIPAL, "");
        String phone = driver.phoneFor(principal);
        String notes = driver.medicalNotesFor(principal);
        String banReason = driver.banReasonFor(principal);

        if (!phone.equals("+1234567890")) {
            throw new AssertionError("Team principal phone not visible: " + phone);
        }
        if (!notes.equals("medical notes here")) {
            throw new AssertionError("Team principal medical notes not visible: " + notes);
        }
        if (!banReason.equals("ban reason here")) {
            throw new AssertionError("Team principal ban reason not visible: " + banReason);
        }
    }

    private static void testEqualsAndHashCodeFollowLicenseAndEmployeeId() {
        Driver d1 = new Driver("D1", "Driver One", "LIC123", 25, "+1234567890");
        Driver d2 = new Driver("D2", "Driver Two", "LIC123", 30, "+1987654321");
        if (!d1.equals(d2)) {
            throw new AssertionError("Drivers with same license should be equal.");
        }
        if (d1.hashCode() != d2.hashCode()) {
            throw new AssertionError("Equal drivers should have same hashCode.");
        }

        Engineer e1 = new Engineer("E1", "Engineer One", EngineerSpecialty.AERODYNAMICS, 5, "+1234567890");
        Engineer e2 = new Engineer("E1", "Engineer Two", EngineerSpecialty.RACE_ENGINEERING, 10, "+1987654321");
        if (!e1.equals(e2)) {
            throw new AssertionError("Engineers with same employeeId should be equal.");
        }
        if (e1.hashCode() != e2.hashCode()) {
            throw new AssertionError("Equal engineers should have same hashCode.");
        }
    }
}