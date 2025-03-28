package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.Event;
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
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.COURSE;

@Service
@Slf4j
public class StudentCourseUpdateEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentProcess studentProcess;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentCourseUpdateEventService(EventRepository eventRepository,
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
        TraxStudentUpdateDTO studentCourseUpdate = (TraxStudentUpdateDTO) request;
        if (studentCourseUpdate != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            String accessToken = restUtils.fetchAccessToken();
            // Load grad student
            StudentGradDTO currentStudent = studentProcess.loadStudentData(studentCourseUpdate.getPen(), accessToken);
            if (currentStudent != null) {
                processCourse(studentCourseUpdate, currentStudent, accessToken);
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

    public void processCourse(TraxStudentUpdateDTO studentCourseUpdate, StudentGradDTO currentStudent, String accessToken) {
        log.info(" Process Course : studentID = {}, pen = {} ", currentStudent.getStudentID(), studentCourseUpdate.getPen());

        // Transcript & TVR
        populateNewBatchFlags(currentStudent);

        studentProcess.triggerGraduationBatchRun(COURSE, currentStudent.getStudentID(), studentCourseUpdate.getPen(), currentStudent.getNewRecalculateGradStatus(), currentStudent.getNewRecalculateProjectedGrad(), accessToken);
    }

    @Override
    public String getEventType() {
        return COURSE.toString();
    }
}
