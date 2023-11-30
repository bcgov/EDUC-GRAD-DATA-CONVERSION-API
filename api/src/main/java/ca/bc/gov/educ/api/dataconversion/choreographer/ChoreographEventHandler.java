package ca.bc.gov.educ.api.dataconversion.choreographer;

import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.service.EventService;
import ca.bc.gov.educ.api.dataconversion.util.JsonUtil;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.jboss.threads.EnhancedQueueExecutor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

import static ca.bc.gov.educ.api.dataconversion.constant.EventType.*;

/**
 * This class is responsible to handle different choreographed events related student by calling different services.
 */

@Component
@Slf4j
public class ChoreographEventHandler {
  private final Executor singleTaskExecutor = new EnhancedQueueExecutor.Builder()
      .setThreadFactory(new ThreadFactoryBuilder().setNameFormat("single-task-executor-%d").build())
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
          case "NEWSTUDENT":
            log.debug("Processing NEWSTUDENT event record :: {} ", event);
            final ConvGradStudent newStudent = JsonUtil.getJsonObjectFromString(ConvGradStudent.class, event.getEventPayload());
            this.eventServiceMap.get(NEWSTUDENT.toString()).processEvent(newStudent, event);
            break;
          case "UPD_DEMOG":
            log.debug("Processing UPD_DEMOG event record :: {} ", event);
            final TraxDemographicsUpdateDTO updateDemog = JsonUtil.getJsonObjectFromString(TraxDemographicsUpdateDTO.class, event.getEventPayload());
            this.eventServiceMap.get(UPD_DEMOG.toString()).processEvent(updateDemog, event);
            break;
          case "UPD_GRAD":
            log.debug("Processing UPD_GRAD event record :: {} ", event);
            final TraxGraduationUpdateDTO updateGrad = JsonUtil.getJsonObjectFromString(TraxGraduationUpdateDTO.class, event.getEventPayload());
            this.eventServiceMap.get(UPD_GRAD.toString()).processEvent(updateGrad, event);
            break;
          case "UPD_STD_STATUS":
            log.debug("Processing UPD_STD_STATUS event record :: {} ", event);
            final TraxStudentStatusUpdateDTO updateStudentStatus = JsonUtil.getJsonObjectFromString(TraxStudentStatusUpdateDTO.class, event.getEventPayload());
            this.eventServiceMap.get(UPD_STD_STATUS.toString()).processEvent(updateStudentStatus, event);
            break;
          case "XPROGRAM":
            log.debug("Processing XPROGRAM event record :: {} ", event);
            final TraxXProgramDTO xprogram = JsonUtil.getJsonObjectFromString(TraxXProgramDTO.class, event.getEventPayload());
            this.eventServiceMap.get(XPROGRAM.toString()).processEvent(xprogram, event);
            break;
          case "ASSESSMENT":
            log.debug("Processing ASSESSMENT event record :: {} ", event);
            final TraxStudentUpdateDTO assessment = JsonUtil.getJsonObjectFromString(TraxStudentUpdateDTO.class, event.getEventPayload());
            this.eventServiceMap.get(ASSESSMENT.toString()).processEvent(assessment, event);
            break;
          case "COURSE":
            log.debug("Processing COURSE event record :: {} ", event);
            final TraxStudentUpdateDTO course = JsonUtil.getJsonObjectFromString(TraxStudentUpdateDTO.class, event.getEventPayload());
            this.eventServiceMap.get(COURSE.toString()).processEvent(course, event);
            break;
          case "FI10ADD":
            log.debug("Processing FI10ADD event record :: {} ", event);
            final TraxFrenchImmersionUpdateDTO frenchImmersion = JsonUtil.getJsonObjectFromString(TraxFrenchImmersionUpdateDTO.class, event.getEventPayload());
            this.eventServiceMap.get(FI10ADD.toString()).processEvent(frenchImmersion, event);
            break;
          default:
            log.warn("Silently ignoring event: {}", event);
            break;
        }
      } catch (final Exception exception) {
        log.error("Exception while processing event :: {} - {}", event, exception.getMessage());
      }
    });
  }
}
