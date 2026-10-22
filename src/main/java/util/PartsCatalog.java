package util;

import java.util.Locale;
import java.util.Map;

public final class PartsCatalog {
    private static final Map<String, Double> UNIT_PRICES = Map.of(
            "TIRES", 1200.0,
            "AERO_KIT", 10000.0,
            "HYBRID_COMPONENTS", 25000.0,
            "RESTORATION_KIT", 5000.0);

    private PartsCatalog() {
    }

    public static double getUnitPrice(String partType) {
        if (partType == null || partType.isBlank()) {
            throw new IllegalArgumentException("Part type is required.");
        }
        Double price = UNIT_PRICES.get(partType.strip().toUpperCase(Locale.ROOT));
        if (price == null) {
            throw new IllegalArgumentException("Unknown part type. Choose a part from the catalog.");
        }
        return price;
    }

    public static Map<String, Double> getPrices() {
        return UNIT_PRICES;
    }
}
