package f1.ui;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;
import f1.model.Circuit;
import f1.model.Team;
import f1.model.UserRole;
import java.util.ArrayList;
import java.util.List;
import util.Constants;

public final class ConsoleState {
    private final List<Team> teams = new ArrayList<>();
    private final List<Circuit> circuits = new ArrayList<>();
    private Team selectedTeam;
    private UserRole role = UserRole.VIEWER;

    public List<Team> getTeams() {
        return List.copyOf(teams);
    }

    public List<Circuit> getCircuits() {
        return List.copyOf(circuits);
    }

    public Team team() {
        if (selectedTeam == null) {
            throw new IllegalStateException("Create or select a team, or load demo data first.");
        }
        return selectedTeam;
    }

    public void selectTeam(Team team) {
        if (teams.stream().noneMatch(existing -> existing == team)) {
            throw new IllegalArgumentException("Select a registered team.");
        }
        selectedTeam = team;
    }

    public void addTeam(Team team) {
        java.util.Objects.requireNonNull(team, "Team is required.");
        if (teams.stream().anyMatch(existing -> existing.getTeamId().equals(team.getTeamId()))) {
            throw new DuplicateEntityException("Team ID already exists.");
        }
        if (teams.size() >= Constants.MAXIMUM_INSTANCES) {
            throw new CapacityExceededException("Team capacity has been reached.");
        }
        teams.add(team);
        selectedTeam = team;
    }

    public void addCircuit(Circuit circuit) {
        java.util.Objects.requireNonNull(circuit, "Circuit is required.");
        if (circuits.stream().anyMatch(existing -> existing.getCircuitId().equals(circuit.getCircuitId()))) {
            throw new DuplicateEntityException("Circuit ID already exists.");
        }
        if (circuits.size() >= Constants.MAXIMUM_INSTANCES) {
            throw new CapacityExceededException("Circuit capacity has been reached.");
        }
        circuits.add(circuit);
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = java.util.Objects.requireNonNull(role);
    }
}
