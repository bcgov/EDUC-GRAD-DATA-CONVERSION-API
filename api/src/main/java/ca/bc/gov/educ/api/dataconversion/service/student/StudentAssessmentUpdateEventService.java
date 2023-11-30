package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.Event;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.model.StudentGradDTO;
import ca.bc.gov.educ.api.dataconversion.model.TraxStudentUpdateDTO;
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
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.ASSESSMENT;

@Service
@Slf4j
public class StudentAssessmentUpdateEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentProcess studentProcess;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentAssessmentUpdateEventService(EventRepository eventRepository,
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
        TraxStudentUpdateDTO studentAssessmentUpdate = (TraxStudentUpdateDTO) request;
        if (studentAssessmentUpdate != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }

            // Load grad student
            StudentGradDTO currentStudent = studentProcess.loadStudentData(studentAssessmentUpdate.getPen(), accessToken);
            if (currentStudent != null) {
                processAssessment(studentAssessmentUpdate, currentStudent, accessToken);
            }
        }

        var existingEvent = eventRepository.findByEventId(event.getEventId());
        existingEvent.ifPresent(eventRecord -> {
            eventRecord.setEventStatus(PROCESSED.toString());
            eventRecord.setUpdateDate(LocalDateTime.now());
            eventRepository.save(eventRecord);
        });
    }

    public void processAssessment(TraxStudentUpdateDTO studentAssessmentUpdate, StudentGradDTO currentStudent, String accessToken) {
        log.info(" Process Assessment : studentID = {}, pen = {} ", currentStudent.getStudentID(),studentAssessmentUpdate.getPen());
        studentProcess.triggerGraduationBatchRun(ASSESSMENT, currentStudent.getStudentID(), studentAssessmentUpdate.getPen(), "Y", "Y", accessToken);
    }

    @Override
    public String getEventType() {
        return ASSESSMENT.toString();
    }
}
