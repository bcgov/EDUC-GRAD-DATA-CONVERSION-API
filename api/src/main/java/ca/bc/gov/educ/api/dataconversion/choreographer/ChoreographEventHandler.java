package ca.bc.gov.educ.api.dataconversion.choreographer;

import ca.bc.gov.educ.api.dataconversion.entity.conv.Event;
import ca.bc.gov.educ.api.dataconversion.model.TraxUpdateInGrad;
import ca.bc.gov.educ.api.dataconversion.service.EventService;
import ca.bc.gov.educ.api.dataconversion.util.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static ca.bc.gov.educ.api.dataconversion.constant.EventType.UPDATE_TRAX_STUDENT_MASTER;

/**
 * This class is responsible to handle different choreographed events related student by calling different services.
 */

@Component
@Slf4j
public class ChoreographEventHandler {
  private final ObjectMapper mapper = new ObjectMapper();
  private final Executor singleTaskExecutor = new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("task-executor-%d").build())
      .setCorePoolSize(1).setMaximumPoolSize(1).build();
  private final Map<String, EventService> eventServiceMap;

  public ChoreographEventHandler(final List<EventService> eventServices) {
    this.eventServiceMap = new HashMap<>();
    eventServices.forEach(eventService -> this.eventServiceMap.put(eventService.getEventType(), eventService));
  }

  public void handleEvent(@NonNull final Event event) {
    //only one thread will process all the request. since RDB won't handle concurrent requests.
    this.singleTaskExecutor.execute(() -> {
      try {
        switch (event.getEventType()) {
          case "UPDATE_TRAX_STUDENT_MASTER":
            log.info("Processing CREATE_GRAD_STATUS event record :: {} ", event);
            final TraxUpdateInGrad traxUpdateInGrad = JsonUtil.getJsonObjectFromString(TraxUpdateInGrad.class, event.getEventPayload());
            this.eventServiceMap.get(UPDATE_TRAX_STUDENT_MASTER.toString()).processEvent(traxUpdateInGrad, event);
            break;
          default:
            log.warn("Silently ignoring event: {}", event);
            break;
        }
      } catch (final Exception exception) {
        log.error("Exception while processing event :: {}", event, exception);
      }
    });
  }
}
