package f1.ui;

import f1.model.UserRole;
import java.io.InputStreamReader;

public final class ConsoleApplication {
    private final ConsoleInput input;
    private final ConsoleState state;

    public ConsoleApplication(ConsoleInput input, ConsoleState state) {
        this.input = java.util.Objects.requireNonNull(input);
        this.state = java.util.Objects.requireNonNull(state);
    }

    public static void main(String[] args) {
        new ConsoleApplication(new ConsoleInput(new InputStreamReader(System.in), System.out), new ConsoleState()).run();
    }

    public void run() {
        input.show("F1 Team Operations - enter EXIT at any prompt to quit.");
        try {
            while (true) {
                input.show("Main menu | Role: " + state.getRole());
                input.show("1 Teams | 2 Strategies | 3 Inventory and budget | 4 Load demo | 5 Session role | 0 EXIT");
                input.show("6 Cars | 7 Circuits | 8 Race results | 9 Personnel");
                try {
                    dispatch(input.integer("Main action", 0, 9));
                } catch (ConsoleInput.ExitRequestedException | ui.ConsoleInput.ExitRequested exit) {
                    throw exit;
                } catch (RuntimeException error) {
                    input.show("Action failed: " + error.getMessage());
                }
            }
        } catch (ConsoleInput.ExitRequestedException | ui.ConsoleInput.ExitRequested exit) {
            input.show("Goodbye.");
        }
    }

    private void dispatch(int action) {
        switch (action) {
            case 0 -> throw new ConsoleInput.Exit();
            case 1 -> new TeamMenu(input, state).open();
            case 2 -> new StrategyMenu(input, state).open();
            case 3 -> new ResourceMenu(input, state).open();
            case 4 -> {
                DemoData.load(state);
                input.show("Demo selected: team DEMO, cars H1/C1, engineers E1/E2, circuit TRACK1.");
            }
            case 5 -> {
                state.setRole(input.choice("Session role", UserRole.class));
                input.show("Session role: " + state.getRole());
            }
            case 6 -> new ui.CarMenu(new ui.ConsoleInput(input), state.team()).open();
            case 7 -> new ui.CircuitMenu(new ui.ConsoleInput(input), state.getCircuitRegistry()).open();
            case 8 -> new ui.RaceResultMenu(new ui.ConsoleInput(input), state.team()).open();
            case 9 -> {
                PersonnelMenu personnel = new PersonnelMenu(state.team(), input);
                state.setRole(personnel.open(state.getRole()));
            }
            default -> throw new IllegalArgumentException("Unknown main action.");
        }
    }
}
