package test;

import f1.model.Team;
import f1.model.UserRole;

public final class TeamBudgetVerification {
    private static int checks;

    public static void main(String[] args) {
        verifyFunding();
        verifyRejectedAmounts();
        verifyAccess();
        verifySummaryPrivacy();
        System.out.println("PASS: Team budget (" + checks + " checks)");
    }

    private static void verifyFunding() {
        Team team = new Team("BUDGET", "Apex", 2, 1000.50);
        check(
                team.getBudget(UserRole.TEAM_PRINCIPAL) == 1000.50,
                "Initial funding must be preserved.");
        team.addBudget(250.25, UserRole.TEAM_PRINCIPAL);
        team.addBudget(0.25, UserRole.TEAM_PRINCIPAL);
        check(
                team.getBudget(UserRole.TEAM_PRINCIPAL) == 1251,
                "Repeated funding must accumulate without replacing the balance.");
        check(
                team.getCars().isEmpty()
                        && team.getDrivers().isEmpty()
                        && team.getEngineers().isEmpty()
                        && team.getChampionshipPoints() == 0,
                "Funding must not modify other team state.");
        Team empty = new Team("EMPTY", "Apex", 1, 0);
        empty.addBudget(0.01, UserRole.TEAM_PRINCIPAL);
        check(
                empty.getBudget(UserRole.TEAM_PRINCIPAL) == 0.01,
                "An unfunded team must accept a positive addition.");
    }

    private static void verifyRejectedAmounts() {
        Team team = new Team("REJECT", "Apex", 2, 1000);
        for (double amount :
                new double[] {
                    0, -0.0, -0.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY
                }) {
            expectFailure(
                    IllegalArgumentException.class,
                    () -> team.addBudget(amount, UserRole.TEAM_PRINCIPAL));
            check(
                    team.getBudget(UserRole.TEAM_PRINCIPAL) == 1000,
                    "Invalid additions must preserve the balance.");
        }
        Team large = new Team("LARGE", "Apex", 1, Double.MAX_VALUE);
        expectFailure(
                IllegalArgumentException.class,
                () -> large.addBudget(Double.MAX_VALUE, UserRole.TEAM_PRINCIPAL));
        expectFailure(
                IllegalArgumentException.class, () -> large.addBudget(1, UserRole.TEAM_PRINCIPAL));
        check(
                large.getBudget(UserRole.TEAM_PRINCIPAL) == Double.MAX_VALUE,
                "Overflow and lost-precision rejection must preserve the balance.");
        team.addBudget(50, UserRole.TEAM_PRINCIPAL);
        check(
                team.getBudget(UserRole.TEAM_PRINCIPAL) == 1050,
                "A rejected addition must not prevent later valid funding.");
    }

    private static void verifyAccess() {
        Team team = new Team("ACCESS", "Apex", 1, 500);
        for (UserRole role : UserRole.values()) {
            if (role != UserRole.TEAM_PRINCIPAL) {
                expectFailure(SecurityException.class, () -> team.getBudget(role));
                expectFailure(SecurityException.class, () -> team.addBudget(100, role));
                check(
                        team.getBudget(UserRole.TEAM_PRINCIPAL) == 500,
                        "Unauthorized funding must not modify the balance.");
            }
        }
        expectFailure(SecurityException.class, () -> team.getBudget(null));
        expectFailure(SecurityException.class, () -> team.addBudget(100, null));
        expectFailure(SecurityException.class, () -> team.addBudget(Double.NaN, UserRole.VIEWER));
        check(
                team.getBudget(UserRole.TEAM_PRINCIPAL) == 500,
                "Missing or unauthorized roles must not modify the balance.");
    }

    private static void verifySummaryPrivacy() {
        Team team = new Team("PRIVATE", "Apex", 1, 98765.43);
        team.addBudget(100, UserRole.TEAM_PRINCIPAL);
        String summary = team.toString();
        check(
                !summary.contains("98765")
                        && !summary.contains("98865")
                        && !summary.contains("budget"),
                "Public summaries must not expose initial or updated balances.");
        check(
                summary.contains("PRIVATE") && summary.contains("Apex"),
                "A private summary must still identify the team.");
    }

    private static void expectFailure(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName() + ".");
        } catch (RuntimeException failure) {
            if (!expected.isInstance(failure)) {
                throw new AssertionError("Expected " + expected.getSimpleName() + ".", failure);
            }
            check(
                    failure.getMessage() != null && !failure.getMessage().isBlank(),
                    "Rejected actions must have a clear error message.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
