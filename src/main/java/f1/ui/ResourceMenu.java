package f1.ui;

import f1.model.Team;
import f1.model.UserRole;

import util.PartsCatalog;

import java.util.List;

final class ResourceMenu {
    private final ConsoleInput input;
    private final ConsoleState state;

    ResourceMenu(ConsoleInput input, ConsoleState state) {
        this.input = input;
        this.state = state;
    }

    void open() {
        input.show(
                "Resources: 1 Budget | 2 Inventory | 3 Add budget | 4 Buy parts | 5 Consume parts |"
                        + " 6 Engineer requests | 0 Back");
        int action = input.integer("Resource action", 0, 6);
        if (action == 0) {
            return;
        }
        Team team = state.team();
        switch (action) {
            case 1 -> input.show("Budget: " + team.getBudget(state.getRole()));
            case 2 -> input.show("Inventory: " + team.getPartsInventory(state.getRole()));
            case 3 -> {
                requirePrincipal();
                team.addBudget(input.amount("Budget addition", true), state.getRole());
                input.show("Budget updated.");
            }
            case 4, 5 -> {
                requireInventoryAccess();
                String part =
                        input.select(
                                "Part",
                                List.copyOf(PartsCatalog.getPrices().keySet()),
                                value -> value);
                input.show("Unit purchase price: " + PartsCatalog.getUnitPrice(part));
                int quantity = input.integer("Quantity", 1, Integer.MAX_VALUE);
                if (action == 4) {
                    team.addParts(part, quantity);
                    input.show("Parts purchased; budget deducted.");
                } else {
                    team.consumeParts(part, quantity);
                    input.show("Parts consumed.");
                }
            }
            case 6 -> {
                requireInventoryAccess();
                var engineer =
                        input.select(
                                "Engineer", team.getEngineers(), f1.model.Engineer::getEmployeeId);
                if (team.getCars().stream()
                        .noneMatch(
                                car ->
                                        car.getChassisId()
                                                .equals(engineer.getAssignedChassisId()))) {
                    throw new IllegalStateException(
                            "The engineer must be assigned to this team's car before fulfilling"
                                    + " requests.");
                }
                var pending = engineer.requestedPartsFor(state.getRole());
                input.show("Outstanding requests: " + pending);
                if (pending.isEmpty()) {
                    return;
                }
                String part =
                        input.select(
                                "Requested part", List.copyOf(pending.keySet()), value -> value);
                int quantity = input.integer("Quantity to purchase", 1, pending.get(part));
                engineer.fulfillPartsRequest(part, quantity, state.getRole());
                input.show("Request fulfilled; stock purchased and budget deducted once.");
            }
            default -> {}
        }
    }

    private void requirePrincipal() {
        if (state.getRole() != UserRole.TEAM_PRINCIPAL) {
            throw new SecurityException("Team principal access is required.");
        }
    }

    private void requireInventoryAccess() {
        if (state.getRole() != UserRole.TEAM_PRINCIPAL && state.getRole() != UserRole.ENGINEER) {
            throw new SecurityException("Team principal or engineer access is required.");
        }
    }
}
