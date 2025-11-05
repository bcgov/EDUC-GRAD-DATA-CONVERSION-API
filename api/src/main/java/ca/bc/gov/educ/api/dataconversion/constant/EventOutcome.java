package ca.bc.gov.educ.api.dataconversion.constant;

/**
 * The enum Event outcome.
 */
public enum EventOutcome {
  /**
   * Student updated event outcome.
   */
  TRAX_STUDENT_MASTER_UPDATED;

  public static boolean isValid(String value) {
    if (value == null) {
      return false;
    }
    try {
      EventOutcome.valueOf(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }

}
