package util;

import f1.model.UserRole;

public final class AccessContext {
    private final UserRole role;
    private final String actorId;

    public AccessContext(UserRole role, String actorId) {
        if (role == null) {
            throw new IllegalArgumentException("User role is required.");
        }
        this.role = role;
        this.actorId = actorId == null ? "" : actorId.strip();
    }

    public UserRole getRole() {
        return role;
    }

    public String getActorId() {
        return actorId;
    }
}
