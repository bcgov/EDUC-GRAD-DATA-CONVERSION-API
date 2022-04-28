package ca.bc.gov.educ.api.dataconversion.util;

public class ThreadLocalStateUtil {
    private static ThreadLocal<String> transaction = new ThreadLocal<>();

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
}
