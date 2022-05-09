package ca.bc.gov.educ.api.dataconversion.util;

public class ThreadLocalStateUtil {
    private static ThreadLocal<String> transaction = new ThreadLocal<>();
    private static ThreadLocal<String> user = new ThreadLocal<>();

    /**
     * Set the current correlationID for this thread
     *
     * @param correlationID
     */
    public static void setCorrelationID(String correlationID){
        transaction.set(correlationID);
    }

    /**
     * Get the current correlationID for this thread
     *
     * @return the correlationID, or null if it is unknown.
     */
    public static String getCorrelationID() {
        return transaction.get();
    }

    /**
     * Set the current user for this thread
     *
     * @param currentUser
     */
    public static void setCurrentUser(String currentUser){
        user.set(currentUser);
    }

    /**
     * Get the current user for this thread
     *
     * @return the username of the current user, or null if it is unknown.
     */
    public static String getCurrentUser() {
        return user.get();
    }

    public static void clear() {
        transaction.remove();
        user.remove();
    }
}
