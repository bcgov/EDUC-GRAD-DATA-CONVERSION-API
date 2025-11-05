package ca.bc.gov.educ.api.dataconversion.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

@EqualsAndHashCode(callSuper = true)
@Data
public class IgnoreEventException extends Exception {

  private final String eventType;
  private final String eventOutcome;
  /**
   * The constant serialVersionUID.
   */
  @Serial
  private static final long serialVersionUID = 5241655513745148898L;

  /**
   * Instantiates a new Pen reg api runtime exception.
   *
   * @param message the message
   */
  public IgnoreEventException(final String message, String eventType, String eventOutcome) {
    super(message);
    this.eventType = eventType;
    this.eventOutcome = eventOutcome;
  }
}
