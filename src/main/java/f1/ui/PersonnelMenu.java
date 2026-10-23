package f1.ui;

import f1.model.Driver;
import f1.model.F1Car;
import f1.model.UserRole;
import f1.service.PersonnelRegistry;
import util.AccessContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public final class PersonnelMenu {
    private final PersonnelRegistry registry;
    private final ConsoleInput input;
    private AccessContext currentUser;
    private final List<F1Car> cars = new ArrayList<>();

    public PersonnelMenu(PersonnelRegistry registry, ConsoleInput input) {
        this.registry = registry;
        this.input = input;
        this.currentUser = new AccessContext(UserRole.VIEWER, "");
    }

    private F1Car findCarByChassisId(String chassisId) {
        for (F1Car car : cars) {
            if (car.getChassisId().equals(chassisId)) {
                return car;
            }
        }
        return null;
    }

    private void ensureCarsAvailable() {
        if (cars.isEmpty()) {
            System.out.println("No cars available. Create a car first.");
            throw new IllegalStateException("No cars available");
        }
    }

    public void run() {
        System.out.println("=== F1 Team Personnel Management ===");
        while (true) {
            printMenu();
            try {
                String choice = input.readText("Select option: ");
                if ("EXIT".equals(choice)) {
                    System.out.println("Exiting...");
                    break;
                }
                handleChoice(choice);
            } catch (ConsoleInput.ExitRequestedException e) {
                System.out.println("Exiting...");
                break;
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
            System.out.println();
        }
    }

    private void printMenu() {
        System.out.println("\n--- Personnel Menu ---");
        System.out.println("1. Register Driver");
        System.out.println("2. Update Medical Clearance");
        System.out.println("3. Apply Ban");
        System.out.println("4. Clear Ban");
        System.out.println("5. Register Engineer");
        System.out.println("6. Assign Driver to Car");
        System.out.println("7. Assign Engineer to Car");
        System.out.println("8. Release Engineer");
        System.out.println("9. Remove Driver");
        System.out.println("10. Remove Engineer");
        System.out.println("11. Request Car Setup");
        System.out.println("12. Update Driver Contact");
        System.out.println("13. Update Engineer Contact");
        System.out.println("14. Update Engineer Specialty");
        System.out.println("15. Request Parts");
        System.out.println("16. Complete Race");
        System.out.println("17. Complete Setup");
        System.out.println("18. Set Current User Role");
        System.out.println("EXIT. Quit");
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
            default -> System.out.println("Unrecognized choice: " + choice);
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
            System.out.println("Registered: " + driver);
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void updateMedicalClearance() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                System.out.println("Driver not found.");
                return;
            }
            boolean cleared = input.readYesNo("Medical clearance (yes/no)");
            String notes = input.readText("Medical notes: ");
            if ("EXIT".equals(notes)) return;
            driver.setMedicalClearance(cleared, notes);
            System.out.println("Medical clearance updated for " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void applyBan() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                System.out.println("Driver not found.");
                return;
            }
            String reason = input.readText("Ban reason: ");
            if ("EXIT".equals(reason)) return;
            driver.applyBan(reason);
            System.out.println("Ban applied to " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void clearBan() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                System.out.println("Driver not found.");
                return;
            }
            driver.clearBan();
            System.out.println("Ban cleared for " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void registerEngineer() {
        System.out.println("(not wired yet)");
    }

    private void assignDriverToCar() {
        System.out.println("(not wired yet)");
    }

    private void assignEngineerToCar() {
        System.out.println("(not wired yet)");
    }

    private void releaseEngineer() {
        System.out.println("(not wired yet)");
    }

    private void removeDriver() {
        System.out.println("(not wired yet)");
    }

    private void removeEngineer() {
        System.out.println("(not wired yet)");
    }

    private void requestCarSetup() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                System.out.println("Driver not found.");
                return;
            }
            String preference = input.readText("Setup preference: ");
            if ("EXIT".equals(preference)) return;
            driver.requestCarSetup(preference);
            System.out.println("Car setup requested for " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void updateDriverContact() {
        try {
            String driverId = input.readText("Driver ID: ");
            if ("EXIT".equals(driverId)) return;
            Driver driver = registry.findDriverById(driverId);
            if (driver == null) {
                System.out.println("Driver not found.");
                return;
            }
            String newPhone = input.readText("New phone: ");
            if ("EXIT".equals(newPhone)) return;
            driver.updateContact(newPhone);
            System.out.println("Contact updated for " + driver.getName());
        } catch (ConsoleInput.ExitRequestedException e) {
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void updateEngineerContact() {
        System.out.println("(not wired yet)");
    }

    private void updateEngineerSpecialty() {
        System.out.println("(not wired yet)");
    }

    private void requestParts() {
        System.out.println("(not wired yet)");
    }

    private void completeRace() {
        System.out.println("(not wired yet)");
    }

    private void completeSetup() {
        System.out.println("(not wired yet)");
    }

    private void setCurrentUserRole() {
        try {
            UserRole role = input.readEnum("Enter role (VIEWER, DRIVER, ENGINEER, TEAM_PRINCIPAL): ", UserRole.class);
            String actorId = "";
            if (role == UserRole.DRIVER || role == UserRole.ENGINEER || role == UserRole.TEAM_PRINCIPAL) {
                actorId = input.readText("Enter actor ID: ");
                if ("EXIT".equals(actorId)) {
                    return;
                }
            }
            currentUser = new AccessContext(role, actorId);
            System.out.println("Current user set to: " + role + (actorId.isEmpty() ? "" : " (" + actorId + ")"));
        } catch (ConsoleInput.ExitRequestedException e) {
        }
    }

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            PersonnelRegistry registry = new PersonnelRegistry();
            ConsoleInput input = new ConsoleInput(scanner);
            PersonnelMenu menu = new PersonnelMenu(registry, input);
            menu.run();
        }
    }
}