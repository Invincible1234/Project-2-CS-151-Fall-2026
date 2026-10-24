package test;

import static test.StrategyCompletionFixture.check;
import static test.StrategyCompletionFixture.expectFailure;
import static test.StrategyCompletionFixture.fixture;

import exception.InsufficientResourcesException;
import exception.InvalidStrategyException;

import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.RaceStrategy;
import f1.model.SessionType;
import f1.model.UserRole;
import f1.ui.ConsoleState;

import model.StrategyStatus;

import java.util.Map;

public final class EngineerWorkflowVerification {
    public static void main(String[] args) {
        verifySetup();
        verifyRequests();
        verifyConsolePurchase();
        System.out.println("Engineer setup and parts workflow verification passed.");
    }

    private static void verifySetup() {
        var context = fixture(null, true, 0);
        context.car().prepareForSession(SessionType.PRACTICE);
        context.car().startSession();
        context.car().completeSession();
        RaceStrategy plan =
                new RaceStrategy(
                        "ENGINEER-SETUP",
                        context.car(),
                        context.engineer(),
                        context.strategy().getCircuit());
        context.team().addStrategy(plan);
        Engineer stranger =
                new Engineer(
                        "STRANGER",
                        "Engineer",
                        EngineerSpecialty.HYBRID_POWER_UNIT,
                        5,
                        "5551234567");
        RaceStrategy wrongEngineer =
                new RaceStrategy("WRONG-CREW", context.car(), stranger, plan.getCircuit());
        check(
                !context.engineer().completeSetup(wrongEngineer),
                "An assigned engineer must reject a plan naming a different engineer on the same"
                        + " car.");
        check(
                !stranger.completeSetup(plan) && !context.engineer().completeSetup(null),
                "An unassigned engineer or missing plan cannot complete setup.");
        expectFailure(InvalidStrategyException.class, () -> context.engineer().completeSetup(plan));
        check(
                !context.car().isRaceReady() && !plan.hasFinalizedLapTimeEstimate(),
                "Incomplete planning must not partially prepare the car or finalize the estimate.");
        plan.adjustForWeather(context.strategy().getCircuit().getTypicalWeather());
        plan.addEnergyPlan("DEPLOY=2;RECOVER=1");
        plan.addRequiredParts("TIRES", 1);
        check(
                context.engineer().completeSetup(plan)
                        && context.car().isRaceReady()
                        && context.car().getPreparedSessionType() == SessionType.RACE
                        && plan.hasFinalizedLapTimeEstimate()
                        && plan.getStatus() == StrategyStatus.DRAFT,
                "Setup must prepare the car and estimate without skipping explicit strategy"
                        + " readiness.");
        plan.markReady();
        check(
                !context.engineer().completeSetup(plan),
                "A ready plan must not be edited through setup.");
        context.car().scheduleStrategy("ENGINEER-SETUP");
        check(
                plan.getStatus() == StrategyStatus.ACTIVE,
                "ID-based scheduling must start the registered plan.");
        expectFailure(IllegalStateException.class, () -> context.engineer().releaseFromCar());
        plan.cancelStrategy("Setup test finished");
        expectFailure(
                IllegalArgumentException.class, () -> context.car().scheduleStrategy("UNKNOWN"));
    }

