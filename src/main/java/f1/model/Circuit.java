package f1.model;

import util.Validation;

public class Circuit {
    private final String circuitId;
    private final String name;

    public Circuit(String circuitId, String name) {
        this.circuitId = Validation.requireText(circuitId, "Circuit ID");
        this.name = Validation.requireText(name, "Circuit name");
    }

    public String getCircuitId() {
        return circuitId;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof Circuit circuit
                && circuitId.equals(circuit.circuitId);
    }

    @Override
    public int hashCode() {
        return circuitId.hashCode();
    }

    @Override
    public String toString() {
        return "Circuit{circuitId='" + circuitId + "', name='" + name + "'}";
    }
}
