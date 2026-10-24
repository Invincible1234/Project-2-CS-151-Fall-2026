package test;

import exception.CapacityExceededException;
import exception.InsufficientResourcesException;

import f1.model.Team;
import f1.model.UserRole;

import util.PartsCatalog;

import java.util.Map;

public final class TeamInventoryVerification {
    private static int checks;

    public static void main(String[] args) {
        verifyPurchases();
        verifyConsumption();
        verifyInvalidInputs();
        verifyInsufficientResources();
        verifyNumericLimits();
        verifyInventoryAccess();
        System.out.println("PASS: Team inventory (" + checks + " checks)");
    }

    private static void verifyPurchases() {
        Team team = team(100000);
        Map<String, Integer> before = inventory(team);
        check(team.addParts(" tires ", 2), "Purchase must succeed with sufficient funds.");
        check(inventory(team).equals(Map.of("TIRES", 2)), "Part names must be normalized.");
        check(
                balance(team) == 100000 - 2 * PartsCatalog.getUnitPrice("TIRES"),
                "Purchase must deduct the catalog price for every item.");
        check(before.isEmpty(), "Earlier inventory snapshots must not change.");
        check(team.addParts("TIRES", 3), "Repeated purchase must succeed.");
        check(team.addParts("AERO_KIT", 1), "Different catalog parts must be supported.");
        check(
                inventory(team).equals(Map.of("TIRES", 5, "AERO_KIT", 1)),
                "Purchases must accumulate stock under canonical keys.");
        check(
                balance(team)
                        == 100000
                                - 5 * PartsCatalog.getUnitPrice("TIRES")
                                - PartsCatalog.getUnitPrice("AERO_KIT"),
                "Each purchase must charge exactly once.");
        check(
                team.getCars().isEmpty()
                        && team.getDrivers().isEmpty()
                        && team.getEngineers().isEmpty()
                        && team.getChampionshipPoints() == 0,
                "Purchases must not alter unrelated team state.");
    }

    private static void verifyConsumption() {
        Team team = team(100000);
        team.addParts("TIRES", 3);
        team.addParts("AERO_KIT", 1);
        double budget = balance(team);
        Map<String, Integer> before = inventory(team);
        check(team.consumeParts(" tires ", 2), "Partial consumption must succeed.");
        check(
                inventory(team).equals(Map.of("TIRES", 1, "AERO_KIT", 1)),
                "Consumption must update only the requested part.");
        check(before.get("TIRES") == 3, "Consumption must not change earlier snapshots.");
        check(team.consumeParts("TIRES", 1), "Exact-stock consumption must succeed.");
        check(!inventory(team).containsKey("TIRES"), "Empty stock entries must be removed.");
        check(balance(team) == budget, "Consumption must neither charge nor refund the budget.");
        expectUnchanged(
                team, InsufficientResourcesException.class, () -> team.consumeParts("TIRES", 1));
    }

    private static void verifyInvalidInputs() {
        Team team = team(100000);
        team.addParts("TIRES", 2);
        for (String invalid : new String[] {null, "", " \t\n", "\u2003", "UNKNOWN"}) {
            expectUnchanged(team, IllegalArgumentException.class, () -> team.addParts(invalid, 1));
            expectUnchanged(
                    team, IllegalArgumentException.class, () -> team.consumeParts(invalid, 1));
        }
        for (int invalid : new int[] {0, -1, Integer.MIN_VALUE}) {
            expectUnchanged(
                    team, IllegalArgumentException.class, () -> team.addParts("TIRES", invalid));
            expectUnchanged(
                    team,
                    IllegalArgumentException.class,
                    () -> team.consumeParts("TIRES", invalid));
        }
    }

