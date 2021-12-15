package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.dataconversion.constant.EventOutcome;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
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
public class ChoreographedEvent {
  UUID eventID;
  /**
   * The Event type.
   */
  EventType eventType;
  /**
   * The Event outcome.
   */
  EventOutcome eventOutcome;
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
