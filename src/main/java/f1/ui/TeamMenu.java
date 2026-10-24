package f1.ui;

import f1.model.Team;

import util.Constants;

final class TeamMenu {
    private final ConsoleInput input;
    private final ConsoleState state;

    TeamMenu(ConsoleInput input, ConsoleState state) {
        this.input = input;
        this.state = state;
    }

    void open() {
        input.show("Teams: 1 Create | 2 Select | 3 Summary | 4 Performance | 0 Back");
        switch (input.integer("Team action", 0, 4)) {
            case 1 -> create();
            case 2 -> {
                state.selectTeam(input.select("Team", state.getTeams(), Team::getTeamId));
                input.show("Selected team: " + state.team().getName());
            }
            case 3 -> input.show(state.team());
            case 4 -> input.show(state.team().generateTeamPerformanceReport());
            default -> {}
        }
    }

    private void create() {
        String id =
                input.read(
                        "New team ID",
                        value -> {
                            if (state.getTeams().stream()
                                    .anyMatch(team -> team.getTeamId().equals(value))) {
                                throw new IllegalArgumentException("Team ID already exists.");
                            }
                            return value;
                        });
        String name = input.text("Team name");
        int capacity =
                input.integer(
                        "Car capacity (1-" + Constants.MAXIMUM_INSTANCES + ")",
                        1,
                        Constants.MAXIMUM_INSTANCES);
        double budget = input.amount("Starting budget", false);
        state.addTeam(new Team(id, name, capacity, budget));
        input.show("Created and selected team: " + id);
    }
}
