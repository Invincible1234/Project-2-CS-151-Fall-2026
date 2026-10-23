package util;

import exception.CapacityExceededException;
import java.util.HashMap;
import java.util.Map;

public final class InstanceLimit {
    private static final Map<Class<?>, Integer> COUNTS = new HashMap<>();

    private InstanceLimit() {
    }

    public static void checkAndCount(Class<?> type) {
        if (type == null) {
            throw new IllegalArgumentException("Type is required.");
        }
        int current = COUNTS.getOrDefault(type, 0);
        if (current >= Constants.MAXIMUM_INSTANCES) {
            throw new CapacityExceededException(
                    "Cannot create another " + type.getSimpleName() + ". The limit is "
                            + Constants.MAXIMUM_INSTANCES + ".");
        }
        COUNTS.put(type, current + 1);
    }

    public static void resetForTesting() {
        COUNTS.clear();
    }
}
