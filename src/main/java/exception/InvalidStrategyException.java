package exception;

public class InvalidStrategyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidStrategyException(String message) {
        super(message);
    }
}
