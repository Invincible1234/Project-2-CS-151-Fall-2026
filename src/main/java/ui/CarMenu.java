package ui;

import f1.model.AeroConfiguration;
import f1.model.Circuit;
import f1.model.ClassicRaceCar;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.HybridUpgrade;
import f1.model.SessionType;
import f1.model.Team;

import java.util.List;
import java.util.Objects;

public final class CarMenu {
    private final ConsoleInput input;
    private final Team team;
    private final List<Team> teams;
    private final List<Circuit> circuits;

    public CarMenu(ConsoleInput input, Team team) {
        this(input, team, List.of(team), List.of());
    }

    public CarMenu(ConsoleInput input, Team team, List<Team> teams, List<Circuit> circuits) {
        this.teams = List.copyOf(teams);
        this.circuits = List.copyOf(circuits);
        this.input = Objects.requireNonNull(input, "Shared input is required.");
        this.team = Objects.requireNonNull(team, "Team is required.");
    }

    public void run() {
        while (open()) {}
    }

    public boolean open() {
        input.print(
                "Cars: 1 Create hybrid | 2 Create classic | 3 List | 4 Prepare | 5 Mileage | 6"
                    + " Upgrade | 7 Remove | 8 Decommission classic | 9 Service | 10 Details | 11"
                    + " Advanced operations | 0 Back");
        int option = input.integer("Choose a car action", 0, 11);
        if (option == 0) {
            return false;
        }
        input.action(() -> perform(option));
        return true;
    }

    private void perform(int option) {
        if (option == 1 || option == 2) {
            createCar(option == 1);
            return;
        }
        if (option == 3) {
            if (team.getCars().isEmpty()) {
                input.print("No cars registered.");
            }
            for (F1Car car : team.getCars()) {
                input.print(car.toString());
            }
            return;
        }
        F1Car car = selectCar();
        switch (option) {
            case 4 ->
                    input.print(
                            car.prepareForSession(input.choice("Session", SessionType.class))
                                    ? "Car prepared."
                                    : "Car could not be prepared; check service, readiness, and"
                                            + " commitments.");
            case 5 -> {
                car.updateMileage(input.decimal("Distance to add in km", 0, Double.MAX_VALUE));
                input.print("Mileage updated to " + car.getTotalMileage() + " km.");
            }
            case 6 -> upgrade(car);
            case 7 -> input.print(team.removeCar(car) ? "Car removed." : "Car was not removed.");
            case 8 -> {
                if (!(car instanceof ClassicRaceCar classic)) {
                    throw new IllegalArgumentException(
                            "Only classic cars can be decommissioned here.");
                }
                if (input.yesNo("Permanently decommission " + car.getChassisId() + "?")) {
                    classic.decommission();
                    input.print("Classic car decommissioned.");
                } else {
                    input.print("Decommission cancelled.");
                }
            }
            case 9 -> {
                car.performService(input.text("Service notes"));
                input.print("Service recorded.");
            }
            case 10 -> {
                input.print(car.getRegulatoryComplianceNotes());
                input.print("Next maintenance: " + car.getNextMaintenanceDue());
                input.print("Out of service: " + car.getOutOfServiceReason());
                input.print(
                        "Power unit: "
                                + car.getPowerUnitType()
                                + "; aero: "
                                + car.getCurrentAeroConfig()
                                + "; setup date: "
                                + car.getLastSetupDate()
                                + "; strategy: "
                                + car.getScheduledStrategy());
                input.print(
                        "Last service: "
                                + car.getLastServiceDate()
                                + "; mileage since service: "
                                + car.getMileageSinceService()
                                + " km; notes: "
                                + car.getLastServiceNotes());
            }
            case 11 -> new CarOperationsMenu(input, team, car, teams, circuits).open();
            default -> throw new IllegalArgumentException("Unknown car action.");
        }
    }

    private F1Car selectCar() {
        if (team.getCars().isEmpty()) {
            throw new IllegalStateException("Register a car first.");
        }
        String id = input.text("Chassis ID");
        return team.getCars().stream()
                .filter(car -> car.getChassisId().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No car has that chassis ID."));
    }

    private void createCar(boolean hybrid) {
        if (team.getCars().size() >= team.getCapacity()) {
            throw new IllegalStateException("Team car capacity has been reached.");
        }
        String id = input.text("Unique chassis ID");
        if (team.getCars().stream().anyMatch(car -> car.getChassisId().equals(id))) {
            throw new IllegalArgumentException("Chassis ID is already registered.");
        }
        String model = input.text("Model");
        int year = input.integer("Car year", 1950, input.today().getYear());
        F1Car car;
        if (hybrid) {
            double capacity =
                    input.decimal("Battery capacity in MJ", Double.MIN_VALUE, Double.MAX_VALUE);
            double charge = input.decimal("Initial battery charge in MJ", 0, capacity);
            double power = input.decimal("MGU-K power in kW", Double.MIN_VALUE, 350);
            car =
                    new HybridRaceCar(
                            id,
                            team.getName(),
                            model,
                            year,
                            capacity,
                            charge,
                            power,
                            input.clock());
        } else {
            int originality = input.integer("Originality score", 0, 100);
            int restoration =
                    input.integer("Completed restoration year", year, input.today().getYear());
            double power = input.decimal("Power limit in kW", Double.MIN_VALUE, Double.MAX_VALUE);
            car =
                    new ClassicRaceCar(
                            id,
                            team.getName(),
                            model,
                            year,
                            originality,
                            restoration,
                            power,
                            input.clock());
        }
        team.addCar(car);
        input.print("Car registered: " + id);
    }

    private void upgrade(F1Car car) {
        int type = input.integer("Upgrade: 1 Aerodynamics | 2 Hybrid power unit", 1, 2);
        if (type == 1) {
            car.applyAeroUpgrade(
                    input.choice("Aero configuration", AeroConfiguration.class).name());
        } else {
            if (!(car instanceof HybridRaceCar hybrid)) {
                throw new IllegalArgumentException("A hybrid power upgrade requires a hybrid car.");
            }
            hybrid.applyHybridUpgrade(input.choice("Hybrid upgrade", HybridUpgrade.class).name());
        }
        input.print("Upgrade applied; prepare the car again before a session.");
    }
}
