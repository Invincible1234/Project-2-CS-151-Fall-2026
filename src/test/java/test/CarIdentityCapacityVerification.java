package test;

import static test.StrategyCompletionFixture.check;
import static test.StrategyCompletionFixture.expectFailure;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;

import f1.model.ClassicRaceCar;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.Team;

import util.Constants;
import util.InstanceLimit;

import java.util.HashSet;

public final class CarIdentityCapacityVerification {
    public static void main(String[] args) {
        F1Car hybrid = hybrid(" SAME ");
        F1Car classic = classic("SAME");
        F1Car other = hybrid("OTHER");
        check(
                hybrid.equals(classic)
                        && classic.equals(hybrid)
                        && hybrid.hashCode() == classic.hashCode()
                        && !hybrid.equals(other)
                        && !hybrid.equals(null)
                        && !hybrid.equals("SAME"),
                "Car identity must use the normalized chassis ID across subclasses.");
        HashSet<F1Car> cars = new HashSet<>();
        cars.add(hybrid);
        cars.add(classic);
        check(
                cars.size() == 1 && cars.contains(classic),
                "Hash-based collections must recognize chassis duplicates.");
        Team team = new Team("IDENTITY", "Apex", 2, 0);
        team.addCar(hybrid);
        expectFailure(DuplicateEntityException.class, () -> team.addCar(classic));
        hybrid.updateMileage(5);
        check(
                cars.contains(hybrid) && cars.contains(classic),
                "Mutable car state must not change its hash identity.");
        check(
                hybrid.getPowerUnitType().equals("HYBRID")
                        && classic.getPowerUnitType().equals("CLASSIC")
                        && hybrid.toString().contains("SAME")
                        && hybrid.toString().contains("mileageKm=5.0")
                        && classic.toString().contains("CLASSIC"),
                "Car summaries must contain meaningful type and operating state.");

        InstanceLimit.resetForTesting();
        for (int i = 0; i < Constants.MAXIMUM_INSTANCES; i++) {
            expectFailure(
                    IllegalArgumentException.class,
                    () -> new HybridRaceCar("INVALID", "Apex", "Hybrid", 2026, 0, 0));
            expectFailure(
                    IllegalArgumentException.class,
                    () -> new ClassicRaceCar("INVALID", "Apex", "Classic", 1980, 101, 2020, 250));
            hybrid("H-" + i);
            classic("C-" + i);
        }
        expectFailure(CapacityExceededException.class, () -> hybrid("OVER-H"));
        expectFailure(CapacityExceededException.class, () -> classic("OVER-C"));
        InstanceLimit.resetForTesting();
        for (int i = 0; i < Constants.MAXIMUM_INSTANCES; i++) {
            expectFailure(IllegalArgumentException.class, () -> new Team("BAD", "Apex", 0, 0));
            new Team("TEAM-" + i, "Apex", 1, 0);
        }
        expectFailure(CapacityExceededException.class, () -> new Team("OVER", "Apex", 1, 0));
        System.out.println("Car identity and global creation limit verification passed.");
    }

    private static F1Car hybrid(String id) {
        return new HybridRaceCar(id, "Apex", "Hybrid", 2026, 4, 3);
    }

    private static F1Car classic(String id) {
        return new ClassicRaceCar(id, "Apex", "Classic", 1980, 90, 2020, 250);
    }
}
