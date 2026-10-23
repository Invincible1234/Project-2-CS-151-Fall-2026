package test;

import f1.model.Team;
import f1.model.UserRole;
import f1.ui.ConsoleState;
import java.util.Map;
import static test.ConsoleSessionFixture.check;
import static test.ConsoleSessionFixture.run;

public final class ConsoleTeamMenuVerification {
    public static void main(String[] args) {
        ConsoleState state = new ConsoleState();
        String script = "1\n3\n1\n1\nT1\nApex\n0\n2\n-1\n10000\n"
                + "1\n1\nT1\nT2\nSecond\n1\n0\n1\n2\nmissing\nT1\n1\n3\n1\n4\n"
                + "3\n1\n3\n2\n5\nENGINEER\n3\n2\n3\n1\n3\n3\n"
                + "5\nTEAM_PRINCIPAL\n3\n3\n0\n500\n3\n4\nTIRES\n-1\n2\n"
                + "3\n5\nTIRES\n3\n3\n5\nTIRES\n1\n3\n1\n3\n2\nEXIT\n";
        String transcript = run(script, state);
        Team team = state.team();
        check(state.getTeams().size() == 2 && team.getTeamId().equals("T1"), "Create, duplicate retry, and selection must work.");
        check(team.getBudget(UserRole.TEAM_PRINCIPAL) == 8100, "Budget additions and part purchases must use model accounting.");
        check(team.getPartsInventory(UserRole.ENGINEER).equals(Map.of("TIRES", 1)), "Consumption must reject insufficient stock.");
        check(transcript.contains("Team performance: Apex") && transcript.contains("Created and selected team: T2"),
                "Team summary and performance must be reachable.");
        check(transcript.contains("Team principal access is required")
                && transcript.contains("Team principal or engineer access is required"), "Resource access must respect the session role.");
        check(transcript.contains("Not enough unreserved parts") && transcript.contains("Budget: 8100.0"),
                "Business failures must recover to a usable main menu.");
        check(!transcript.contains("Exception") && !transcript.contains("at f1."), "Handled failures must not expose stack traces.");
        check(transcript.split("Main menu", -1).length > 10, "Actions must redisplay the main menu.");
        ConsoleState demo = new ConsoleState();
        String demoOutput = run("4\n4\nEXIT\n", demo);
        check(demo.getTeams().size() == 1 && demo.getCircuits().size() == 1
                && demo.team().getCars().size() == 2, "Repeated demo loading must not duplicate data.");
        check(demoOutput.contains("Demo IDs are already in use"), "Duplicate demo loading must give a clear error.");
        System.out.println("PASS: Team menus, resource accounting, and permissions");
    }
}
