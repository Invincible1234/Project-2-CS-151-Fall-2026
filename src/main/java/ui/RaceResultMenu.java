package ui;

import f1.model.Driver;
import f1.model.RaceResult;
import f1.model.RaceStrategy;
import f1.model.Team;

import model.StrategyStatus;

import java.util.Comparator;
import java.util.Objects;

public final class RaceResultMenu {
    private final ConsoleInput input;
    private final Team team;

    public RaceResultMenu(ConsoleInput input, Team team) {
        this.input = Objects.requireNonNull(input, "Shared input is required.");
        this.team = Objects.requireNonNull(team, "Team is required.");
    }

    public void run() {
        while (open()) {}
    }

    public boolean open() {
        input.print(
                "Race results: 1 Record and apply points | 2 List | 3 Correct finish"
                        + " | 4 Void result | 5 Standings | 6 Team performance | 0 Back");
        int option = input.integer("Choose a result action", 0, 6);
        if (option == 0) {
            return false;
        }
        input.action(() -> perform(option));
        return true;
    }

    private void perform(int option) {
        switch (option) {
            case 1 -> recordResult();
            case 2 -> {
                if (team.getRaceResults().isEmpty()) {
                    input.print("No race results recorded.");
                }
                for (RaceResult result : team.getRaceResults()) {
                    input.print(result.summarize());
                    if (!result.getCorrectionReason().isEmpty()) {
                        input.print("Correction reason: " + result.getCorrectionReason());
                    }
                    if (!result.getVoidReason().isEmpty()) {
                        input.print("Void reason: " + result.getVoidReason());
                    }
                }
            }
            case 3 -> {
                RaceResult result = selectResult();
                int position = input.integer("Corrected finishing position", 1, Integer.MAX_VALUE);
                result.correctFinishingPosition(position, input.text("Correction reason"));
                input.print("Finish corrected and championship points updated.");
            }
            case 4 -> {
                RaceResult result = selectResult();
                String reason = input.text("Void reason");
                if (input.yesNo(
                        "Void result " + result.getResultId() + " and reverse its points?")) {
                    result.voidResult(reason);
                    input.print("Result voided and championship points reversed.");
                } else {
                    input.print("Voiding cancelled.");
                }
            }
            case 5 -> showStandings();
            case 6 -> input.print(team.generateTeamPerformanceReport());
            default -> throw new IllegalArgumentException("Unknown result action.");
        }
    }

    private void recordResult() {
        var completed =
                team.getStrategies().stream()
                        .filter(strategy -> strategy.getStatus() == StrategyStatus.COMPLETED)
                        .toList();
        if (completed.isEmpty()) {
            throw new IllegalStateException("Complete a race strategy before recording a result.");
        }
        completed.forEach(
                strategy ->
                        input.print(
                                "Strategy "
                                        + strategy.getStrategyId()
                                        + ", chassis "
                                        + strategy.getChassisId()
                                        + ", driver "
                                        + strategy.getRaceDriver().getDriverId()
                                        + ", circuit "
                                        + strategy.getCircuit().getCircuitId()));
        String strategyId = input.text("Completed strategy ID");
        String chassisId = input.text("Chassis ID from that strategy");
        RaceStrategy strategy =
                completed.stream()
                        .filter(
                                candidate ->
                                        candidate.getStrategyId().equals(strategyId)
                                                && candidate.getChassisId().equals(chassisId))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                "No completed strategy matches that ID and"
                                                        + " chassis."));
        String resultId = input.text("Unique result ID");
        if (team.getRaceResults().stream()
                .anyMatch(result -> result.getResultId().equals(resultId))) {
            throw new IllegalArgumentException("Result ID is already registered.");
        }
        String raceId = input.text("Race ID");
        int position = input.integer("Finishing position", 1, Integer.MAX_VALUE);
        String date = input.pastOrPresentDate("Race date").toString();
        RaceResult result =
                new RaceResult(
                        resultId,
                        raceId,
                        strategy.getChassisId(),
                        strategy.getRaceDriver().getDriverId(),
                        strategy.getCircuit().getCircuitId(),
                        position,
                        date,
                        input.clock());
        result.recordResult();
        strategy.getRaceDriver().completeRace(result, team, strategy);
        input.print(
                "Result recorded; "
                        + result.getPointsEarned()
                        + " points applied to driver and team.");
    }

    private RaceResult selectResult() {
        if (team.getRaceResults().isEmpty()) {
            throw new IllegalStateException("No race results recorded.");
        }
        String id = input.text("Result ID");
        return team.getRaceResults().stream()
                .filter(result -> result.getResultId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No result has that ID."));
    }

    private void showStandings() {
        input.print(
                "Team "
                        + team.getName()
                        + " ("
                        + team.getTeamId()
                        + "): "
                        + team.getChampionshipPoints()
                        + " points");
        input.print("Driver standings for this team:");
        if (team.getDrivers().isEmpty()) {
            input.print("No registered drivers.");
        }
        team.getDrivers().stream()
                .sorted(
                        Comparator.comparingInt(Driver::getChampionshipPoints)
                                .reversed()
                                .thenComparing(Driver::getDriverId))
                .forEach(
                        driver ->
                                input.print(
                                        driver.getDriverId()
                                                + " ("
                                                + driver.getName()
                                                + "): "
                                                + driver.getChampionshipPoints()
                                                + " points"));
    }
}
