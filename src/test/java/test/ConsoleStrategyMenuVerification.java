package test;

import static test.ConsoleSessionFixture.action;
import static test.ConsoleSessionFixture.check;
import static test.ConsoleSessionFixture.run;

import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.UserRole;
import f1.ui.ConsoleState;

import model.StrategyStatus;

public final class ConsoleStrategyMenuVerification {
    public static void main(String[] args) {
        ConsoleState state = new ConsoleState();
        String id = "H1/PLAN";
        String script =
                "4\n2\n1\nPLAN\nmissing\nH1\nE1\nTRACK1\n"
                        + action(8, id)
                        + action(2, id, "CLEAR")
                        + action(3, id, "SOFT")
                        + action(4, id, "DEPLOY=5;RECOVER=0")
                        + action(4, id, "DEPLOY=2;RECOVER=1")
                        + action(5, id, "TIRES", "1")
                        + action(6, id)
                        + action(7, id)
                        + action(8, id)
                        + "5\nENGINEER\n3\n5\nTIRES\n10\n"
                        + action(9, id)
                        + action(9, id)
                        + "2\n11\n1\n4\nEXIT\n";
        String transcript = run(script, state);
        RaceStrategy strategy = state.team().getStrategies().get(0);
        HybridRaceCar car = (HybridRaceCar) strategy.getCar();
        check(
                strategy.getStatus() == StrategyStatus.COMPLETED && car.getTotalMileage() == 5,
                "The menu must support the entire hybrid strategy lifecycle exactly once.");
        check(
                car.getCurrentEnergyMJ() == 2
                        && car.getAssignedEngineer() == null
                        && car.getAssignedDriver() == null,
                "UI completion must apply energy and release personnel through the model.");
        check(
                state.team().getPartsInventory(UserRole.ENGINEER).get("TIRES") == 9,
                "UI consumption must preserve reservations and completion must consume once.");
        check(
                transcript.contains("Only a ready strategy can start")
                        && transcript.contains("at most 4 MJ")
                        && transcript.contains("Only an active strategy can complete"),
                "Invalid transitions and energy must be handled.");
        check(
                transcript.contains("COMPLETED") && transcript.contains("Total mileage (km): 5.00"),
                "Status listing and performance must reflect the completed session.");
        util.InstanceLimit.resetForTesting();
        verifyClassicCancellation();
        System.out.println("PASS: Console strategy planning, completion, and cancellation");
    }

    private static void verifyClassicCancellation() {
        ConsoleState state = new ConsoleState();
        String id = "C1/CLASSIC";
        String script =
                "4\n2\n1\nCLASSIC\nC1\nE2\nTRACK1\n"
                        + action(2, id, "CLEAR")
                        + action(4, id, "DEPLOY=1;RECOVER=0")
                        + action(5, id, "TIRES", "1")
                        + action(6, id)
                        + action(7, id)
                        + action(8, id)
                        + action(10, id, "Weather shutdown")
                        + "2\n11\nEXIT\n";
        String transcript = run(script, state);
        RaceStrategy strategy = state.team().getStrategies().get(0);
        check(
                strategy.getStatus() == StrategyStatus.CANCELLED
                        && strategy.getCancellationReason().equals("Weather shutdown")
                        && strategy.getCar().getTotalMileage() == 0,
                "Classic cancellation must be reachable and preserve its reason without adding"
                        + " mileage.");
        check(
                state.team().getPartsInventory(UserRole.ENGINEER).get("TIRES") == 10
                        && !strategy.getCar().hasActiveAssignments(),
                "Cancellation must release crew and preserve stock.");
        check(
                transcript.contains("Energy deployment plans require a hybrid car")
                        && transcript.contains("reason=Weather shutdown"),
                "Invalid plans and cancellation status must be visible.");
    }
}
