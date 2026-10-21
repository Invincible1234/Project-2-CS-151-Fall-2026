package exception;

public class InsufficientResourcesException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InsufficientResourcesException(String message) {
        super(message);
    }
}
