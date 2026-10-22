package test;

import f1.model.Driver;
import f1.model.Engineer;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.Team;
import f1.model.UserRole;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TeamPerformanceVerification {
    private static int checks;

    public static void main(String[] args) {
        verifyEmptyReport();
        verifyPopulatedReport();
        verifyPrivacy();
        verifyRejectedResult();
        System.out.println("PASS: Team performance (" + checks + " checks)");
    }

    private static void verifyEmptyReport() {
        Team team = new Team("EMPTY", "Apex", 2, 0);
        String report = team.generateTeamPerformanceReport();
        check(report.contains("Apex (EMPTY)") && report.contains("Cars: 0/2"),
                "Report must identify the team and capacity.");
        check(report.contains("Recorded results: 0") && report.contains("Championship points: 0"),
                "New teams must have no results or points.");
        check(report.contains("Wins: 0") && report.contains("Podiums: 0")
                && report.contains("Best finish: N/A") && report.contains("Average finish: N/A"),
                "Empty statistics must avoid invalid averages or sentinel values.");
        check(report.contains("No registered drivers."), "Empty standings must be explained.");
    }

    private static void verifyPopulatedReport() {
        Team team = new Team("POPULATED", "Apex", 2, 100000);
        F1Car first = new HybridRaceCar("CAR-1", "Apex", "Hybrid", 2026, 4, 2);
        F1Car second = new HybridRaceCar("CAR-2", "Apex", "Hybrid", 2026, 4, 2);
        first.updateMileage(12.5);
        second.updateMileage(7.75);
        second.markOutOfService();
        team.addCar(first);
        team.addCar(second);
        Driver zed = new Driver("ZED", "Driver Z", "LICENSE-Z", 25, "5551234567");
        Driver alpha = new Driver(" ALPHA ", "Driver A", "LICENSE-A", 25, "5551234567");
        team.addDriver(zed);
        team.addDriver(alpha);
        team.addEngineer(new Engineer("ENGINEER", "Engineer", "HYBRID_POWER_UNIT", 5, "5551234567"));
        List<Driver> before = team.getDrivers();
        String report = team.generateTeamPerformanceReport();
        check(report.contains("Cars: 2/2") && report.contains("Ready cars: 0")
                && report.contains("Decommissioned cars: 1"), "Report must reflect current car state.");
        check(report.contains("Total mileage (km): 20.25"), "Report must aggregate car mileage.");
        check(report.contains("Drivers: 2") && report.contains("Engineers: 1"),
                "Report must count personnel.");
        check(report.indexOf("ALPHA: 0 points") < report.indexOf("ZED: 0 points")
                && report.contains("ALPHA: 0 points") && report.contains("ZED: 0 points"),
                "Tied standings must have a deterministic driver-ID order.");
        check(team.getDrivers().get(0) == zed && before.get(0) == zed,
                "Sorting standings must not reorder registered drivers.");
        check(report.equals(team.generateTeamPerformanceReport()), "Repeated reporting must be stable.");
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.GERMANY);
            check(report.equals(team.generateTeamPerformanceReport()),
                    "Report formatting must not depend on the machine locale.");
        } finally {
            Locale.setDefault(original);
        }
    }

    private static void verifyPrivacy() {
        Team team = new Team("PRIVATE", "Apex", 2, 98765.43);
        Driver driver = new Driver("DRIVER", "Driver", "PRIVATE-LICENSE", 25, "5559876543");
        driver.setMedicalClearance(true, "PRIVATE-MEDICAL-NOTE");
        driver.applyBan("PRIVATE-BAN-REASON");
        team.addDriver(driver);
        team.addParts("TIRES", 2);
        double budget = team.getBudget(UserRole.TEAM_PRINCIPAL);
        Map<String, Integer> inventory = team.getPartsInventory(UserRole.TEAM_PRINCIPAL);
        String report = team.generateTeamPerformanceReport();
        for (String sensitive : new String[] { "98765", "96365", "PRIVATE-LICENSE", "5559876543",
                "PRIVATE-MEDICAL-NOTE", "PRIVATE-BAN-REASON", "TIRES", "budget", "partsInventory" }) {
            check(!report.contains(sensitive), "Public report exposed restricted data: " + sensitive);
        }
        check(team.getBudget(UserRole.TEAM_PRINCIPAL) == budget
                && team.getPartsInventory(UserRole.TEAM_PRINCIPAL).equals(inventory)
                && team.getChampionshipPoints() == 0, "Reporting must not change team state.");
    }

    private static void verifyRejectedResult() {
        Team team = new Team("REJECT", "Apex", 1, 0);
        String before = team.generateTeamPerformanceReport();
        try {
            team.recordRaceResult(null);
            throw new AssertionError("Null result was accepted.");
        } catch (IllegalArgumentException expected) {
            check(expected.getMessage() != null && !expected.getMessage().isBlank(),
                    "Invalid results must have a clear error message.");
        }
        check(before.equals(team.generateTeamPerformanceReport()) && team.getRaceResults().isEmpty(),
                "Rejected result must not alter results or statistics.");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
