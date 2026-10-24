package f1.ui;

import f1.model.Engineer;
import f1.model.EngineerSpecialty;
import f1.model.RaceStrategy;
import f1.model.Team;
import util.Validation;

final class PersonnelWorkflowActions {
    private final ConsoleInput input;
    private final PersonnelTeamActions people;
    private final Team team;

    PersonnelWorkflowActions(ConsoleInput input, PersonnelTeamActions people, Team team) {
        this.input = input;
        this.people = people;
        this.team = team;
    }

    void updateContact() {
        Engineer engineer = people.engineer();
        engineer.updateContact(input.read("New engineer phone", Validation::requirePhone));
        input.show("Engineer contact updated.");
    }

    void updateSpecialty() {
        people.engineer().updateSpecialty(input.choice("Specialty", EngineerSpecialty.class));
        input.show("Engineer specialty updated.");
    }

    void requestParts() {
        Engineer engineer = people.engineer();
        String part = input.read("Requested part type", util.PartsCatalog::normalizePartType);
        int quantity = input.integer("Requested quantity", 1, Integer.MAX_VALUE);
        input.show(engineer.requestParts(part, quantity)
                ? "Parts requested; use Inventory and budget to purchase stock."
                : "Assign the engineer to a car before requesting parts.");
    }

    void completeSetup() {
        requireTeam();
        Engineer engineer = people.engineer();
        RaceStrategy strategy = input.select("Strategy (chassis/strategy)", team.getStrategies(),
                entry -> entry.getChassisId() + "/" + entry.getStrategyId());
        input.show(engineer.completeSetup(strategy) ? "Setup confirmed." : "Assign this engineer to the strategy car first.");
    }

    void completeRace() {
        requireTeam();
        new ui.RaceResultMenu(new ui.ConsoleInput(input), team).open();
    }

    private void requireTeam() {
        if (team == null) {
            throw new IllegalStateException("Open Personnel from the main application with a selected team.");
        }
    }
}
