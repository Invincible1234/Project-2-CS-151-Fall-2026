package test;

import exception.CapacityExceededException;
import f1.model.Circuit;
import f1.model.Engineer;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.Team;
import model.WeatherCondition;
import util.Constants;

public final class RaceStrategyCapacityVerification {
    public static void main(String[] args) {
        Team team = new Team("CAPACITY", "Apex", 1, 0);
        F1Car car = new HybridRaceCar("CAR", "Apex", "Hybrid", 2026, 4, 2);
        Engineer engineer = new Engineer("ENGINEER", "Engineer", "HYBRID_POWER_UNIT", 5, "5551234567");
        Circuit circuit = new Circuit("CIRCUIT", "Circuit", 5, 12, true, WeatherCondition.CLEAR, "Location");
        team.addCar(car);
        team.addEngineer(engineer);
        try {
            new RaceStrategy(" ", car, engineer, circuit);
            throw new AssertionError("Invalid strategy ID was accepted.");
        } catch (IllegalArgumentException expected) {
        }
        for (int i = 0; i < Constants.MAXIMUM_INSTANCES; i++) {
            if (!team.addStrategy(new RaceStrategy("PLAN-" + i, car, engineer, circuit))) {
                throw new AssertionError("Strategy within capacity was rejected.");
            }
        }
        try {
            new RaceStrategy("OVERFLOW", car, engineer, circuit);
            throw new AssertionError("Strategy instance limit was exceeded.");
        } catch (CapacityExceededException expected) {
            if (team.getStrategies().size() != Constants.MAXIMUM_INSTANCES
                    || car.hasScheduledStrategy() || !engineer.isAvailable()) {
                throw new AssertionError("Capacity rejection changed registration or resource state.");
            }
        }
        System.out.println("PASS: Race strategy capacity and unchanged state");
    }
}
