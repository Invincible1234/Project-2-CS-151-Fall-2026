package test;

import exception.DuplicateEntityException;
import f1.model.Circuit;
import f1.model.Engineer;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.RaceStrategy;
import f1.model.Team;
import java.util.HashSet;
import java.util.List;
import model.StrategyStatus;
import model.WeatherCondition;

public final class RaceStrategyFoundationVerification {
    private static int checks;

    public static void main(String[] args) {
        F1Car car = car("CAR-1");
        Engineer engineer = engineer("ENGINEER-1");
        Circuit circuit = new Circuit("CIRCUIT-1", "Circuit", 5, 12, true, WeatherCondition.RAIN, "Location");
        verifyConstruction(car, engineer, circuit);
        verifyInvalidInputs(car, engineer, circuit);
        verifyIdentity(car, engineer, circuit);
        verifyRegistration(car, engineer, circuit);
        System.out.println("PASS: Race strategy foundation (" + checks + " checks)");
    }

    private static void verifyConstruction(F1Car car, Engineer engineer, Circuit circuit) {
        RaceStrategy strategy = new RaceStrategy("  PLAN-1  ", car, engineer, circuit);
        check("PLAN-1".equals(strategy.getStrategyId()), "Strategy ID must be stripped.");
        check("CAR-1".equals(strategy.getChassisId()) && "ENGINEER-1".equals(strategy.getEngineerId()),
                "Strategy must preserve linked entity identities.");
        check(strategy.getCar() == car && strategy.getEngineer() == engineer && strategy.getCircuit() == circuit,
                "Strategy must preserve its domain associations.");
        check(strategy.getStatus() == StrategyStatus.DRAFT && strategy.getRegisteredTeam() == null,
                "Construction must create an unregistered draft.");
        check(strategy.getExpectedWeather() == WeatherCondition.RAIN, "Draft weather must reflect its circuit.");
        check(strategy.getTireCompound() == null && strategy.getEstimatedLapTimeDelta() == 0
                && strategy.getEnergyDeploymentPlan().isEmpty() && strategy.getCancellationReason().isEmpty(),
                "A draft must not claim completed planning or cancellation decisions.");
        check(strategy.getPartsUsed().isEmpty(), "Draft parts must be initialized and empty.");
        expectFailure(UnsupportedOperationException.class, () -> strategy.getPartsUsed().add("TIRES"));
        check(!car.hasScheduledStrategy() && engineer.isAvailable(),
                "Constructing a draft must not schedule the car or reserve an engineer.");
        String summary = strategy.toString();
        check(summary.contains("PLAN-1") && summary.contains("CAR-1") && summary.contains("CIRCUIT-1")
                && summary.contains("DRAFT") && !summary.contains("budget"),
                "Strategy summary must identify the draft without exposing team finances.");
    }

    private static void verifyInvalidInputs(F1Car car, Engineer engineer, Circuit circuit) {
        for (String invalid : new String[] { null, "", " \t\n", "\u2003" }) {
            expectFailure(IllegalArgumentException.class, () -> new RaceStrategy(invalid, car, engineer, circuit));
        }
        expectFailure(IllegalArgumentException.class, () -> new RaceStrategy("ID", null, engineer, circuit));
        expectFailure(IllegalArgumentException.class, () -> new RaceStrategy("ID", car, null, circuit));
        expectFailure(IllegalArgumentException.class, () -> new RaceStrategy("ID", car, engineer, null));
    }

    private static void verifyIdentity(F1Car car, Engineer engineer, Circuit circuit) {
        RaceStrategy first = new RaceStrategy("IDENTITY", car, engineer, circuit);
        RaceStrategy equivalent = new RaceStrategy(" IDENTITY ", car, engineer("OTHER-ENGINEER"), circuit);
        RaceStrategy differentId = new RaceStrategy("OTHER-ID", car, engineer, circuit);
        RaceStrategy differentChassis = new RaceStrategy("IDENTITY", car("OTHER-CAR"), engineer, circuit);
        check(first.equals(first) && first.equals(equivalent) && equivalent.equals(first),
                "Equality must use strategy ID and chassis ID rather than engineer or object identity.");
        check(first.hashCode() == equivalent.hashCode(), "Equivalent strategies must have equal hash codes.");
        check(!first.equals(differentId) && !first.equals(differentChassis)
                && !first.equals(null) && !first.equals("IDENTITY"),
                "Different strategy identities and unrelated values must not compare equal.");
        HashSet<RaceStrategy> strategies = new HashSet<>(List.of(first, equivalent, differentId, differentChassis));
        check(strategies.size() == 3, "Hash collections must recognize the compound identity.");
    }

