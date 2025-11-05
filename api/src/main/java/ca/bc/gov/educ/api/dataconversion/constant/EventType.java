package ca.bc.gov.educ.api.dataconversion.constant;

/**
 * The enum Event type.
 */
public enum EventType {
  /* ===========================================================
    Incremental updates from Trax to Grad
   =============================================================*/
  /**
   * Trax update type
   */
  NEWSTUDENT,
  UPD_DEMOG,
  UPD_GRAD,
  XPROGRAM,
  ASSESSMENT,
  COURSE,
  FI10ADD;

  public static boolean isValid(String value) {
    if (value == null) {
      return false;
    }
    try {
      EventType.valueOf(value);
      return true;
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
