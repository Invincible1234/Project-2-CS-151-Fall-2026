package test;

import static test.ConsoleSessionFixture.check;
import static test.ConsoleSessionFixture.run;

import f1.model.UserRole;
import f1.ui.ConsoleState;

public final class ConsolePersonnelPrivacyVerification {
    public static void main(String[] args) {
        ConsoleState state = new ConsoleState();
        String viewer = run("4\n9\n22\n1\nD1\nEXIT\n", state);
        check(
                viewer.contains("Phone: **********") && !viewer.contains("Medical notes: Cleared"),
                "Viewer details must mask contact and medical information.");
        String owner = run("9\n18\nDRIVER\nD1\n9\n22\n1\nD1\nEXIT\n", state);
        check(
                owner.contains("Phone: 5551234567")
                        && owner.contains("Medical notes: Cleared")
                        && state.getAccessContext().getActorId().equals("D1"),
                "The actor identity must survive returning to the main menu and opening details.");
        String other = run("9\n22\n1\nD2\nEXIT\n", state);
        check(
                other.contains("Phone: **********") && !other.contains("Medical notes: Cleared"),
                "A driver must not see a different driver's private details.");
        String principal = run("5\nTEAM_PRINCIPAL\n9\n22\n1\nD2\n9\n23\nLICENSE1\nEXIT\n", state);
        check(
                principal.contains("Medical notes: Cleared")
                        && principal.contains("driverId='D1'")
                        && state.getRole() == UserRole.TEAM_PRINCIPAL,
                "A principal may view details and find a registered driver by license.");
        String engineer = run("9\n18\nENGINEER\nE1\n9\n22\n2\nE1\n9\n22\n2\nE2\nEXIT\n", state);
        check(
                engineer.contains("Phone: 5551234567") && engineer.contains("Phone: **********"),
                "An engineer may view their own contact, while another engineer's contact stays"
                        + " masked.");
        System.out.println("Console personnel privacy verification passed.");
    }
}
