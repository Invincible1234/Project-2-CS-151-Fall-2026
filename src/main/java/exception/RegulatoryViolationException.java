package exception;

public class RegulatoryViolationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RegulatoryViolationException(String message) {
        super(message);
    }
}
