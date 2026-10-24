package f1.ui;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;

import f1.model.Circuit;
import f1.model.Team;
import f1.model.UserRole;
import f1.service.CircuitRegistry;

import util.Constants;
import util.IdentityRegistry;

import java.util.ArrayList;
import java.util.List;

public final class ConsoleState {
    private final List<Team> teams = new ArrayList<>();
    private final CircuitRegistry circuits = new CircuitRegistry();
    private Team selectedTeam;
    private util.AccessContext access = new util.AccessContext(UserRole.VIEWER, "");

    public List<Team> getTeams() {
        return List.copyOf(teams);
    }

    public List<Circuit> getCircuits() {
        return circuits.getCircuits();
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
        IdentityRegistry.claim(this, team, List.of("Team ID", team.getTeamId()));
        teams.add(team);
        selectedTeam = team;
    }

    public void addCircuit(Circuit circuit) {
        circuits.addCircuit(circuit);
    }

    public CircuitRegistry getCircuitRegistry() {
        return circuits;
    }

    public UserRole getRole() {
        return access.getRole();
    }

    public util.AccessContext getAccessContext() {
        return access;
    }

    public void setAccessContext(util.AccessContext access) {
        this.access = java.util.Objects.requireNonNull(access, "Access context is required.");
    }

    public void setRole(UserRole role) {
        setAccessContext(new util.AccessContext(role, ""));
    }
}
