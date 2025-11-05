package ca.bc.gov.educ.api.dataconversion.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChoreographedEventValidation {
  UUID eventID;
  /**
   * The Event type.
   */
  String eventType;
  /**
   * The Event outcome.
   */
  String eventOutcome;
  /**
   * The Activity code.
   */
  String activityCode;
  /**
   * The Event payload.
   */
  String eventPayload; // json string
  /**
   * The Create user.
   */
  String createUser;
  /**
   * The Update user.
   */
  String updateUser;
}
