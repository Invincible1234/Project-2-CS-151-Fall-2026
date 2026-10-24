package test;

import static test.ConsoleSessionFixture.action;
import static test.ConsoleSessionFixture.check;
import static test.ConsoleSessionFixture.run;

import f1.model.EngineerSpecialty;
import f1.model.UserRole;
import f1.ui.ConsoleState;

import util.AccessContext;

public final class ConsolePersonnelCoordinationVerification {
    public static void main(String[] args) {
        ConsoleState state = new ConsoleState();
        String id = "H1/PLAN";
        String script =
                "4\n2\n1\nPLAN\nH1\nE1\nTRACK1\n9\n15\nE1\nTIRES\n2\n"
                        + action(2, id, "CLEAR")
                        + action(4, id, "DEPLOY=2;RECOVER=1")
                        + action(5, id, "TIRES", "1")
                        + "9\n17\nE1\nH1/PLAN\n"
                        + action(7, id)
                        + action(8, id)
                        + "9\n9\nD1\n9\n10\nE1\n9\n8\nE1\n"
                        + action(10, id, "Crew change")
                        + "9\n9\nD1\n9\n10\nE1\n"
                        + "9\n5\nE3\nEngineer Three\nAERODYNAMICS\n4\n5551234567\n"
                        + "9\n13\nE3\n+15551234567\n9\n14\nE3\nCLASSIC_RESTORATION\n"
                        + "9\n18\nENGINEER\nE3\nEXIT\n";
        String output = run(script, state);
        check(
                state.team().getDrivers().size() == 1
                        && state.team().getDrivers().get(0).getDriverId().equals("D2"),
                "Removing a driver must update the actual selected team.");
        check(
                state.team().getEngineers().size() == 2
                        && state.team().getEngineers().stream()
                                .noneMatch(engineer -> engineer.getEmployeeId().equals("E1")),
                "Engineer removal must preserve the other registered personnel.");
        var engineer =
                state.team().getEngineers().stream()
                        .filter(candidate -> candidate.getEmployeeId().equals("E3"))
                        .findFirst()
                        .orElseThrow();
        check(
                engineer.getSpecialty() == EngineerSpecialty.CLASSIC_RESTORATION
                        && engineer.phoneFor(new AccessContext(UserRole.TEAM_PRINCIPAL, ""))
                                .equals("+15551234567"),
                "Engineer contact and specialty actions must update the selected registered"
                        + " instance.");
        check(
                state.getRole() == UserRole.ENGINEER,
                "Personnel role changes must return to the shared session.");
        check(
                output.contains("Parts requested")
                        && output.contains("Setup confirmed")
                        && output.contains("Cannot release")
                        && !output.contains("not wired yet"),
                "Personnel actions must call the real model and reject release during an active"
                        + " strategy.");
        check(
                state.team().getPartsInventory(UserRole.ENGINEER).get("TIRES") == 10,
                "A request and cancellation must not silently purchase or consume parts.");
        run("9\n10\nE3\nEXIT\n", state);
        check(
                state.team().getEngineers().size() == 1,
                "Registry updates must persist across menu entries.");
        System.out.println(
                "PASS: Personnel registration, assignment protection, updates, and removal"
                        + " coordination");
    }
}
