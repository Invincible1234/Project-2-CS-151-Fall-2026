package test;

import util.PartsCatalog;

import java.util.Map;

public final class PartsCatalogVerification {
    private static int checks;

    public static void main(String[] args) {
        Map<String, Double> prices = PartsCatalog.getPrices();
        check(!prices.isEmpty(), "Catalog must offer parts.");
        for (Map.Entry<String, Double> part : prices.entrySet()) {
            check(
                    Double.isFinite(part.getValue()) && part.getValue() > 0,
                    "Catalog prices must be finite and positive.");
            check(
                    PartsCatalog.getUnitPrice(part.getKey()) == part.getValue(),
                    "Displayed and lookup prices must agree.");
        }
        check(
                PartsCatalog.getUnitPrice("  tires  ") == prices.get("TIRES"),
                "Price lookup must normalize case and surrounding whitespace.");
        check(
                "TIRES".equals(PartsCatalog.normalizePartType("  tires  ")),
                "Inventory keys must use the same canonical part type as price lookup.");
        for (String invalid : new String[] {null, "", " \t\n", "\u2003", "UNKNOWN", "TIRE"}) {
            try {
                PartsCatalog.getUnitPrice(invalid);
                throw new AssertionError("Invalid part type was accepted.");
            } catch (IllegalArgumentException expected) {
                check(
                        expected.getMessage() != null && !expected.getMessage().isBlank(),
                        "Invalid parts must have a clear error message.");
            }
        }
        double originalPrice = prices.get("TIRES");
        try {
            prices.put("TIRES", 0.0);
            throw new AssertionError("Catalog prices must not be externally mutable.");
        } catch (UnsupportedOperationException expected) {
            checks++;
        }
        check(
                PartsCatalog.getUnitPrice("TIRES") == originalPrice,
                "Rejected catalog mutation must preserve the price.");
        System.out.println("PASS: Parts catalog (" + checks + " checks)");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
        checks++;
    }
}
