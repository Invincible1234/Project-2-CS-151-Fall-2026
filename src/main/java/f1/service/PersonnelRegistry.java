package f1.service;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;

import f1.model.Driver;
import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.Team;

import util.Constants;
import util.IdentityRegistry;
import util.Validation;

import java.util.ArrayList;
import java.util.List;

public final class PersonnelRegistry {
    private final List<Driver> drivers = new ArrayList<>();
    private final List<Engineer> engineers = new ArrayList<>();

    private final Team team;

    public PersonnelRegistry() {
        team = null;
    }

    public PersonnelRegistry(Team team) {
        this.team = java.util.Objects.requireNonNull(team, "Team is required.");
        drivers.addAll(team.getDrivers());
        engineers.addAll(team.getEngineers());
    }

    public Driver registerDriver(
            String driverId, String name, String licenseNumber, int age, String phone) {
        String id = Validation.requireText(driverId, "Driver ID");
        String license = Validation.requireText(licenseNumber, "License number");
        if (matchDriverByLicense(license) != null) {
            throw new DuplicateEntityException(
                    "A driver with this license number is already registered.");
        }
        if (matchDriverById(id) != null) {
            throw new DuplicateEntityException("A driver with this id is already registered.");
        }
        ensureRoom(drivers, "driver");
        Driver driver = new Driver(driverId, name, licenseNumber, age, phone);
        if (team != null) {
            team.addDriver(driver);
        } else {
            IdentityRegistry.claim(
                    this,
                    driver,
                    List.of("Driver license", driver.getLicenseNumber()),
                    List.of("Driver ID", driver.getDriverId()));
        }
        drivers.add(driver);
        return driver;
    }

    public Engineer registerEngineer(
            String employeeId,
            String name,
            EngineerSpecialty specialty,
            int experienceYears,
            String phone) {
        String id = Validation.requireText(employeeId, "Employee ID");
        if (matchEngineerById(id) != null) {
            throw new DuplicateEntityException(
                    "An engineer with this employee id is already registered.");
        }
        ensureRoom(engineers, "engineer");
        Engineer engineer = new Engineer(employeeId, name, specialty, experienceYears, phone);
        if (team != null) {
            team.addEngineer(engineer);
        } else {
            IdentityRegistry.claim(
                    this, engineer, List.of("Employee ID", engineer.getEmployeeId()));
        }
        engineers.add(engineer);
        return engineer;
    }

    public Driver findDriverById(String driverId) {
        return matchDriverById(Validation.requireText(driverId, "Driver ID"));
    }

    public Driver findDriverByLicense(String licenseNumber) {
        return matchDriverByLicense(Validation.requireText(licenseNumber, "License number"));
    }

    public Engineer findEngineerByEmployeeId(String employeeId) {
        return matchEngineerById(Validation.requireText(employeeId, "Employee ID"));
    }

    public List<Driver> getDrivers() {
        return List.copyOf(drivers);
    }

    public List<Engineer> getEngineers() {
        return List.copyOf(engineers);
    }

    public void removeDriver(String driverId) {
        Driver driver = findDriverById(driverId);
        if (driver == null) {
            throw new IllegalArgumentException("No driver is registered with this id.");
        }
        if (team == null) {
            driver.releaseAllAssignments();
            IdentityRegistry.release(this, driver);
        } else {
            team.removeDriver(driver);
        }
        drivers.remove(driver);
    }

    public void removeEngineer(String employeeId) {
        Engineer engineer = findEngineerByEmployeeId(employeeId);
        if (engineer == null) {
            throw new IllegalArgumentException("No engineer is registered with this id.");
        }
        if (team == null) {
            engineer.releaseFromCar();
            IdentityRegistry.release(this, engineer);
        } else {
            team.removeEngineer(engineer);
        }
        engineers.remove(engineer);
    }

    private void ensureRoom(List<?> people, String label) {
        if (people.size() >= Constants.MAXIMUM_INSTANCES) {
            throw new CapacityExceededException(
                    "Cannot register another "
                            + label
                            + ". The limit is "
                            + Constants.MAXIMUM_INSTANCES
                            + ".");
        }
    }

    private Driver matchDriverById(String driverId) {
        for (Driver driver : drivers) {
            if (driver.getDriverId().equals(driverId)) {
                return driver;
            }
        }
        return null;
    }

    private Driver matchDriverByLicense(String licenseNumber) {
        for (Driver driver : drivers) {
            if (driver.getLicenseNumber().equals(licenseNumber)) {
                return driver;
            }
        }
        return null;
    }

    private Engineer matchEngineerById(String employeeId) {
        for (Engineer engineer : engineers) {
            if (engineer.getEmployeeId().equals(employeeId)) {
                return engineer;
            }
        }
        return null;
    }
}
