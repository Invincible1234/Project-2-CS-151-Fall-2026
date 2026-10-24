package test;

import f1.model.Team;

import util.Constants;

import java.util.HashSet;
import java.util.List;

public final class TeamFoundationVerification {
    private static int checks;

    public static void main(String[] args) {
        verifyConstruction();
        verifyInvalidInputs();
        verifyIdentity();
        verifyCollections();
        verifySummary();
        System.out.println("PASS: Team foundation (" + checks + " checks)");
    }

    private static void verifyConstruction() {
        Team team = new Team("  TEAM-1  ", "  Apex Racing  ", 2, 0);
        check("TEAM-1".equals(team.getTeamId()), "Team ID must be stripped.");
        check("Apex Racing".equals(team.getName()), "Team name must be stripped.");
        check(team.getCapacity() == 2, "Team capacity must be preserved.");
        check(team.getChampionshipPoints() == 0, "A new team must start with zero points.");
        check(
                new Team("MIN", "Minimum", 1, 0).getCapacity() == 1,
                "Minimum capacity must be accepted.");
        check(
                new Team("MAX", "Maximum", Constants.MAXIMUM_INSTANCES, Double.MAX_VALUE)
                                .getCapacity()
                        == Constants.MAXIMUM_INSTANCES,
                "Maximum capacity and finite budget must be accepted.");
    }

    private static void verifyInvalidInputs() {
        for (String invalid : new String[] {null, "", " \t\n", "\u2003"}) {
            expectInvalid(() -> new Team(invalid, "Apex", 2, 0), "Invalid team ID accepted.");
            expectInvalid(() -> new Team("ID", invalid, 2, 0), "Invalid team name accepted.");
        }
        for (int invalid :
                new int[] {
                    Integer.MIN_VALUE, -1, 0, Constants.MAXIMUM_INSTANCES + 1, Integer.MAX_VALUE
                }) {
            expectInvalid(() -> new Team("ID", "Apex", invalid, 0), "Invalid capacity accepted.");
        }
        for (double invalid :
                new double[] {
                    -0.01,
                    -Double.MAX_VALUE,
                    Double.NaN,
                    Double.POSITIVE_INFINITY,
                    Double.NEGATIVE_INFINITY
                }) {
            expectInvalid(() -> new Team("ID", "Apex", 2, invalid), "Invalid budget accepted.");
        }
    }

    private static void verifyIdentity() {
        Team first = new Team("ID-1", "Apex", 2, 100);
        Team sameId = new Team(" ID-1 ", "Different name", 3, 200);
        Team third = new Team("ID-1", "Third name", 1, 0);
        Team differentId = new Team("ID-2", "Apex", 2, 100);
        check(first.equals(first), "Equality must be reflexive.");
        check(first.equals(sameId) && sameId.equals(first), "Equality must use stable team ID.");
        check(sameId.equals(third) && first.equals(third), "Equality must be transitive.");
        check(first.hashCode() == sameId.hashCode(), "Equal teams must have equal hash codes.");
        check(!first.equals(differentId), "Different team IDs must not compare equal.");
        check(
                !first.equals(null) && !first.equals("ID-1"),
                "Unrelated values must not compare equal.");
        HashSet<Team> teams = new HashSet<>();
        teams.add(first);
        teams.add(sameId);
        teams.add(differentId);
        check(teams.size() == 2, "Hash collections must recognize team identity.");
    }

    private static void verifyCollections() {
        Team team = new Team("ID", "Apex", 2, 0);
        List<List<?>> collections =
                List.of(
                        team.getCars(),
                        team.getDrivers(),
                        team.getEngineers(),
                        team.getRaceResults(),
                        team.getStrategies());
        for (List<?> collection : collections) {
            check(collection.isEmpty(), "A new team's collections must be initialized and empty.");
            try {
                collection.add(null);
                throw new AssertionError("Collection accessor allowed external mutation.");
            } catch (UnsupportedOperationException expected) {
                checks++;
            }
        }
        check(
                team.getCars().isEmpty()
                        && team.getDrivers().isEmpty()
                        && team.getEngineers().isEmpty()
                        && team.getRaceResults().isEmpty()
                        && team.getStrategies().isEmpty(),
                "Failed external mutations must leave the team unchanged.");
    }

    private static void verifySummary() {
        Team team = new Team("TEAM-SUMMARY", "Apex Racing", 2, 98765.43);
        String summary = team.toString();
        check(
                summary.contains("TEAM-SUMMARY") && summary.contains("Apex Racing"),
                "Summary must identify the team.");
        check(
                summary.contains("capacity=2")
                        && summary.contains("cars=0")
                        && summary.contains("championshipPoints=0"),
                "Summary must show useful safe state.");
        check(
                !summary.contains("98765")
                        && !summary.contains("budget")
                        && !summary.contains("partsInventory"),
                "Summary must not expose budget or inventory.");
    }

    private static void expectInvalid(Runnable action, String message) {
        try {
            action.run();
            throw new AssertionError(message);
        } catch (IllegalArgumentException expected) {
            check(
                    expected.getMessage() != null && !expected.getMessage().isBlank(),
                    "Validation errors must have an explanation.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
