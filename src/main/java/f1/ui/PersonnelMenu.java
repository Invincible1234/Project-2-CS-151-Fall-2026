package f1.ui;

import f1.model.ClassicRaceCar;
import f1.model.Driver;
import f1.model.Engineer;
import f1.model.F1Car;
import f1.model.HybridRaceCar;
import f1.model.Team;
import f1.model.UserRole;
import f1.service.PersonnelRegistry;

import util.AccessContext;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public final class PersonnelMenu {
    private final PersonnelRegistry registry;
    private final ConsoleInput input;
    private AccessContext currentUser;
    private Team team;
    private final List<F1Car> cars = new ArrayList<>();

    public PersonnelMenu(PersonnelRegistry registry, ConsoleInput input) {
        this.registry = registry;
        this.input = input;
        this.currentUser = new AccessContext(UserRole.VIEWER, "");
    }

    public PersonnelMenu(Team team, ConsoleInput input) {
        this(new PersonnelRegistry(team), input);
        this.team = team;
        cars.addAll(team.getCars());
    }

    public UserRole open(UserRole role) {
        return open(new AccessContext(role, "")).getRole();
    }

    public AccessContext open(AccessContext caller) {
        currentUser = java.util.Objects.requireNonNull(caller, "Access context is required.");
        printMenu();
        int action = input.integer("Personnel action (0 Back)", 0, 23);
        if (action != 0) {
            handleChoice(Integer.toString(action));
        }
        if (input.isExitRequested()) {
            throw new ConsoleInput.ExitRequestedException();
        }
        return currentUser;
    }

    private PersonnelTeamActions teamActions() {
        return new PersonnelTeamActions(input, registry, cars);
    }

    private PersonnelWorkflowActions workflowActions() {
        return new PersonnelWorkflowActions(input, teamActions(), team);
    }

    private F1Car findCarByChassisId(String chassisId) {
        for (F1Car car : cars) {
            if (car.getChassisId().equals(chassisId)) {
                return car;
            }
        }
        return null;
    }

    public void run() {
        input.show("=== F1 Team Personnel Management ===");
        while (true) {
            printMenu();
            try {
                String choice = input.readText("Select option: ");
                if ("EXIT".equals(choice) || "0".equals(choice)) {
                    input.show("Exiting...");
                    break;
                }
                handleChoice(choice);
            } catch (ConsoleInput.ExitRequestedException e) {
                input.show("Exiting...");
                break;
            } catch (Exception e) {
                input.show("Error: " + e.getMessage());
            }
            input.show("");
        }
    }

    private void printMenu() {
        input.show("\n--- Personnel Menu ---");
        input.show("1. Register Driver");
        input.show("2. Update Medical Clearance");
        input.show("3. Apply Ban");
        input.show("4. Clear Ban");
        input.show("5. Register Engineer");
        input.show("6. Assign Driver to Car");
        input.show("7. Assign Engineer to Car");
        input.show("8. Release Engineer");
        input.show("9. Remove Driver");
        input.show("10. Remove Engineer");
        input.show("11. Request Car Setup");
        input.show("12. Update Driver Contact");
        input.show("13. Update Engineer Contact");
        input.show("14. Update Engineer Specialty");
        input.show("15. Request Parts");
        input.show("16. Complete Race");
        input.show("17. Complete Setup");
        input.show("18. Set Current User Role");
        input.show("19. Create Hybrid Car");
        input.show("20. Create Classic Car");
        input.show("21. List Cars");
        input.show("22. View Personnel Details");
        input.show("23. Find Driver by License");
        input.show("0. Back | EXIT. Quit");
    }

    private void handleChoice(String choice) {
        switch (choice) {
            case "1" -> registerDriver();
            case "2" -> updateMedicalClearance();
            case "3" -> applyBan();
            case "4" -> clearBan();
            case "5" -> registerEngineer();
            case "6" -> assignDriverToCar();
            case "7" -> assignEngineerToCar();
            case "8" -> releaseEngineer();
            case "9" -> removeDriver();
            case "10" -> removeEngineer();
            case "11" -> requestCarSetup();
            case "12" -> updateDriverContact();
            case "13" -> updateEngineerContact();
            case "14" -> updateEngineerSpecialty();
            case "15" -> requestParts();
            case "16" -> completeRace();
            case "17" -> completeSetup();
            case "18" -> setCurrentUserRole();
            case "19" -> createHybridCar();
            case "20" -> createClassicCar();
            case "21" -> listCars();
            case "22" -> viewPersonnelDetails();
            case "23" -> {
                Driver driver = registry.findDriverByLicense(input.text("Driver license"));
                input.show(driver == null ? "Driver not found." : driver);
            }
            default -> input.show("Unrecognized choice: " + choice);
        }
    }

    private void viewPersonnelDetails() {
        if (input.integer("Personnel type: 1 Driver | 2 Engineer", 1, 2) == 1) {
            Driver driver = input.select("Driver", registry.getDrivers(), Driver::getDriverId);
            input.show(driver);
            input.show("Phone: " + driver.phoneFor(currentUser));
            input.show("Medical notes: " + driver.medicalNotesFor(currentUser));
            input.show("Ban reason: " + driver.banReasonFor(currentUser));
            input.show("Preferred setup: " + driver.getPreferredSetup());
        } else {
            Engineer engineer = teamActions().engineer();
            input.show(engineer);
            input.show("Phone: " + engineer.phoneFor(currentUser));
        }
    }

    private void registerDriver() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            String name = input.readText("Name: ");
            if ("EXIT".equals(name)) return;
            String licenseNumber = input.readText("License number: ");
            if ("EXIT".equals(licenseNumber)) return;
            int age = input.readInt("Age: ");
            String phone = input.readText("Phone: ");
            if ("EXIT".equals(phone)) return;
            Driver driver = registry.registerDriver(driverId, name, licenseNumber, age, phone);
            input.show("Registered: " + driver);
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            input.show("Error: " + e.getMessage());
        }
    }

    private void updateMedicalClearance() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                input.show("Driver not found.");
                return;
            }
            boolean cleared = input.readYesNo("Medical clearance (yes/no)");
            String notes = input.readText("Medical notes: ");
            if ("EXIT".equals(notes)) return;
            driver.setMedicalClearance(cleared, notes);
            input.show("Medical clearance updated for " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            input.show("Error: " + e.getMessage());
        }
    }

    private void applyBan() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                input.show("Driver not found.");
                return;
            }
            String reason = input.readText("Ban reason: ");
            if ("EXIT".equals(reason)) return;
            driver.applyBan(reason);
            input.show("Ban applied to " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            input.show("Error: " + e.getMessage());
        }
    }

    private void clearBan() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                input.show("Driver not found.");
                return;
            }
            driver.clearBan();
            input.show("Ban cleared for " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            input.show("Error: " + e.getMessage());
        }
    }

    private void registerEngineer() {
        teamActions().registerEngineer();
    }

    private void assignDriverToCar() {
        teamActions().assignDriver();
    }

    private void assignEngineerToCar() {
        teamActions().assignEngineer();
    }

    private void releaseEngineer() {
        teamActions().releaseEngineer();
    }

    private void removeDriver() {
        teamActions().removeDriver();
    }

    private void removeEngineer() {
        teamActions().removeEngineer();
    }

    private void requestCarSetup() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                input.show("Driver not found.");
                return;
            }
            String preference = input.readText("Setup preference: ");
            if ("EXIT".equals(preference)) return;
            driver.requestCarSetup(preference);
            input.show("Car setup requested for " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            input.show("Error: " + e.getMessage());
        }
    }

    private void updateDriverContact() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                input.show("Driver not found.");
                return;
            }
            String newPhone = input.readText("New phone: ");
            if ("EXIT".equals(newPhone)) return;
            driver.updateContact(newPhone);
            input.show("Contact updated for " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            input.show("Error: " + e.getMessage());
        }
    }

    private void updateEngineerContact() {
        workflowActions().updateContact();
    }

    private void updateEngineerSpecialty() {
        workflowActions().updateSpecialty();
    }

    private void requestParts() {
        workflowActions().requestParts();
    }

    private void completeRace() {
        workflowActions().completeRace();
    }

    private void completeSetup() {
        workflowActions().completeSetup();
    }

    private void setCurrentUserRole() {
        try {
            UserRole role =
                    input.readEnum(
                            "Enter role (VIEWER, DRIVER, ENGINEER, TEAM_PRINCIPAL): ",
                            UserRole.class);
            String actorId = "";
            if (role == UserRole.DRIVER
                    || role == UserRole.ENGINEER
                    || role == UserRole.TEAM_PRINCIPAL) {
                actorId = input.readText("Enter actor ID: ");
                if ("EXIT".equals(actorId)) {
                    return;
                }
            }
            currentUser = new AccessContext(role, actorId);
            input.show(
                    "Current user set to: "
                            + role
                            + (actorId.isEmpty() ? "" : " (" + actorId + ")"));
        } catch (ConsoleInput.ExitRequestedException e) {
        }
    }

    private void createHybridCar() {
        try {
            String chassisId = input.readText("Chassis ID: ");
            if ("EXIT".equals(chassisId)) return;
            if (findCarByChassisId(chassisId) != null) {
                input.show("Chassis ID already exists.");
                return;
            }
            String teamName = input.readText("Team name: ");
            if ("EXIT".equals(teamName)) return;
            String model = input.readText("Model: ");
            if ("EXIT".equals(model)) return;
            int year = input.readInt("Year: ");
            double batteryCapacity = input.readInt("Battery capacity (MJ): ");
            double currentEnergy = input.readInt("Current energy (MJ): ");
            HybridRaceCar car =
                    new HybridRaceCar(
                            chassisId,
                            teamName,
                            model,
                            year,
                            batteryCapacity,
                            currentEnergy,
                            f1.model.HybridUpgrade.BALANCED.getPowerKw(),
                            input.clock());
            if (team != null) {
                team.addCar(car);
            }
            cars.add(car);
            input.show("Created hybrid car: " + car.getChassisId());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            input.show("Error: " + e.getMessage());
        }
    }

    private void createClassicCar() {
        try {
            String chassisId = input.readText("Chassis ID: ");
            if ("EXIT".equals(chassisId)) return;
            if (findCarByChassisId(chassisId) != null) {
                input.show("Chassis ID already exists.");
                return;
            }
            String teamName = input.readText("Team name: ");
            if ("EXIT".equals(teamName)) return;
            String model = input.readText("Model: ");
            if ("EXIT".equals(model)) return;
            int year = input.readInt("Year: ");
            int originalityScore = input.readInt("Originality score (0-100): ");
            int restorationYear = input.readInt("Restoration year: ");
            double powerLimit = input.readInt("Power limit (kW): ");
            ClassicRaceCar car =
                    new ClassicRaceCar(
                            chassisId,
                            teamName,
                            model,
                            year,
                            originalityScore,
                            restorationYear,
                            powerLimit,
                            input.clock());
            if (team != null) {
                team.addCar(car);
            }
            cars.add(car);
            input.show("Created classic car: " + car.getChassisId());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            input.show("Error: " + e.getMessage());
        }
    }

    private void listCars() {
        if (cars.isEmpty()) {
            input.show("No cars available.");
            return;
        }
        input.show("Available cars:");
        for (F1Car car : cars) {
            input.show(
                    "  "
                            + car.getChassisId()
                            + " ("
                            + car.getClass().getSimpleName()
                            + ") - "
                            + car.getTeamName()
                            + " "
                            + car.getModel()
                            + " "
                            + car.getYear());
        }
    }

    public static void main(String[] args) {
        ConsoleInput input = new ConsoleInput(new InputStreamReader(System.in), System.out);
        new PersonnelMenu(new PersonnelRegistry(), input).run();
    }
}
