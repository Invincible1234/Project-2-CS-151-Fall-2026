package f1.model;

/**
 * Raceable is the shared contract for a race weekend.
 *
 * HybridRaceCar and ClassicRaceCar both need to prepare for a session,
 * finish that session, report maintenance, and say if they can race.
 * A safety car or a simulator rig might need those same actions later,
 * and those are not F1 cars. An interface lets them share the actions
 * without sharing F1Car fields.
 */
public interface Raceable {

    /**
     * Gets this object ready for one session.
     * sessionType is "practice", "qualifying", or "race".
     * Returns true when setup worked and the object can run.
     * Returns false when it is not ready, such as when maintenance is due.
     */
    boolean prepareForSession(String sessionType);

    /**
     * Ends the session that is in progress.
     * The class that implements this should update its own state,
     * for example mileage or whether it is still race ready.
     */
    void completeSession();

    /**
     * Reports when this object should next be maintained.
     * The text is a short note a crew member can read,
     * such as "before the next race" or "after 50 more km".
     */
    String getNextMaintenanceDue();

    /**
     * Says whether this object can be entered in a race right now.
     * This is false when setup is unfinished or the object is out of service.
     */
    boolean isAvailableForRace();
}
