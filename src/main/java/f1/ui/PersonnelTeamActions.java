package f1.ui;

import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.F1Car;
import f1.service.PersonnelRegistry;
import java.util.List;
import util.Validation;

final class PersonnelTeamActions {
    private final ConsoleInput input;
    private final PersonnelRegistry registry;
    private final List<F1Car> cars;

    PersonnelTeamActions(ConsoleInput input, PersonnelRegistry registry, List<F1Car> cars) {
        this.input = input;
        this.registry = registry;
        this.cars = cars;
    }

    void registerEngineer() {
        String id = input.text("Employee ID");
        String name = input.text("Engineer name");
        EngineerSpecialty specialty = input.choice("Specialty", EngineerSpecialty.class);
        int years = input.integer("Experience years", 0, 60);
        String phone = input.read("Phone (10-15 digits, optional +)", Validation::requirePhone);
        registry.registerEngineer(id, name, specialty, years, phone);
        input.show("Engineer registered with the selected team: " + id);
    }

    void assignDriver() {
        Driver driver = input.select("Driver", registry.getDrivers(), Driver::getDriverId);
        driver.assignToCar(input.select("Car", cars, F1Car::getChassisId));
        input.show("Driver assigned.");
    }

    void assignEngineer() {
        Engineer engineer = engineer();
        engineer.assignToCar(input.select("Car", cars, F1Car::getChassisId));
        input.show("Engineer assigned.");
    }

    void releaseEngineer() {
        engineer().releaseFromCar();
        input.show("Engineer released.");
    }

    void removeDriver() {
        Driver driver = input.select("Driver", registry.getDrivers(), Driver::getDriverId);
        registry.removeDriver(driver.getDriverId());
        input.show("Driver removed from the selected team.");
    }

    void removeEngineer() {
        registry.removeEngineer(engineer().getEmployeeId());
        input.show("Engineer removed from the selected team.");
    }

    Engineer engineer() {
        return input.select("Engineer", registry.getEngineers(), Engineer::getEmployeeId);
    }
}
