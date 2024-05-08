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
  UPD_STD_STATUS, // UPD_STD_STATUS will be deprecated
  XPROGRAM,
  ASSESSMENT,
  COURSE,
  FI10ADD
}
