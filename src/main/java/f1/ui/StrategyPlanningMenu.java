package f1.ui;

import f1.model.Circuit;
import f1.model.Engineer;
import f1.model.F1Car;
import f1.model.RaceStrategy;
import f1.model.Team;

import model.TireCompound;
import model.WeatherCondition;

import util.PartsCatalog;

import java.util.List;

final class StrategyPlanningMenu {
    private final ConsoleInput input;
    private final ConsoleState state;

    StrategyPlanningMenu(ConsoleInput input, ConsoleState state) {
        this.input = input;
        this.state = state;
    }

    void create() {
        Team team = state.team();
        String id = input.text("New strategy ID");
        F1Car car = input.select("Car chassis", team.getCars(), F1Car::getChassisId);
        Engineer engineer = input.select("Engineer", team.getEngineers(), Engineer::getEmployeeId);
        Circuit circuit = input.select("Circuit", state.getCircuits(), Circuit::getCircuitId);
        team.addStrategy(new RaceStrategy(id, car, engineer, circuit));
        input.show("Created draft strategy: " + id + " for " + car.getChassisId());
    }

    void edit(RaceStrategy strategy, int action) {
        switch (action) {
            case 2 ->
                    strategy.adjustForWeather(
                            input.choice("Expected weather", WeatherCondition.class));
            case 3 ->
                    strategy.selectTireCompound(input.choice("Tire compound", TireCompound.class));
            case 4 -> strategy.addEnergyPlan(input.text("Energy plan DEPLOY=<MJ>;RECOVER=<MJ>"));
            case 5 -> {
                String part =
                        input.select(
                                "Required part",
                                List.copyOf(PartsCatalog.getPrices().keySet()),
                                value -> value);
                strategy.addRequiredParts(
                        part, input.integer("Required quantity", 1, Integer.MAX_VALUE));
            }
            case 6 ->
                    input.show(
                            "Estimated lap delta (seconds): "
                                    + strategy.finalizeLapTimeImpact(strategy.getCircuit()));
            default -> throw new IllegalArgumentException("Unknown planning action.");
        }
        input.show("Strategy updated: " + strategy.getStrategyId());
    }
}
