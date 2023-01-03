package ca.bc.gov.educ.api.dataconversion.service.student;

import ca.bc.gov.educ.api.dataconversion.entity.conv.Event;
import ca.bc.gov.educ.api.dataconversion.model.ResponseObj;
import ca.bc.gov.educ.api.dataconversion.model.StudentGradDTO;
import ca.bc.gov.educ.api.dataconversion.model.TraxStudentStatusUpdateDTO;
import ca.bc.gov.educ.api.dataconversion.repository.conv.EventRepository;
import ca.bc.gov.educ.api.dataconversion.service.EventService;
import ca.bc.gov.educ.api.dataconversion.util.EducGradDataConversionApiConstants;
import ca.bc.gov.educ.api.dataconversion.util.RestUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

import static ca.bc.gov.educ.api.dataconversion.constant.EventStatus.PROCESSED;
import static ca.bc.gov.educ.api.dataconversion.constant.EventType.UPD_STD_STATUS;

@Service
@Slf4j
public class StudentStatusUpdateEventService extends StudentBaseService implements EventService {

    private final EventRepository eventRepository;

    private final StudentService studentService;
    private final RestUtils restUtils;

    private final EducGradDataConversionApiConstants constants;

    @Autowired
    public StudentStatusUpdateEventService(EventRepository eventRepository,
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
        TraxStudentStatusUpdateDTO studentStatusUpdate = (TraxStudentStatusUpdateDTO) request;
        if (studentStatusUpdate != null && constants.isGradUpdateEnabled()) {
            // Get Access Token
            ResponseObj res = restUtils.getTokenResponseObject();
            String accessToken = null;
            if (res != null) {
                accessToken = res.getAccess_token();
            }
            // Load grad student
            StudentGradDTO currentStudent = studentService.loadStudentData(studentStatusUpdate.getPen(), accessToken);
            processStudent(studentStatusUpdate, currentStudent, accessToken);
        }

        var existingEvent = eventRepository.findByEventId(event.getEventId());
        existingEvent.ifPresent(eventRecord -> {
            eventRecord.setEventStatus(PROCESSED.toString());
            eventRecord.setUpdateDate(LocalDateTime.now());
            eventRepository.save(eventRecord);
        });
    }

    public void processStudent(TraxStudentStatusUpdateDTO studentStatusUpdate, StudentGradDTO currentStudent, String accessToken) {
        boolean isChanged = false;

        log.info(" Process Student Status : studentID = {}, pen = {}", currentStudent.getStudentID(), studentStatusUpdate.getPen());
        String newStudentStatus = getGradStudentStatus(studentStatusUpdate.getStudentStatus(), studentStatusUpdate.getArchiveFlag());
        // Student Status
        if (!StringUtils.equals(newStudentStatus, currentStudent.getStudentStatus())) {
            currentStudent.setNewStudentStatus(newStudentStatus);
            if (StringUtils.equalsIgnoreCase(currentStudent.getNewStudentStatus(), "CUR")
                || StringUtils.equalsIgnoreCase(currentStudent.getNewStudentStatus(), "TER")
                || StringUtils.equalsIgnoreCase(currentStudent.getNewStudentStatus(), "DEC")) {
                // Transcript
                currentStudent.setNewRecalculateGradStatus("Y");
                // TVR
                currentStudent.setNewRecalculateProjectedGrad("Y");
            }
            log.info(" => student status : current = {}, request = {}", currentStudent.getStudentStatus(), currentStudent.getNewStudentStatus());
            isChanged = true;
        }

        if (isChanged) {
            log.info(" Save Student : studentID = {}, pen = {}", currentStudent.getStudentID(), studentStatusUpdate.getPen());
            studentService.saveGraduationStudent(currentStudent, accessToken);
        }
    }

    @Override
    public String getEventType() {
        return UPD_STD_STATUS.toString();
    }
}
