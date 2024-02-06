package ca.bc.gov.educ.api.dataconversion.service.student;

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
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.FI10ADD;

@Service
@Slf4j
public class StudentFrenchImmersionEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentProcess studentProcess;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentFrenchImmersionEventService(EventRepository eventRepository,
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
        TraxFrenchImmersionUpdateDTO frenchImmersionUpdate = (TraxFrenchImmersionUpdateDTO) request;
        if (frenchImmersionUpdate != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            // Load grad student
            StudentGradDTO currentStudent = studentProcess.loadStudentData(frenchImmersionUpdate.getPen(), accessToken);
            if (currentStudent != null) {
                processFrenchImmersion(frenchImmersionUpdate, currentStudent, accessToken);
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

    public void processFrenchImmersion(TraxFrenchImmersionUpdateDTO frenchImmersionUpdate, StudentGradDTO currentStudent, String accessToken) {
        log.info(" Process French Immersion : studentID = {}", currentStudent.getStudentID());
        if (studentProcess.hasAnyFrenchImmersionCourse(currentStudent.getProgram(), frenchImmersionUpdate.getPen(), accessToken)) {
            log.info(" => [FI] optional program will be added if not exist for {}.", currentStudent.getProgram());
            studentProcess.addStudentOptionalProgram("FI", currentStudent, false, accessToken);

            // Transcript & TVR
            populateNewBatchFlags(currentStudent);

            studentProcess.triggerGraduationBatchRun(FI10ADD, currentStudent.getStudentID(), frenchImmersionUpdate.getPen(), currentStudent.getNewRecalculateGradStatus(), currentStudent.getNewRecalculateProjectedGrad(), accessToken);
        }
    }

    @Override
    public String getEventType() {
        return FI10ADD.toString();
    }
}
