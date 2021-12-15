package ca.bc.gov.educ.api.dataconversion.service;

import ca.bc.gov.educ.api.dataconversion.entity.trax.Event;

public interface EventService {

  <T extends Object> void processEvent(T request, Event event);

  String getEventType();
}
