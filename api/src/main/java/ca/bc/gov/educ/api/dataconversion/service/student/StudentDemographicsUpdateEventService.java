package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.model.*;
import ca.bc.gov.educ.api.dataconversion.process.StudentProcess;
import ca.bc.gov.educ.api.dataconversion.repository.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.EventService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.api.dataconversion.constant.EventStatus.PROCESSED;
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.COURSE;
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.UPD_DEMOG;

@Service
@Slf4j
public class StudentDemographicsUpdateEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentProcess studentProcess;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentDemographicsUpdateEventService(EventRepository eventRepository,
                                                 StudentProcess studentProcess,
                                                 RestUtils restUtils,
                                                 EducGradDataConversionApiConstants constants) {
        this.eventRepository = eventRepository;
        this.studentProcess = studentProcess;
        this.restUtils = restUtils;
        this.constants = constants;
    }

    @Override
    public <T extends Object> void processEvent(T request, Event event) {
        TraxDemographicsUpdateDTO updateDemog  = (TraxDemographicsUpdateDTO) request;
        if (updateDemog != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            // Load grad student
            StudentGradDTO currentStudent = studentProcess.loadStudentData(updateDemog.getPen(), accessToken);
            if (currentStudent != null) {
                processStudentDemographics(updateDemog, currentStudent, accessToken);
            } else {
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

    public void processStudentDemographics(TraxDemographicsUpdateDTO updateDemog, StudentGradDTO currentStudent, String accessToken) {
        log.info(" Process Demographics : studentID = {}, pen = {} ", currentStudent.getStudentID(), updateDemog.getPen());

        // Transcript & TVR
        populateNewBatchFlags(currentStudent);

        studentProcess.triggerGraduationBatchRun(COURSE, currentStudent.getStudentID(), updateDemog.getPen(), currentStudent.getNewRecalculateGradStatus(), currentStudent.getNewRecalculateProjectedGrad(), accessToken);
    }

    @Override
    public String getEventType() {
        return UPD_DEMOG.toString();
    }
}
