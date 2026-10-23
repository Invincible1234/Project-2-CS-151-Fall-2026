package f1.ui;

import f1.model.RaceStrategy;
import java.util.List;

final class StrategyMenu {
    private final ConsoleInput input;
    private final ConsoleState state;
    private final StrategyPlanningMenu planning;

    StrategyMenu(ConsoleInput input, ConsoleState state) {
        this.input = input;
        this.state = state;
        planning = new StrategyPlanningMenu(input, state);
    }

    void open() {
        input.show("Strategies: 1 Create | 2 Weather | 3 Tires | 4 Energy | 5 Required parts | 6 Finalize estimate");
        input.show("7 Mark ready | 8 Start | 9 Complete | 10 Cancel | 11 List | 0 Back");
        int action = input.integer("Strategy action", 0, 11);
        if (action == 0) {
            return;
        }
        if (action == 1) {
            planning.create();
            return;
        }
        List<RaceStrategy> strategies = state.team().getStrategies();
        if (action == 11) {
            if (strategies.isEmpty()) {
                input.show("No strategies registered for this team.");
            }
            strategies.forEach(strategy -> input.show(strategy + "; reason=" + strategy.getCancellationReason()));
            return;
        }
        RaceStrategy strategy = input.select("Strategy (chassis/strategy)", strategies,
                entry -> entry.getChassisId() + "/" + entry.getStrategyId());
        if (action <= 6) {
            planning.edit(strategy, action);
            return;
        }
        switch (action) {
            case 7 -> strategy.markReady();
            case 8 -> strategy.getCar().scheduleStrategy(strategy);
            case 9 -> strategy.markComplete();
            case 10 -> strategy.cancelStrategy(input.text("Cancellation reason"));
            default -> throw new IllegalArgumentException("Unknown strategy action.");
        }
        input.show("Strategy " + strategy.getStrategyId() + ": " + strategy.getStatus());
    }
}
