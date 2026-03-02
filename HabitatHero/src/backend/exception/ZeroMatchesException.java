package exception;
/**
 * Thrown by {@link RecommendationEngine} when hard filters eliminate every
 * candidate HDB block, leaving zero results to present to the user.
 */
public class ZeroMatchesException extends RuntimeException {

    public ZeroMatchesException() {
        super("No Matches Found. Please broaden your search criteria.");
    }

    public ZeroMatchesException(String message) {
        super(message);
    }
}