    private static void verifyRequests() {
        var context = fixture(null, false, 0);
        Engineer engineer = context.engineer();
        expectFailure(IllegalArgumentException.class, () -> engineer.requestParts("MISSING", 1));
        expectFailure(IllegalArgumentException.class, () -> engineer.requestParts("TIRES", 0));
        engineer.requestParts(" tires ", 2);
        engineer.requestParts("TIRES", 1);
        Map<String, Integer> snapshot = engineer.requestedPartsFor(UserRole.ENGINEER);
        check(
                snapshot.equals(Map.of("TIRES", 3)),
                "Requests must accumulate by normalized part name.");
        try {
            snapshot.clear();
            throw new AssertionError("Request snapshots must be immutable.");
        } catch (UnsupportedOperationException expected) {
        }
        expectFailure(SecurityException.class, () -> engineer.requestedPartsFor(UserRole.VIEWER));
        expectFailure(
                SecurityException.class,
                () -> engineer.fulfillPartsRequest("TIRES", 1, UserRole.DRIVER));
        double budget = context.team().getBudget(UserRole.TEAM_PRINCIPAL);
        engineer.fulfillPartsRequest("TIRES", 1, UserRole.ENGINEER);
        check(
                engineer.requestedPartsFor(UserRole.ENGINEER).get("TIRES") == 2
                        && snapshot.get("TIRES") == 3
                        && context.team().getBudget(UserRole.TEAM_PRINCIPAL) == budget - 1200
                        && context.team().getPartsInventory(UserRole.ENGINEER).get("TIRES") == 4,
                "Partial fulfillment must update the request and purchase stock exactly once.");
        expectFailure(
                IllegalArgumentException.class,
                () -> engineer.fulfillPartsRequest("TIRES", 3, UserRole.TEAM_PRINCIPAL));
        engineer.requestParts("HYBRID_COMPONENTS", Integer.MAX_VALUE);
        expectFailure(
                IllegalArgumentException.class,
                () -> engineer.requestParts("HYBRID_COMPONENTS", 1));
        double before = context.team().getBudget(UserRole.TEAM_PRINCIPAL);
        var inventory = context.team().getPartsInventory(UserRole.ENGINEER);
        var requests = engineer.requestedPartsFor(UserRole.ENGINEER);
        expectFailure(
                InsufficientResourcesException.class,
                () -> engineer.fulfillPartsRequest("HYBRID_COMPONENTS", 10, UserRole.ENGINEER));
        check(
                context.team().getBudget(UserRole.TEAM_PRINCIPAL) == before
                        && inventory.equals(context.team().getPartsInventory(UserRole.ENGINEER))
                        && requests.equals(engineer.requestedPartsFor(UserRole.ENGINEER)),
                "Insufficient budget must preserve requests, stock, and balance.");
        context.strategy().markReady();
        context.strategy().startStrategy();
        expectFailure(IllegalStateException.class, engineer::releaseFromCar);
        check(
                requests.equals(engineer.requestedPartsFor(UserRole.ENGINEER)),
                "A rejected release must preserve outstanding requests.");
        context.strategy().cancelStrategy("Release after cancellation");
        check(
                engineer.requestedPartsFor(UserRole.ENGINEER).isEmpty(),
                "Successful release must clear car-specific pending requests.");
    }

    private static void verifyConsolePurchase() {
        ConsoleState state = new ConsoleState();
        String output =
                ConsoleSessionFixture.run(
                        "4\n9\n15\nE1\nTIRES\n2\n5\nTEAM_PRINCIPAL\n"
                                + "3\n6\nE1\nTIRES\n1\nEXIT\n",
                        state);
        var engineer = state.team().getEngineers().get(0);
        check(
                output.contains("Request fulfilled")
                        && engineer.requestedPartsFor(UserRole.ENGINEER).get("TIRES") == 1
                        && state.team().getPartsInventory(UserRole.ENGINEER).get("TIRES") == 11
                        && state.team().getBudget(UserRole.TEAM_PRINCIPAL) == 86800,
                "The shared resource menu must fulfill the canonical engineer's request once.");
        f1.model.Team other = new f1.model.Team("OTHER-REQUEST-TEAM", "Other", 1, 0);
        other.addEngineer(engineer);
        state.addTeam(other);
        String rejected = ConsoleSessionFixture.run("3\n6\nE1\nEXIT\n", state);
        check(
                rejected.contains("assigned to this team")
                        && engineer.requestedPartsFor(UserRole.ENGINEER).get("TIRES") == 1
                        && state.getTeams().get(0).getBudget(UserRole.TEAM_PRINCIPAL) == 86800,
                "A shared engineer must not charge another team from the selected team's resource"
                        + " menu.");
    }
}
