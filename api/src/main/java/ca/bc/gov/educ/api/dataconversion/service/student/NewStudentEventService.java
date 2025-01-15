package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.constant.ConversionResultType;
import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.EventService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.api.dataconversion.constant.EventStatus.PROCESSED;
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.NEWSTUDENT;

@Service
@Slf4j
public class NewStudentEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentProcess studentProcess;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public NewStudentEventService(EventRepository eventRepository,
                                  StudentProcess studentProcess,
                                  RestUtils restUtils,
                                  EducGradDataConversionApiConstants constants) {
        super(restUtils);
        this.eventRepository = eventRepository;
        this.studentProcess = studentProcess;
        this.restUtils = restUtils;
        this.constants = constants;
    }

    @Override
    public <T extends Object> void processEvent(T request, Event event) {
        ConvGradStudent convStudent = (ConvGradStudent) request;
        ConvGradStudent result = null;
        if (convStudent != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            ConversionStudentSummaryDTO summary = new ConversionStudentSummaryDTO();
            summary.setAccessToken(accessToken);
            try {
                result = studentProcess.convertStudent(convStudent, summary, false, true);
            } catch (Exception e) {
                ConversionAlert error = new ConversionAlert();
                error.setItem(convStudent.getPen());
                error.setReason("Unexpected Exception is occurred: " + e.getLocalizedMessage());
                summary.getErrors().add(error);
                log.error("unknown exception: " + e.getLocalizedMessage());
                return;
            }
            if (result == null || ConversionResultType.FAILURE.equals(result.getResult())) {
                if (!summary.getErrors().isEmpty()) {
                    summary.getErrors().forEach(e -> log.error("Load is failed for {} - {}", e.getItem(), e.getReason()));
                }
                return;
            }
        }

        var existingEvent = eventRepository.findByEventId(event.getEventId());
        existingEvent.ifPresent(eventRecord -> {
            eventRecord.setEventStatus(PROCESSED.toString());
            eventRecord.setUpdateDate(LocalDateTime.now());
            eventRepository.save(eventRecord);
        });
    }

    @Override
    public String getEventType() {
        return NEWSTUDENT.toString();
    }
}
