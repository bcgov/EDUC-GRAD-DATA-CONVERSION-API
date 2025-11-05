package ca.bc.gov.educ.api.dataconversion.util;

import ca.bc.gov.educ.api.dataconversion.constant.EventOutcome;
import ca.bc.gov.educ.api.dataconversion.constant.EventType;
import ca.bc.gov.educ.api.dataconversion.exception.IgnoreEventException;
import ca.bc.gov.educ.api.dataconversion.model.ChoreographedEvent;
import ca.bc.gov.educ.api.dataconversion.model.ChoreographedEventValidation;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;


public final class EventUtils {
  private EventUtils() {
  }

  public static ChoreographedEvent getChoreographedEventIfValid(String eventString) throws JsonProcessingException, IgnoreEventException {
    final ChoreographedEventValidation event = JsonUtil.getJsonObjectFromString(ChoreographedEventValidation.class, eventString);
    if(StringUtils.isNotBlank(event.getEventOutcome()) && !EventOutcome.isValid(event.getEventOutcome())) {
      throw new IgnoreEventException("Invalid event outcome", event.getEventType(), event.getEventOutcome());
    }else if(StringUtils.isNotBlank(event.getEventType()) && !EventType.isValid(event.getEventType())) {
      throw new IgnoreEventException("Invalid event type", event.getEventType(), event.getEventOutcome());
    }
    return JsonUtil.getJsonObjectFromString(ChoreographedEvent.class, eventString);
  }
}
