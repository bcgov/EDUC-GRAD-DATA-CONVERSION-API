package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.conv.Event;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.model.StudentGradDTO;
import ca.bc.gov.educ.api.dataconversion.model.TraxStudentUpdateDTO;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
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

    private final StudentService studentService;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentCourseUpdateEventService(EventRepository eventRepository,
                                           StudentService studentService,
                                           RestUtils restUtils,
                                           EducGradDataConversionApiConstants constants) {
        this.eventRepository = eventRepository;
        this.studentService = studentService;
        this.restUtils = restUtils;
        this.constants = constants;
    }

    @Override
    public <T extends Object> void processEvent(T request, Event event) {
        TraxStudentUpdateDTO studentCourseUpdate = (TraxStudentUpdateDTO) request;
        if (studentCourseUpdate != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            // Load grad student
            StudentGradDTO currentStudent = studentService.loadStudentData(studentCourseUpdate.getPen(), accessToken);
            processCourse(studentCourseUpdate, currentStudent);
        }

        var existingEvent = eventRepository.findByEventId(event.getEventId());
        existingEvent.ifPresent(eventRecord -> {
            eventRecord.setEventStatus(PROCESSED.toString());
            eventRecord.setUpdateDate(LocalDateTime.now());
            eventRepository.save(eventRecord);
        });
    }

    public void processCourse(TraxStudentUpdateDTO studentCourseUpdate, StudentGradDTO currentStudent) {
        log.info(" Process Courses : studentID = {}, pen = {} ", currentStudent.getStudentID(), studentCourseUpdate.getPen());
        studentService.triggerGraduationBatchRun(currentStudent.getStudentID(), "Y", "Y");
    }

    @Override
    public String getEventType() {
        return COURSE.toString();
    }
}
