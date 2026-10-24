package util;

import exception.DuplicateEntityException;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class IdentityRegistry {
    private static final Map<List<String>, Claim> CLAIMS = new HashMap<>();

    private IdentityRegistry() {}

    @SafeVarargs
    public static void claim(Object owner, Object entity, List<String>... identities) {
        for (List<String> identity : identities) {
            Claim existing = CLAIMS.get(identity);
            if (existing != null && existing.entity != entity) {
                throw new DuplicateEntityException(
                        identity.get(0) + " is already registered in this session.");
            }
        }
        for (List<String> identity : identities) {
            Claim claim = CLAIMS.computeIfAbsent(List.copyOf(identity), key -> new Claim(entity));
            claim.owners.add(owner);
        }
    }

    public static void release(Object owner, Object entity) {
        CLAIMS.entrySet()
                .removeIf(
                        entry -> {
                            Claim claim = entry.getValue();
                            if (claim.entity == entity) {
                                claim.owners.remove(owner);
                                return claim.owners.isEmpty();
                            }
                            return false;
                        });
    }

    static void resetForTesting() {
        CLAIMS.clear();
    }

    private static final class Claim {
        private final Object entity;
        private final Set<Object> owners = Collections.newSetFromMap(new IdentityHashMap<>());

        private Claim(Object entity) {
            this.entity = entity;
        }
    }
}
