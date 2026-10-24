package test;

import static test.ConsoleSessionFixture.action;
import static test.ConsoleSessionFixture.check;
import static test.ConsoleSessionFixture.run;

import f1.model.ClassicRaceCar;
import f1.model.HybridRaceCar;
import f1.model.RaceResultStatus;
import f1.model.UserRole;
import f1.ui.ConsoleState;

import model.StrategyStatus;

public final class ProjectAcceptanceVerification {
    public static void main(String[] args) {
        ConsoleState state = new ConsoleState();
        String create =
                "1\n1\nTEAM\nApex\n2\n100000\n5\nTEAM_PRINCIPAL\n"
                        + "6\n1\nH\nHybrid\n2026\n4\n3\n350\n"
                        + "6\n2\nC\nClassic\n1980\n90\n2020\n250\n"
                        + "9\n1\nD\nDriver\nLICENSE\n25\n5551234567\n9\n2\nD\nYES\nCleared\n"
                        + "9\n5\nEH\nHybrid Engineer\nHYBRID_POWER_UNIT\n5\n5551234567\n"
                        + "9\n5\nEC\nClassic Engineer\nCLASSIC_RESTORATION\n8\n5551234567\n"
                        + "9\n6\nD\nH\n9\n6\nD\nC\n9\n7\nEH\nH\n9\n7\nEC\nC\n"
                        + "7\n1\nTRACK\nCircuit\nVenue\n5\n12\nYES\nRAIN\n"
                        + "7\n4\nTRACK\nBrake early in rain\n7\n5\nTRACK\n100\n7\n6\nTRACK\n"
                        + "9\n15\nEH\nTIRES\n3\n3\n6\nEH\nTIRES\n3\n"
                        + plan("H", "HP", "EH", true)
                        + plan("C", "CP", "EC", false)
                        + action(8, "H/HP")
                        + action(8, "C/CP")
                        + "EXIT\n";
        String active = run(create, state);
        var team = state.team();
        check(
                team.getCars().size() == 2
                        && team.getDrivers().size() == 1
                        && team.getEngineers().size() == 2
                        && team.getStrategies().size() == 2
                        && state.getCircuits().size() == 1,
                "All project components must be creatable and connected through the shared"
                        + " console.");
        check(
                team.getStrategies().get(0).getStatus() == StrategyStatus.ACTIVE
                        && team.getStrategies().get(1).getStatus() == StrategyStatus.READY
                        && active.contains("Driver has a car with an active session"),
                "A driver shared between both car types must not run simultaneous strategies.");
        String finish =
                run(
                        action(10, "H/HP", "Run classic first")
                                + action(8, "C/CP")
                                + action(9, "C/CP")
                                + result("CP", "C", "CR", "CLASSIC-RACE", 2)
                                + "9\n6\nD\nH\n9\n7\nEH\nH\n"
                                + plan("H", "HP2", "EH", true)
                                + action(8, "H/HP2")
                                + action(9, "H/HP2")
                                + result("HP2", "H", "HR", "HYBRID-RACE", 1)
                                + "8\n3\nCR\n3\nTiming correction\n8\n4\nHR\nDisqualified\nYES\n"
                                + "1\n4\n8\n5\n2\n11\nEXIT\n",
                        state);
        HybridRaceCar hybrid = (HybridRaceCar) team.getCars().get(0);
        ClassicRaceCar classic = (ClassicRaceCar) team.getCars().get(1);
        check(
                team.getStrategies().get(0).getStatus() == StrategyStatus.CANCELLED
                        && team.getStrategies().get(1).getStatus() == StrategyStatus.COMPLETED
                        && team.getStrategies().get(2).getStatus() == StrategyStatus.COMPLETED,
                "Cancellation must leave both car types usable for later complete race workflows.");
        check(
                hybrid.getTotalMileage() == 5
                        && classic.getTotalMileage() == 5
                        && hybrid.getCurrentEnergyMJ() == 2.5
                        && !hybrid.hasActiveAssignments()
                        && !classic.hasActiveAssignments()
                        && team.getEngineers().stream()
                                .allMatch(engineer -> engineer.isAvailable()),
                "Only completed races may change mileage/energy, and both races must release"
                        + " crew.");
        check(
                team.getBudget(UserRole.TEAM_PRINCIPAL) == 96400
                        && team.getPartsInventory(UserRole.TEAM_PRINCIPAL).get("TIRES") == 1,
                "Requests must purchase once, cancellation must release reservations, and two"
                        + " finishes must consume two tires.");
        check(
                team.getRaceResults().size() == 2
                        && team.getChampionshipPoints() == 15
                        && team.getDrivers().get(0).getChampionshipPoints() == 15
                        && team.getRaceResults().get(1).getStatus() == RaceResultStatus.VOIDED,
                "Both result types must reconcile driver/team points after corrections and"
                        + " voiding.");
        check(
                finish.contains("Recorded results: 1")
                        && finish.contains("Total mileage (km): 10.00")
                        && finish.contains("15 points")
                        && !finish.contains("Action failed")
                        && !finish.contains("Exception")
                        && !finish.contains("not wired"),
                "The final whole-project workflow must finish without unexpected errors or"
                        + " placeholders.");
        System.out.println("Whole-project acceptance verification passed.");
    }

    private static String plan(String car, String id, String engineer, boolean hybrid) {
        String key = car + "/" + id;
        return "2\n1\n"
                + id
                + "\n"
                + car
                + "\n"
                + engineer
                + "\nTRACK\n"
                + action(2, key, "RAIN")
                + action(3, key, "INTERMEDIATE")
                + (hybrid ? action(4, key, "DEPLOY=1;RECOVER=0.5") : "")
                + action(5, key, "TIRES", "1")
                + "9\n17\n"
                + engineer
                + "\n"
                + key
                + "\n"
                + action(7, key);
    }

    private static String result(
            String strategy, String car, String result, String race, int position) {
        return "8\n1\n"
                + strategy
                + "\n"
                + car
                + "\n"
                + result
                + "\n"
                + race
                + "\n"
                + position
                + "\n"
                + ConsoleSessionFixture.TODAY
                + "\n";
    }
}