    private static void verifyRegistration(F1Car car, Engineer engineer, Circuit circuit) {
        Team team = new Team("TEAM", "Apex", 2, 0);
        F1Car secondCar = car("CAR-2");
        team.addCar(car);
        team.addCar(secondCar);
        team.addEngineer(engineer);
        RaceStrategy first = new RaceStrategy("REGISTER", car, engineer, circuit);
        List<RaceStrategy> before = team.getStrategies();
        check(team.addStrategy(first), "Valid draft registration must succeed.");
        check(first.getRegisteredTeam() == team && team.getStrategies().get(0) == first && before.isEmpty(),
                "Registration must update ownership and preserve earlier collection snapshots.");
        expectFailure(DuplicateEntityException.class, () -> team.addStrategy(first));
        RaceStrategy duplicate = new RaceStrategy("REGISTER", car, engineer, circuit);
        expectFailure(DuplicateEntityException.class, () -> team.addStrategy(duplicate));
        check(duplicate.getRegisteredTeam() == null && team.getStrategies().size() == 1,
                "Rejected duplicates must not acquire ownership or mutate registration.");
        check(team.addStrategy(new RaceStrategy("REGISTER", secondCar, engineer, circuit)),
                "Same strategy ID on a different chassis must remain a distinct identity.");
        check(team.addStrategy(new RaceStrategy("OTHER-PLAN", car, engineer, circuit)),
                "Multiple drafts for a car must be allowed before lifecycle validation.");
        expectFailure(UnsupportedOperationException.class, () -> team.getStrategies().clear());

        RaceStrategy missingCar = new RaceStrategy("FOREIGN-CAR", car("UNREGISTERED"), engineer, circuit);
        RaceStrategy missingEngineer = new RaceStrategy("FOREIGN-ENGINEER", car, engineer("UNREGISTERED"), circuit);
        RaceStrategy carCopy = new RaceStrategy("CAR-COPY", car("CAR-1"), engineer, circuit);
        RaceStrategy engineerCopy = new RaceStrategy("ENGINEER-COPY", car, engineer("ENGINEER-1"), circuit);
        for (RaceStrategy invalid : List.of(missingCar, missingEngineer, carCopy, engineerCopy)) {
            expectFailure(IllegalArgumentException.class, () -> team.addStrategy(invalid));
            check(invalid.getRegisteredTeam() == null && team.getStrategies().size() == 3,
                    "Membership rejection must preserve both team and draft state.");
        }
        expectFailure(IllegalArgumentException.class, () -> team.addStrategy(null));
        Team foreignTeam = new Team("FOREIGN", "Other", 2, 0);
        expectFailure(IllegalStateException.class, () -> foreignTeam.addStrategy(first));
        check(first.getRegisteredTeam() == team && foreignTeam.getStrategies().isEmpty(),
                "A registered draft must not move to a second team.");
        check(first.getStatus() == StrategyStatus.DRAFT && !car.hasScheduledStrategy() && engineer.isAvailable(),
                "Registering drafts must not start strategies or reserve operational resources.");
    }

    private static F1Car car(String id) {
        return new HybridRaceCar(id, "Apex", "Hybrid", 2026, 4, 2);
    }

    private static Engineer engineer(String id) {
        return new Engineer(id, "Engineer", "HYBRID_POWER_UNIT", 5, "5551234567");
    }

    private static void expectFailure(Class<? extends RuntimeException> expected, Runnable action) {
        try {
            action.run();
            throw new AssertionError("Expected " + expected.getSimpleName() + ".");
        } catch (RuntimeException failure) {
            if (!expected.isInstance(failure)) {
                throw new AssertionError("Expected " + expected.getSimpleName() + ".", failure);
            }
            check(expected == UnsupportedOperationException.class
                    || failure.getMessage() != null && !failure.getMessage().isBlank(),
                    "Rejected actions must have a clear error message.");
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
