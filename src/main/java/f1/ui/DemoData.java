package f1.ui;

import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.SessionType;
import f1.model.Team;

import model.WeatherCondition;

import util.Constants;

import java.time.Clock;

final class DemoData {
    static void load(ConsoleState state, Clock clock) {
        if (state.getTeams().stream().anyMatch(team -> team.getTeamId().equals("DEMO"))
                || state.getCircuits().stream()
                        .anyMatch(circuit -> circuit.getCircuitId().equals("TRACK1"))) {
            throw new IllegalStateException("Demo IDs are already in use.");
        }
        if (state.getTeams().size() >= Constants.MAXIMUM_INSTANCES
                || state.getCircuits().size() >= Constants.MAXIMUM_INSTANCES) {
            throw new IllegalStateException("Make room for the demo team and circuit first.");
        }
        Team team = new Team("DEMO", "Apex", 2, 100000);
        addCrew(
                team,
                new HybridRaceCar(
                        "H1",
                        "Apex",
                        "Hybrid",
                        2026,
                        4,
                        3,
                        f1.model.HybridUpgrade.BALANCED.getPowerKw(),
                        clock),
                "1",
                EngineerSpecialty.HYBRID_POWER_UNIT);
        addCrew(
                team,
                new ClassicRaceCar("C1", "Apex", "Classic", 1980, 90, 2020, 250, clock),
                "2",
                EngineerSpecialty.CLASSIC_RESTORATION);
        team.addParts("TIRES", 10);
        Circuit circuit =
                new Circuit(
                        "TRACK1",
                        "Demo circuit",
                        5,
                        12,
                        true,
                        WeatherCondition.CLEAR,
                        "Demo venue");
        state.addCircuit(circuit);
        state.addTeam(team);
    }

    private static void addCrew(Team team, F1Car car, String suffix, EngineerSpecialty specialty) {
        Driver driver =
                new Driver(
                        "D" + suffix,
                        "Demo driver " + suffix,
                        "LICENSE" + suffix,
                        25,
                        "5551234567");
        driver.setMedicalClearance(true, "Cleared");
        Engineer engineer =
                new Engineer("E" + suffix, "Demo engineer " + suffix, specialty, 5, "5551234567");
        team.addCar(car);
        team.addDriver(driver);
        team.addEngineer(engineer);
        team.assignDriver(driver, car);
        engineer.assignToCar(car);
        car.prepareForSession(SessionType.RACE);
    }
}
