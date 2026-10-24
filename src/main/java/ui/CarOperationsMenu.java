package ui;

import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.Engineer;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.Team;

import model.WeatherCondition;

import java.util.List;

final class CarOperationsMenu {
    private final ConsoleInput input;
    private final Team team;
    private final F1Car car;
    private final List<Team> teams;
    private final List<Circuit> circuits;

    CarOperationsMenu(
            ConsoleInput input, Team team, F1Car car, List<Team> teams, List<Circuit> circuits) {
        this.input = input;
        this.team = team;
        this.car = car;
        this.teams = teams;
        this.circuits = circuits;
    }

    void open() {
        input.print(
                "Advanced car operations: 1 Out of service | 2 Start session | 3 Complete session |"
                        + " 4 Classic operations | 5 Hybrid operations | 6 Predict lap impact | 0"
                        + " Back");
        switch (input.integer("Advanced action", 0, 6)) {
            case 0 -> {
                return;
            }
            case 1 -> car.markOutOfService(input.text("Out-of-service reason"));
            case 2 -> car.startSession();
            case 3 -> car.completeSession();
            case 4 -> {
                classicOperations();
                return;
            }
            case 5 -> {
                hybridOperations();
                return;
            }
            case 6 -> {
                predict();
                return;
            }
            default -> throw new IllegalArgumentException("Unknown car operation.");
        }
        input.print("Car updated: " + car);
    }

    private void classicOperations() {
        if (!(car instanceof ClassicRaceCar classic)) {
            throw new IllegalArgumentException(
                    "Choose a classic car for preservation or restoration.");
        }
        input.print(
                "Classic: 1 Originality change | 2 Schedule restoration | 3 Complete restoration |"
                        + " 4 Preservation mode | 5 Transfer ownership | 6 History | 7 Release to"
                        + " outside owner | 0 Back");
        switch (input.integer("Classic action", 0, 7)) {
            case 0 -> {
                return;
            }
            case 1 ->
                    classic.updateOriginalityScore(input.integer("Originality change", -100, 100));
            case 2 ->
                    classic.scheduleRestoration(
                            input.integer("Restoration year", input.today().getYear(), 9999));
            case 3 -> {
                int change = input.integer("Originality change after restoration", -100, 100);
                classic.completeRestoration(change, input.text("Restoration service notes"));
            }
            case 4 -> {
                if (!input.yesNo("Permanently enable preservation mode for this runtime?")) {
                    input.print("Preservation cancelled.");
                    return;
                }
                classic.applyPreservationMode();
            }
            case 5 -> {
                teams.forEach(
                        candidate ->
                                input.print(candidate.getTeamId() + ": " + candidate.getName()));
                String id = input.text("Registered destination team ID");
                Team destination =
                        teams.stream()
                                .filter(candidate -> candidate.getTeamId().equals(id))
                                .findFirst()
                                .orElseThrow(
                                        () -> new IllegalArgumentException("No team has that ID."));
                classic.transferOwnershipRecord(destination);
                input.print("Ownership transferred to " + destination.getName() + ".");
                return;
            }
            case 6 -> {
                input.print(classic.getOwnershipRecord());
                input.print(
                        "Originality: "
                                + classic.getOriginalityScore()
                                + "/100; restoration year: "
                                + classic.getRestorationYear()
                                + "; scheduled: "
                                + classic.getScheduledRestorationYear()
                                + "; preservation: "
                                + classic.isPreservationModeEnabled()
                                + "; power: "
                                + classic.getPowerLimitKw()
                                + " kW");
                return;
            }
            case 7 -> {
                String owner = input.text("Outside owner name");
                if (input.yesNo(
                        "Remove this car from the team and transfer it outside the system?")) {
                    classic.transferOwnershipRecord(owner);
                    input.print("Car released: " + classic.getOwnershipRecord());
                } else {
                    input.print("Ownership release cancelled.");
                }
                return;
            }
            default -> throw new IllegalArgumentException("Unknown classic action.");
        }
        input.print("Classic car updated: " + classic);
    }

    private void hybridOperations() {
        if (!(car instanceof HybridRaceCar hybrid)) {
            throw new IllegalArgumentException("Hybrid energy operations require a hybrid car.");
        }
        input.print(
                "Hybrid: 1 Deploy boost | 2 Recover energy | 3 Complete lap | 4 Start next lap | 5"
                        + " Reset counters | 6 Assign specialist | 7 Release specialist | 8 Energy"
                        + " status | 0 Back");
        switch (input.integer("Hybrid action", 0, 8)) {
            case 0 -> {
                return;
            }
            case 1 ->
                    hybrid.deployEnergyBoost(
                            input.decimal("Deployment in MJ", Double.MIN_VALUE, 4));
            case 2 ->
                    hybrid.chargeBatteryFromRecovery(
                            input.decimal("Recovery in MJ", Double.MIN_VALUE, 9));
            case 3 -> hybrid.completeLap();
            case 4 -> hybrid.startNextLap();
            case 5 -> hybrid.resetEnergyState();
            case 6 -> {
                team.getEngineers()
                        .forEach(
                                engineer ->
                                        input.print(
                                                engineer.getEmployeeId()
                                                        + ": "
                                                        + engineer.getName()
                                                        + " ("
                                                        + engineer.getSpecialty()
                                                        + ")"));
                String id = input.text("Registered hybrid specialist employee ID");
                Engineer engineer =
                        team.getEngineers().stream()
                                .filter(candidate -> candidate.getEmployeeId().equals(id))
                                .findFirst()
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "No engineer has that ID."));
                hybrid.lockForSpecialist(engineer);
            }
            case 7 -> hybrid.releaseSpecialist();
            case 8 -> {
                showEnergy(hybrid);
                return;
            }
            default -> throw new IllegalArgumentException("Unknown hybrid action.");
        }
        input.print("Hybrid operation completed.");
        showEnergy(hybrid);
    }

    private void showEnergy(HybridRaceCar hybrid) {
        input.print(
                "Battery: "
                        + hybrid.getCurrentEnergyMJ()
                        + "/"
                        + hybrid.getBatteryCapacityMJ()
                        + " MJ; deployed: "
                        + hybrid.getDeployedThisLapMJ()
                        + " MJ; recovered: "
                        + hybrid.getRecoveredThisLapMJ()
                        + " MJ; lap: "
                        + hybrid.getCurrentLapNumber()
                        + "; lap completed: "
                        + hybrid.isLapCompleted()
                        + "; upgrade: "
                        + hybrid.getHybridUpgrade());
    }

    private void predict() {
        if (circuits.isEmpty()) {
            throw new IllegalStateException(
                    "Register a circuit before predicting lap performance.");
        }
        circuits.forEach(circuit -> input.print(circuit.getCircuitId() + ": " + circuit.getName()));
        String id = input.text("Circuit ID");
        Circuit circuit =
                circuits.stream()
                        .filter(candidate -> candidate.getCircuitId().equals(id))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("No circuit has that ID."));
        WeatherCondition weather = input.choice("Prediction weather", WeatherCondition.class);
        double delta = car.calculateLapTimeDelta(circuit, weather);
        input.print(
                "Estimated lap impact: "
                        + delta
                        + " seconds; projected lap: "
                        + (circuit.estimateBaseLapTime() + delta)
                        + " seconds.");
    }
}
