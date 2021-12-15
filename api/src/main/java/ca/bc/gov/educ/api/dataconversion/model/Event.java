package ca.bc.gov.educ.api.dataconversion.model;

import ca.bc.gov.educ.api.trax.constant.EventOutcome;
import ca.bc.gov.educ.api.trax.constant.EventType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;


/**
 * The type Event.
 */
@AllArgsConstructor
@Builder
@NoArgsConstructor
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Event {
  private EventType eventType;
  private EventOutcome eventOutcome;
  private String replyTo;
  private String eventPayload;
  private String payloadVersion;
  private UUID sagaId;
}
