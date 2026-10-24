package f1.model;

import model.StrategyStatus;

import util.AccessContext;
import util.AccessPolicy;
import util.InstanceLimit;
import util.PartsCatalog;
import util.Validation;

import java.util.HashMap;
import java.util.Map;

public class Engineer {
    private final String employeeId;
    private final String name;
    private EngineerSpecialty specialty;
    private boolean isAvailable;
    private String assignedChassisId;
    private final int experienceYears;
    private String phone;
    private F1Car assignedCar;
    private final Map<String, Integer> requestedParts = new HashMap<>();

    public Engineer(
            String employeeId,
            String name,
            EngineerSpecialty specialty,
            int experienceYears,
            String phone) {
        this.employeeId = Validation.requireText(employeeId, "Employee ID");
        this.name = Validation.requireText(name, "Engineer name");
        if (specialty == null) {
            throw new IllegalArgumentException("Engineer specialty is required.");
        }
        this.specialty = specialty;
        this.experienceYears = Validation.requireExperienceYears(experienceYears);
        this.phone = Validation.requirePhone(phone);
        InstanceLimit.checkAndCount(Engineer.class);
        this.isAvailable = true;
        this.assignedChassisId = "";
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public String getName() {
        return name;
    }

    public EngineerSpecialty getSpecialty() {
        return specialty;
    }

    public int getExperienceYears() {
        return experienceYears;
    }

    public boolean isAvailable() {
        return isAvailable;
    }

    public String getAssignedChassisId() {
        return assignedChassisId;
    }

    public boolean assignToCar(F1Car car) {
        if (car == null) {
            throw new IllegalArgumentException("Car is required.");
        }
        if (!isAvailable || assignedCar != null) {
            throw new IllegalStateException("Engineer is already assigned or unavailable.");
        }
        if (!isQualifiedFor(car)) {
            throw new IllegalArgumentException("Engineer specialty is incompatible with this car.");
        }
        car.assignEngineer(this);
        assignedCar = car;
        assignedChassisId = car.getChassisId();
        isAvailable = false;
        return true;
    }

    public boolean completeSetup(RaceStrategy strategy) {
        if (strategy == null
                || assignedCar == null
                || strategy.getCar() != assignedCar
                || strategy.getEngineer() != this
                || assignedCar.getAssignedEngineer() != this
                || strategy.getStatus() != StrategyStatus.DRAFT
                || !isQualifiedFor(assignedCar)) {
            return false;
        }
        return strategy.completeEngineerSetup();
    }

    public boolean requestParts(String partType, int quantity) {
        String part = PartsCatalog.normalizePartType(partType);
        int pending = requestedParts.getOrDefault(part, 0);
        if (quantity <= 0 || quantity > Integer.MAX_VALUE - pending) {
            throw new IllegalArgumentException(
                    "Requested quantity must be positive and fit the supported range.");
        }
        if (assignedCar == null) {
            return false;
        }
        requestedParts.put(part, pending + quantity);
        return true;
    }

    public Map<String, Integer> requestedPartsFor(UserRole role) {
        requirePartsAccess(role);
        return Map.copyOf(requestedParts);
    }

    public void fulfillPartsRequest(String partType, int quantity, UserRole role) {
        requirePartsAccess(role);
        String part = PartsCatalog.normalizePartType(partType);
        int pending = requestedParts.getOrDefault(part, 0);
        if (quantity <= 0 || quantity > pending) {
            throw new IllegalArgumentException(
                    "Choose a positive quantity within the outstanding request.");
        }
        Team team = assignedCar == null ? null : assignedCar.getRegisteredTeam();
        if (team == null || team.getEngineers().stream().noneMatch(engineer -> engineer == this)) {
            throw new IllegalStateException(
                    "Assign this team's registered engineer before fulfilling a request.");
        }
        team.addParts(part, quantity);
        if (quantity == pending) {
            requestedParts.remove(part);
        } else {
            requestedParts.put(part, pending - quantity);
        }
    }

    private static void requirePartsAccess(UserRole role) {
        if (role != UserRole.TEAM_PRINCIPAL && role != UserRole.ENGINEER) {
            throw new SecurityException(
                    "Engineer or team principal access is required for parts requests.");
        }
    }

    public void releaseFromCar() {
        if (assignedCar != null) {
            assignedCar.releaseEngineer(this);
        }
        requestedParts.clear();
        assignedCar = null;
        assignedChassisId = "";
        isAvailable = true;
    }

    public void updateSpecialty(EngineerSpecialty newSpecialty) {
        if (newSpecialty == null) {
            throw new IllegalArgumentException("Engineer specialty is required.");
        }
        if (assignedCar != null) {
            throw new IllegalStateException("Cannot change the specialty of an assigned engineer.");
        }
        this.specialty = newSpecialty;
    }

    public void updateContact(String newPhone) {
        this.phone = Validation.requirePhone(newPhone);
    }

    public String phoneFor(AccessContext caller) {
        return AccessPolicy.visiblePhone(caller, employeeId, phone);
    }

    public boolean isQualifiedFor(F1Car car) {
        if (car instanceof HybridRaceCar hybrid) {
            if (hybrid.requiresSpecialistEngineer()) {
                return specialty == EngineerSpecialty.HYBRID_POWER_UNIT;
            }
            return specialty != EngineerSpecialty.CLASSIC_RESTORATION;
        }
        if (car instanceof ClassicRaceCar) {
            return specialty == EngineerSpecialty.CLASSIC_RESTORATION
                    || specialty == EngineerSpecialty.AERODYNAMICS
                    || specialty == EngineerSpecialty.RACE_ENGINEERING;
        }
        return false;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof Engineer engineer && employeeId.equals(engineer.employeeId);
    }

    @Override
    public int hashCode() {
        return employeeId.hashCode();
    }

    @Override
    public String toString() {
        return "Engineer{employeeId='"
                + employeeId
                + "', name='"
                + name
                + "', specialty="
                + specialty
                + ", experienceYears="
                + experienceYears
                + ", available="
                + isAvailable
                + ", assignedChassisId='"
                + assignedChassisId
                + "'}";
    }
}