    private static void verifyInsufficientResources() {
        double tirePrice = PartsCatalog.getUnitPrice("TIRES");
        Team unfunded = team(0);
        expectUnchanged(
                unfunded,
                InsufficientResourcesException.class,
                () -> unfunded.addParts("TIRES", 1));
        Team shortBudget = team(tirePrice - 0.01);
        expectUnchanged(
                shortBudget,
                InsufficientResourcesException.class,
                () -> shortBudget.addParts("TIRES", 1));
        Team exactBudget = team(tirePrice);
        check(
                exactBudget.addParts("TIRES", 1) && balance(exactBudget) == 0,
                "A purchase may spend the exact remaining budget.");
        expectUnchanged(
                exactBudget,
                InsufficientResourcesException.class,
                () -> exactBudget.addParts("TIRES", 1));
        expectUnchanged(
                exactBudget,
                InsufficientResourcesException.class,
                () -> exactBudget.consumeParts("TIRES", 2));
        expectUnchanged(
                exactBudget,
                InsufficientResourcesException.class,
                () -> exactBudget.consumeParts("HYBRID_COMPONENTS", 1));
        exactBudget.addBudget(tirePrice, UserRole.TEAM_PRINCIPAL);
        check(
                exactBudget.addParts("TIRES", 1) && inventory(exactBudget).get("TIRES") == 2,
                "Rejected transactions must not prevent a later valid purchase.");
    }

    private static void verifyNumericLimits() {
        Team team = team(1.0e16);
        check(
                team.addParts("TIRES", Integer.MAX_VALUE),
                "Largest supported quantity must be accepted.");
        check(
                inventory(team).get("TIRES") == Integer.MAX_VALUE,
                "Large quantities must not wrap around.");
        expectUnchanged(team, CapacityExceededException.class, () -> team.addParts("TIRES", 1));
        check(
                team.consumeParts("TIRES", Integer.MAX_VALUE) && inventory(team).isEmpty(),
                "Largest supported stock must be consumable without overflow.");
        Team imprecise = team(Double.MAX_VALUE);
        expectUnchanged(
                imprecise, IllegalArgumentException.class, () -> imprecise.addParts("TIRES", 1));
    }

    private static void verifyInventoryAccess() {
        Team team = team(100000);
        team.addParts("RESTORATION_KIT", 2);
        check(
                team.getPartsInventory(UserRole.ENGINEER).equals(inventory(team)),
                "Engineer and principal roles must receive the same inventory snapshot.");
        for (UserRole role : new UserRole[] {UserRole.VIEWER, UserRole.DRIVER, null}) {
            expectUnchanged(team, SecurityException.class, () -> team.getPartsInventory(role));
        }
        Map<String, Integer> snapshot = inventory(team);
        expectUnchanged(
                team,
                UnsupportedOperationException.class,
                () -> snapshot.put("RESTORATION_KIT", 999));
        expectUnchanged(team, UnsupportedOperationException.class, snapshot::clear);
        String summary = team.toString();
        check(
                !summary.contains("RESTORATION_KIT") && !summary.contains("partsInventory"),
                "Public summaries must not reveal detailed inventory.");
    }

    private static void expectUnchanged(
            Team team, Class<? extends RuntimeException> expected, Runnable action) {
        double budget = balance(team);
        Map<String, Integer> stock = inventory(team);
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName() + ".");
        } catch (RuntimeException failure) {
            if (!expected.isInstance(failure)) {
                throw new AssertionError("Expected " + expected.getSimpleName() + ".", failure);
            }
            check(
                    expected == UnsupportedOperationException.class
                            || failure.getMessage() != null && !failure.getMessage().isBlank(),
                    "Rejected transactions must have a clear error message.");
        }
        check(
                balance(team) == budget && inventory(team).equals(stock),
                "A rejected operation must preserve both budget and inventory.");
    }

    private static Team team(double budget) {
        return new Team("INVENTORY", "Apex", 2, budget);
    }

    private static double balance(Team team) {
        return team.getBudget(UserRole.TEAM_PRINCIPAL);
    }

    private static Map<String, Integer> inventory(Team team) {
        return team.getPartsInventory(UserRole.TEAM_PRINCIPAL);
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
