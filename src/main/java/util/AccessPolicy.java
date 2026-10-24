package util;

import f1.model.UserRole;

public final class AccessPolicy {
    public static final String MASKED_PHONE = "**********";

    private AccessPolicy() {}

    public static boolean canViewPhone(AccessContext caller, String ownerId) {
        requireContext(caller);
        if (caller.getRole() == UserRole.TEAM_PRINCIPAL) {
            return true;
        }
        return isOwner(caller, ownerId)
                && (caller.getRole() == UserRole.DRIVER || caller.getRole() == UserRole.ENGINEER);
    }

    public static boolean canViewMedicalNotes(AccessContext caller, String driverId) {
        return canViewOwnDriverRecord(caller, driverId);
    }

    public static boolean canViewBanReason(AccessContext caller, String driverId) {
        return canViewOwnDriverRecord(caller, driverId);
    }

    public static String visiblePhone(AccessContext caller, String ownerId, String phone) {
        if (!canViewPhone(caller, ownerId)) {
            return MASKED_PHONE;
        }
        return phone == null ? "" : phone;
    }

    public static String visibleMedicalNotes(AccessContext caller, String driverId, String notes) {
        if (!canViewMedicalNotes(caller, driverId)) {
            return "";
        }
        return notes == null ? "" : notes;
    }

    public static String visibleBanReason(AccessContext caller, String driverId, String banReason) {
        if (!canViewBanReason(caller, driverId)) {
            return "";
        }
        return banReason == null ? "" : banReason;
    }

    private static boolean canViewOwnDriverRecord(AccessContext caller, String driverId) {
        requireContext(caller);
        if (caller.getRole() == UserRole.TEAM_PRINCIPAL) {
            return true;
        }
        return caller.getRole() == UserRole.DRIVER && isOwner(caller, driverId);
    }

    private static boolean isOwner(AccessContext caller, String ownerId) {
        return ownerId != null && !ownerId.isBlank() && caller.getActorId().equals(ownerId.strip());
    }

    private static void requireContext(AccessContext caller) {
        if (caller == null) {
            throw new IllegalArgumentException("Access context is required.");
        }
    }
}
