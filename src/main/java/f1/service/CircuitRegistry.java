package f1.service;

import exception.CapacityExceededException;
import exception.DuplicateEntityException;

import f1.model.Circuit;

import util.Constants;
import util.IdentityRegistry;
import util.Validation;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CircuitRegistry {
    private final Map<String, Circuit> circuits = new LinkedHashMap<>();

    public void addCircuit(Circuit circuit) {
        if (circuit == null) {
            throw new IllegalArgumentException("Circuit is required.");
        }
        requireAvailableId(circuit.getCircuitId());
        IdentityRegistry.claim(this, circuit, List.of("Circuit ID", circuit.getCircuitId()));
        circuits.put(circuit.getCircuitId(), circuit);
    }

    public void requireAvailableId(String id) {
        String key = Validation.requireText(id, "Circuit ID");
        if (circuits.containsKey(key)) {
            throw new DuplicateEntityException("Circuit ID is already registered.");
        }
        if (circuits.size() >= Constants.MAXIMUM_INSTANCES) {
            throw new CapacityExceededException("Circuit capacity has been reached.");
        }
    }

    public Circuit findCircuit(String id) {
        Circuit circuit = circuits.get(Validation.requireText(id, "Circuit ID"));
        if (circuit == null) {
            throw new IllegalArgumentException("No circuit has that ID.");
        }
        return circuit;
    }

    public List<Circuit> getCircuits() {
        return List.copyOf(circuits.values());
    }
}
