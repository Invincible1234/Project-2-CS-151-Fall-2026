public interface Raceable {
    boolean prepareForSession(String type);

    void completeSession();

    String getNextMaintenanceDue();

    boolean isAvailableForRace();
}
