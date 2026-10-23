package f1.ui;

import f1.model.Team;
import f1.model.UserRole;
import java.util.List;
import util.PartsCatalog;

final class ResourceMenu {
    private final ConsoleInput input;
    private final ConsoleState state;

    ResourceMenu(ConsoleInput input, ConsoleState state) {
        this.input = input;
        this.state = state;
    }

    void open() {
        input.show("Resources: 1 Budget | 2 Inventory | 3 Add budget | 4 Buy parts | 5 Consume parts | 0 Back");
        int action = input.integer("Resource action", 0, 5);
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
                String part = input.select("Part", List.copyOf(PartsCatalog.getPrices().keySet()), value -> value);
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
            default -> { }
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
