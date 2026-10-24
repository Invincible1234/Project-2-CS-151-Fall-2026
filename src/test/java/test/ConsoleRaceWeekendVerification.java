package test;

import f1.model.HybridRaceCar;
import f1.model.UserRole;
import f1.ui.ConsoleState;
import java.time.LocalDate;
import model.StrategyStatus;
import model.WeatherCondition;
import static test.ConsoleSessionFixture.action;
import static test.ConsoleSessionFixture.check;
import static test.ConsoleSessionFixture.run;

public final class ConsoleRaceWeekendVerification {
    public static void main(String[] args) {
        ConsoleState state = new ConsoleState();
        String id = "H/PLAN";
        String script = "1\n1\nTEAM\nApex\n2\n100000\n5\nTEAM_PRINCIPAL\n"
                + "6\n1\nH\nHybrid\n2026\n4\n3\n350\n"
                + "9\n1\nD\nDriver\nLICENSE\n25\n5551234567\n9\n2\nD\nyes\nCleared\n"
                + "9\n5\nE\nEngineer\nHYBRID_POWER_UNIT\n5\n5551234567\n"
                + "9\n6\nD\nH\n9\n7\nE\nH\n6\n4\nH\nRACE\n"
                + "7\n1\nTRACK\nCircuit\nVenue\n5\n12\nyes\nCLEAR\n7\n3\nTRACK\nRAIN\n"
                + "3\n4\nTIRES\n1\n2\n1\nPLAN\nH\nE\nTRACK\n"
                + action(2, id, "RAIN") + action(3, id, "INTERMEDIATE")
                + action(4, id, "DEPLOY=1;RECOVER=0.5") + action(5, id, "TIRES", "1")
                + action(6, id) + action(7, id) + action(8, id) + action(9, id)
                + "8\n1\nPLAN\nH\nRESULT\nRACE\n1\n" + LocalDate.now() + "\n8\n5\n8\n6\n"
                + "8\n1\nPLAN\nH\nCOPY\nOTHER-RACE\n1\n" + LocalDate.now() + "\n1\n4\nEXIT\n";
        String output = run(script, state);
        var team = state.team();
        check(team.getDrivers().size() == 1 && team.getEngineers().size() == 1,
                "Personnel menu registrations must belong to the selected team.");
        check(state.getCircuits().size() == 1 && state.getCircuits().get(0).getTypicalWeather() == WeatherCondition.RAIN,
                "Circuit menu changes must be visible to strategy planning.");
        check(team.getStrategies().size() == 1 && team.getStrategies().get(0).getStatus() == StrategyStatus.COMPLETED,
                "A strategy created entirely through menus must complete.");
        HybridRaceCar car = (HybridRaceCar) team.getCars().get(0);
        check(car.getTotalMileage() == 5 && car.getCurrentEnergyMJ() == 2.5 && !car.hasActiveAssignments(),
                "The integrated session must apply energy and mileage once and release the crew.");
        check(team.getPartsInventory(UserRole.TEAM_PRINCIPAL).isEmpty()
                && team.getBudget(UserRole.TEAM_PRINCIPAL) == 98800, "Purchased tires must be charged and consumed once.");
        check(team.getRaceResults().size() == 1 && team.getChampionshipPoints() == 25
                && team.getDrivers().get(0).getChampionshipPoints() == 25,
                "Results must refer to the completed entry and reject duplicate points.");
        check(output.contains("already has a result") && output.contains("25 points applied")
                && output.contains("Total mileage (km): 5.00"), "Reports must match the completed race and rejected retry.");
        check(!output.contains("not wired yet") && !output.contains("Exception") && !output.contains("at f1."),
                "Integrated paths must execute real actions without placeholder output or stack traces.");
        System.out.println("PASS: Console race weekend from an empty session through result recording");
    }
}
